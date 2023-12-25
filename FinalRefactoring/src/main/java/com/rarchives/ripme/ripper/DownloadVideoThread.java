package com.rarchives.ripme.ripper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;

import com.rarchives.ripme.ui.RipStatusMessage.STATUS;
import com.rarchives.ripme.utils.Utils;

/**
 * Thread for downloading files.
 * Includes retry logic, observer notifications, and other goodies.
 */
class DownloadVideoThread extends Thread {

    private static final Logger logger = Logger.getLogger(DownloadVideoThread.class);

    private URL url;
    private File saveAs;
    private String prettySaveAs;
    private AbstractRipper observer;
    private int retries;
    private String referrer = "";
    private Map<String, String> cookies = new HashMap<>();
    public DownloadVideoThread(URL url, File saveAs, AbstractRipper observer) {
        super();
        this.url = url;
        this.saveAs = saveAs;
        this.prettySaveAs = Utils.removeCWD(saveAs);
        this.observer = observer;
        this.retries = Utils.getConfigInteger("download.retries", 1);
    }

    /**
     * Attempts to download the file. Retries as needed.
     * Notifies observers upon completion/error/warn.
     */
    public void run() {
        if (checkStop()) return;

        if (saveAs.exists()) {
            if (checkFileCondition()) return;
        }

        int bytesTotal, bytesDownloaded = 0;

        try {
            bytesTotal = getTotalBytes(this.url);
        } catch (IOException e) {
            logger.error("Failed to get file size at " + this.url, e);
            observer.downloadErrored(this.url, "Failed to get file size of " + this.url);
            return;
        }

        updateByte(bytesTotal);

        int tries = 0; // Number of attempts to download
        do {
            long fileSize = 0;
            fileSize = getFileSize(fileSize);
            InputStream bis = null; OutputStream fos = null;
            byte[] data = new byte[1024 * 256];
            int bytesRead;
            try {
                logger.info("    Downloading file: " + url + (tries > 0 ? " Retry #" + tries : ""));
                observer.sendUpdate(STATUS.DOWNLOAD_STARTED, url.toExternalForm());

                // Setup HTTP request
                HttpHandler connect = new HttpHandler(url, observer, referrer, cookies, saveAs, fileSize);
                HttpURLConnection huc = connect.getConnection();

                bis = new BufferedInputStream(huc.getInputStream());
                fos = new FileOutputStream(saveAs);

                while ( (bytesRead = bis.read(data)) != -1) {
                    if (checkStop()) return;
                    fos.write(data, 0, bytesRead);
                    bytesDownloaded += bytesRead;
                    observer.setBytesCompleted(bytesDownloaded);
                    observer.sendUpdate(STATUS.COMPLETED_BYTES, bytesDownloaded);
                }
                bis.close();
                fos.close();
                break; // Download successful: break out of infinite loop

            } catch (IOException e) {
                logger.error("[!] Exception while downloading file: " + url + " - " + e.getMessage(), e);
            } finally {
                // Close any open streams
                closeStream(bis);
                closeStream(fos);
            }
            if (exceedMaximumRetries(tries)) return;
        } while (true);
        observer.downloadCompleted(url, saveAs);
        logger.info("[+] Saved " + url + " as " + this.prettySaveAs);
    }

    private void updateByte(int bytesTotal) {
        observer.setBytesTotal(bytesTotal);
        observer.sendUpdate(STATUS.TOTAL_BYTES, bytesTotal);
        logger.debug("Size of file at " + this.url + " = " + bytesTotal + "b");
    }

    private boolean checkFileCondition() {
        if (Utils.getConfigBoolean("file.overwrite", false)) {
            logger.info("[!] Deleting existing file" + prettySaveAs);
            saveAs.delete();
        } else {
            logger.info("[!] Skipping " + url + " -- file already exists: " + prettySaveAs);
            observer.downloadExists(url, saveAs);
            return true;
        }
        return false;
    }

    private boolean checkStop() {
        try {
            observer.stopCheck();
        } catch (IOException e) {
            observer.downloadErrored(url, "Download interrupted");
            return true;
        }
        return false;
    }

    private boolean exceedMaximumRetries(int tries) {
        if (tries > this.retries) {
            logger.error("[!] Exceeded maximum retries (" + this.retries + ") for URL " + url);
            observer.downloadErrored(url, "Failed to download " + url.toExternalForm());
            return true;
        }
        return false;
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
    /**
     * @param url
     *      Target URL
     * @return 
     *      Returns connection length
     */
    private int getTotalBytes(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("HEAD");
        conn.setRequestProperty("accept",  "*/*");
        conn.setRequestProperty("Referer", this.url.toExternalForm()); // Sic
        conn.setRequestProperty("User-agent", AbstractRipper.USER_AGENT);
        return conn.getContentLength();
    }

    private long getFileSize(long fileSize) {
        if (saveAs.exists() && observer.tryResumeDownload()) {
            fileSize = saveAs.length();
        }
        return fileSize;
    }
}