package com.rarchives.ripme.ripper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.net.ssl.HttpsURLConnection;

import com.rarchives.ripme.ui.MainWindow;
import org.apache.log4j.Logger;
import org.jsoup.HttpStatusException;

import com.rarchives.ripme.ui.RipStatusMessage.STATUS;
import com.rarchives.ripme.utils.Utils;

import static com.rarchives.ripme.App.logger;

/**
 * Thread for downloading files. Includes retry logic, observer notifications,
 * and other goodies.
 */
class DownloadFileThread extends Thread {
    private static final Logger logger = Logger.getLogger(DownloadFileThread.class);

    private String referrer = "";
    private Map<String, String> cookies = new HashMap<>();

    private URL url;
    private File saveAs;
    private String prettySaveAs;
    private AbstractRipper observer;
    private int retries;
    private Boolean getFileExtFromMIME;
    private final int TIMEOUT;
    private InputStream bis = null;
    private OutputStream fos = null;
    private boolean isTimeOut = true;
    public DownloadFileThread(URL url, File saveAs, AbstractRipper observer, Boolean getFileExtFromMIME) {
        super();
        this.url = url;
        this.saveAs = saveAs;
        this.prettySaveAs = Utils.removeCWD(saveAs);
        this.observer = observer;
        this.retries = Utils.getConfigInteger("download.retries", 1);
        this.TIMEOUT = Utils.getConfigInteger("download.timeout", 60000);
        this.getFileExtFromMIME = getFileExtFromMIME;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    /**
     * Attempts to download the file. Retries as needed. Notifies observers upon
     * completion/error/warn.
     */
    public void run() {
        // First thing we make sure the file name doesn't have any illegal chars in it
        saveAs = new File(
                saveAs.getParentFile().getAbsolutePath() + File.separator + Utils.sanitizeSaveAs(saveAs.getName()));
        long fileSize = 0;
        int bytesTotal = 0;
        int bytesDownloaded = 0;
        
        fileSize = getFileSize(fileSize);

        if (checkInterrupt()) return;

        if (checkFileExist() || checkFuzzyExist()) {
            if (Utils.getConfigBoolean("file.overwrite", false)) {
                checkOverWrite();
            } else {
                checkAlreadyExist();
                return;
            }
        }

        URL urlToDownload = this.url;
        boolean redirected = false;
        int tries = 0; // Number of attempts to download
        do {
            tries += 1;

            try {
                logger.info("    Downloading file: " + urlToDownload + (tries > 0 ? " Retry #" + tries : ""));
                observer.sendUpdate(STATUS.DOWNLOAD_STARTED, url.toExternalForm());

                // fileSize 可以考慮一下獲得方式
                HttpHandler connect = new HttpHandler(url, observer, referrer, cookies, saveAs, fileSize);
                HttpURLConnection huc = connect.getConnection();

                // ---------------------------

                int statusCode = huc.getResponseCode();
                // int statusCode = 503;
                if (checkRedirected(statusCode, redirected)) {
                    tries-=1;
                    redirected = true;
                }

                if (connect.handleRespond(statusCode)) return ;

                // If the ripper is using the bytes progress bar set bytesTotal to
                // huc.getContentLength()
                if (observer.useByteProgessBar()) {
                    bytesTotal = huc.getContentLength();
                    setByte(bytesTotal);
                }

                // Save file
                bis = new BufferedInputStream(huc.getInputStream());
                // Check if we should get the file ext from the MIME type
                checkFileIsMineType(bis);
                // If we're resuming a download we append data to the existing file

                try {
                    checkFileContent(statusCode);
                } catch (FileNotFoundException e) {
                    // We do this because some filesystems have a max name length
                    checkFileProblem(e);
                }

                byte[] data = new byte[1024 * 256];
                int bytesRead;
                // If this is a test rip we skip large downloads
                if (!isShouldSkipFileDownload(huc)) {
                    while ((bytesRead = bis.read(data)) != -1) {
                        if (checkInterrupt()) return;
                        fos.write(data, 0, bytesRead);
                        if (observer.useByteProgessBar()) {
                            bytesDownloaded += bytesRead;
                            updateObsever(bytesDownloaded);
                        }
                    }
                } else {
                    logger.debug("Not downloading whole file because it is over 10mb and this is a test");
                }
                
                bis.close();
                fos.close();
                break; // Download successful: break out of infinite loop
            } catch (HttpStatusException hse) {
                if (getNotFindError(hse, urlToDownload)) return;
            } catch (Exception e) {
                if(checkExceptionType(e)) return ;
            } finally {
                // Close any open streams
                closeStream(bis);
                closeStream(fos);
            }
            if (exceedMaximumRetries(tries)) return;
        } while (isTimeOut);
        observer.downloadCompleted(url, saveAs);
        logger.info("[+] Saved " + url + " as " + this.prettySaveAs);
    }

    private static boolean checkRedirected(int statusCode, boolean redirected) {
        return statusCode / 100 == 3 && !redirected;
    }

    private void updateObsever(int bytesDownloaded) {
        observer.setBytesCompleted(bytesDownloaded);
        observer.sendUpdate(STATUS.COMPLETED_BYTES, bytesDownloaded);
    }

    private boolean getNotFindError(HttpStatusException hse, URL urlToDownload) {
        logger.debug(Utils.getLocalizedString("http.status.exception"), hse);
        logger.error("[!] HTTP status " + hse.getStatusCode() + " while downloading from " + urlToDownload);
        if (isNotFindError(hse)) {
            observer.downloadErrored(url,
                    "HTTP status code " + hse.getStatusCode() + " while downloading " + url.toExternalForm());
            return true;
        }
        return false;
    }

    private static boolean isNotFindError(HttpStatusException hse) {
        return hse.getStatusCode() == 404 && Utils.getConfigBoolean("errors.skip404", false);
    }

    private boolean checkExceptionType(Exception e) {
        if (e.getMessage().contains("SocketTimeoutException")){
            logger.error("[!] " + url.toExternalForm() + " timedout!");
            isTimeOut = false;
        }

        if (e.getMessage().contains("IOException")){
            logger.debug("IOException", e);
            logger.error("[!] " + Utils.getLocalizedString("exception.while.downloading.file") + ": " + url + " - "
                    + e.getMessage());
        }
        if (e.getMessage().contains("NullPointerException")) {
            logger.error("[!] " + Utils.getLocalizedString("failed.to.download") + " for URL " + url);
            observer.downloadErrored(url,
                    Utils.getLocalizedString("failed.to.download") + " " + url.toExternalForm());
            return true;
        }
        return false;
    }


    private void closeStream(Closeable closeable) {
        if (closeable != null) {
            processClose(closeable);
        }
    }

    private static void processClose(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean exceedMaximumRetries(int tries) {
        if (tries > this.retries) {
            logger.error("[!] " + Utils.getLocalizedString("exceeded.maximum.retries") + " (" + this.retries
                    + ") for URL " + url);
            observer.downloadErrored(url,
                    Utils.getLocalizedString("failed.to.download") + " " + url.toExternalForm());
            return true;
        }
        return false;
    }

    private static boolean isShouldSkipFileDownload(HttpURLConnection huc) {
        return huc.getContentLength() / 1000000 >= 10 && AbstractRipper.isThisATest();
    }

    private void checkFileContent(int statusCode) throws FileNotFoundException {
        fos = (statusCode == 206) ? new FileOutputStream(saveAs, true) : new FileOutputStream(saveAs);
    }

    private void checkFileProblem(FileNotFoundException e) throws FileNotFoundException {
        if (e.getMessage().contains("File name too long")) {
            checkFileNameTooLongProblem();
            fos = new FileOutputStream(saveAs);
        } else if (saveAs.getAbsolutePath().length() > 259 && Utils.isWindows()) {
            // This if is for when the file path has gone above 260 chars which windows does
            // not allow
            fos = new FileOutputStream(
                    Utils.shortenSaveAsWindows(saveAs.getParentFile().getPath(), saveAs.getName()));
        }
    }


    private void checkFileNameTooLongProblem() {
        logger.error("The filename " + saveAs.getName()
                + " is to long to be saved on this file system.");
        logger.info("Shortening filename");
        String[] saveAsSplit = saveAs.getName().split("\\.");
        // Get the file extension so when we shorten the file name we don't cut off the
        // file extension
        String fileExt = saveAsSplit[saveAsSplit.length - 1];
        // The max limit for filenames on Linux with Ext3/4 is 255 bytes
        logger.info(saveAs.getName().substring(0, 254 - fileExt.length()) + fileExt);
        String filename = saveAs.getName().substring(0, 254 - fileExt.length()) + "." + fileExt;
        // We can't just use the new file name as the saveAs because the file name
        // doesn't include the
        // users save path, so we get the user save path from the old saveAs
        saveAs = new File(saveAs.getParentFile().getAbsolutePath() + File.separator + filename);
    }

    private void checkFileIsMineType(InputStream bis) throws IOException {
        if (getFileExtFromMIME) {
            String fileExt = URLConnection.guessContentTypeFromStream(bis);
            setFile(bis, fileExt);
        }
    }

    private void setFile(InputStream bis, String fileExt) throws IOException {
        if (fileExt != null) {
            fileExt = fileExt.replaceAll("image/", "");
            saveAs = new File(saveAs.toString() + "." + fileExt);
        } else {
            resetFile(bis);
        }
    }

    private void resetFile(InputStream bis) throws IOException {
        String fileExt;
        logger.error("Was unable to get content type from stream");
        // Try to get the file type from the magic number
        byte[] magicBytes = new byte[8];
        bis.read(magicBytes, 0, 5);
        bis.reset();
        fileExt = Utils.getEXTFromMagic(magicBytes);
        if (fileExt != null) {
            saveAs = new File(saveAs.toString() + "." + fileExt);
        } else {
            logger.error(Utils.getLocalizedString("was.unable.to.get.content.type.using.magic.number"));
            logger.error(
                    Utils.getLocalizedString("magic.number.was") + ": " + Arrays.toString(magicBytes));
        }
    }

    private void setByte(int bytesTotal) {
        observer.setBytesTotal(bytesTotal);
        observer.sendUpdate(STATUS.TOTAL_BYTES, bytesTotal);
        logger.debug("Size of file at " + this.url + " = " + bytesTotal + "b");
    }

    private void checkAlreadyExist() {
        logger.info("[!] " + Utils.getLocalizedString("skipping") + " " + url + " -- "
                + Utils.getLocalizedString("file.already.exists") + ": " + prettySaveAs);
        observer.downloadExists(url, saveAs);
    }

    private void checkOverWrite() {
        logger.info("[!] " + Utils.getLocalizedString("deleting.existing.file") + prettySaveAs);
        saveAs.delete();
    }

    private long getFileSize(long fileSize) {
        if (saveAs.exists() && observer.tryResumeDownload()) {
            fileSize = saveAs.length();
        }
        return fileSize;
    }

    private boolean checkFuzzyExist() {
        return Utils.fuzzyExists(new File(saveAs.getParent()), saveAs.getName()) && getFileExtFromMIME
                && !observer.tryResumeDownload();
    }

    private boolean checkFileExist() {
        return saveAs.exists() && !observer.tryResumeDownload() && !getFileExtFromMIME;
    }

    private boolean checkInterrupt() {
        try {
            observer.stopCheck();
        } catch (IOException e) {
            observer.downloadErrored(url, Utils.getLocalizedString("download.interrupted"));
            return true;
        }
        return false;
    }

}
