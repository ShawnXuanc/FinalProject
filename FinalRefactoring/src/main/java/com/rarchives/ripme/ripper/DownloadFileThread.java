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
    private InputStream bis = null;
    private OutputStream fos = null;
    private final int TIMEOUT;

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
        saveAs = new File(saveAs.getParentFile().getAbsolutePath() + File.separator + Utils.sanitizeSaveAs(saveAs.getName()));
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
            bis = null;
            fos = null;
            try {
                logger.info("    Downloading file: " + urlToDownload + (tries > 0 ? " Retry #" + tries : ""));
                observer.sendUpdate(STATUS.DOWNLOAD_STARTED, url.toExternalForm());

                // Setup HTTP request
                HttpURLConnection huc;
                HttpHandler httpHandler = new HttpHandler(url, observer, referrer, cookies, saveAs, fileSize);
                huc = httpHandler.setConnect();
                int statusCode = huc.getResponseCode();
                
                int issueCode = httpHandler.handleRespond(statusCode, redirected);
                if (issueCode == ISSUE.REDIRECT.getNum()) {
                    retries -= 1;
                    redirected = true;
                } else if (issueCode == ISSUE.CLIENT.getNum() || issueCode == ISSUE.SERVER.getNum() || issueCode == ISSUE.IMGURHTTP.getNum())
                    return ;
                
                // If the ripper is using the bytes progress bar set bytesTotal to
                // huc.getContentLength()
                if (observer.useByteProgessBar()) {
                    bytesTotal = huc.getContentLength();
                    checkByteTotalSize(bytesTotal);
                }

                // Save file
                saveFile(huc);

                // If we're resuming a download we append data to the existing file
                try {
                    checkFileContent(statusCode);
                } catch (FileNotFoundException e) {
                    // We do this because some filesystems have a max name length
                    checkFileProblem(e);
                }

                // If this is a test rip we skip large downloads
                byte[] data = new byte[1024 * 256];
                int bytesRead;
                if (!checkTestRip(huc)) {
                    while ((bytesRead = bis.read(data)) != -1) {
                        if (checkInterrupt()) return;
                        fos.write(data, 0, bytesRead);
                        if (observer.useByteProgessBar()) {
                            bytesDownloaded += bytesRead;
                            observer.setBytesCompleted(bytesDownloaded);
                            observer.sendUpdate(STATUS.COMPLETED_BYTES, bytesDownloaded);
                        }
                    }
                }

                bis.close();
                fos.close();

                break; // Download successful: break out of infinite loop
            } catch (SocketTimeoutException timeoutEx) {
                // Handle the timeout
                logger.error("[!] " + url.toExternalForm() + " timedout!");
                // Download failed, break out of loop
                break;
            } catch (HttpStatusException hse) {
                logger.debug(Utils.getLocalizedString("http.status.exception"), hse);
                logger.error("[!] HTTP status " + hse.getStatusCode() + " while downloading from " + urlToDownload);
                if (hse.getStatusCode() == 404 && Utils.getConfigBoolean("errors.skip404", false)) {
                    observer.downloadErrored(url,
                            "HTTP status code " + hse.getStatusCode() + " while downloading " + url.toExternalForm());
                    return;
                }
            } catch (IOException e) {
                logger.debug("IOException", e);
                logger.error("[!] " + Utils.getLocalizedString("exception.while.downloading.file") + ": " + url + " - "
                        + e.getMessage());
            } catch (NullPointerException npe){

                logger.error("[!] " + Utils.getLocalizedString("failed.to.download") + " for URL " + url);
                observer.downloadErrored(url,
                        Utils.getLocalizedString("failed.to.download") + " " + url.toExternalForm());
                return;

            }finally {
                // Close any open streams
                closeStream(bis);
                closeStream(fos);
            }
            
            if (tries > this.retries) {
                logger.error("[!] " + Utils.getLocalizedString("exceeded.maximum.retries") + " (" + this.retries
                        + ") for URL " + url);
                observer.downloadErrored(url,
                        Utils.getLocalizedString("failed.to.download") + " " + url.toExternalForm());
                return;
            }
        } while (true);
        observer.downloadCompleted(url, saveAs);
        logger.info("[+] Saved " + url + " as " + this.prettySaveAs);
    }

    private static boolean checkTestRip(HttpURLConnection huc) {
        return huc.getContentLength() / 1000000 >= 10 && AbstractRipper.isThisATest();
    }

    private void saveFile(HttpURLConnection huc) throws IOException {
        bis = new BufferedInputStream(huc.getInputStream());
        // Check if we should get the file ext from the MIME type
        if (getFileExtFromMIME) {
            getFileExt(bis);
        }
    }

    private void closeStream(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void checkByteTotalSize(int bytesTotal) {
        observer.setBytesTotal(bytesTotal);
        observer.sendUpdate(STATUS.TOTAL_BYTES, bytesTotal);
        logger.debug("Size of file at " + this.url + " = " + bytesTotal + "b");
    }

    private void checkFileProblem(FileNotFoundException e) throws FileNotFoundException {
        if (isFileNameTooLong(e)) {
            fileNameTooLongProblem();
            fos = new FileOutputStream(saveAs);
        }else if (isPathLimit()) {
            // This if is for when the file path has gone above 260 chars which windows does
            // not allow
            File parentFile = saveAs.getParentFile();
            fos = new FileOutputStream(
                    Utils.shortenSaveAsWindows(parentFile.getPath(), saveAs.getName()));
        }
    }

    private void checkFileContent(int statusCode) throws FileNotFoundException {
        fos = statusCode == ISSUE.NORMAL.getNum()? new FileOutputStream(saveAs, true):new FileOutputStream(saveAs);
    }

    private boolean isPathLimit() {
        return saveAs.getAbsolutePath().length() > 259 && Utils.isWindows();
    }

    private static boolean isFileNameTooLong(FileNotFoundException e) {
        String errorMessage = e.getMessage();
        return errorMessage.contains("File name too long");
    }

    private void fileNameTooLongProblem() {
        logger.error("The filename " + saveAs.getName()
                + " is to long to be saved on this file system.");
        logger.info("Shortening filename");
        getUserSavePath();
    }

    private void getUserSavePath() {
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

    private void getFileExt(InputStream bis) throws IOException {
        String fileExt = URLConnection.guessContentTypeFromStream(bis);
        if (fileExt != null) {
            fileExt = fileExt.replaceAll("image/", "");
            saveAs = new File(saveAs.toString() + "." + fileExt);
        } else {
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

    private long getFileSize(long fileSize) {
        if (saveAs.exists() && observer.tryResumeDownload()) {
            fileSize = saveAs.length();
        }
        return fileSize;
    }


}
