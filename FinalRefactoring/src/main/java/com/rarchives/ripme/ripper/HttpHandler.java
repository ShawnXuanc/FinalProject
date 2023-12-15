package com.rarchives.ripme.ripper;

import com.rarchives.ripme.utils.Utils;
import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import static com.rarchives.ripme.App.logger;

public class HttpHandler {
    URL url;
    private HttpURLConnection huc;
    private AbstractRipper observer;
    private String referrer;
    private Map<String, String> cookies;
    URL urlToDownload;
    private final int TIMEOUT;
    private long fileSize;
    private File saveAs;



    public HttpHandler(URL url, AbstractRipper observer, String referrer, Map<String, String> cookies, File saveAs, long fileSize) throws IOException {
        this.url = url;
        this.observer = observer;
        this.TIMEOUT = Utils.getConfigInteger("download.timeout", 60000);
        this.referrer = referrer;
        this.saveAs = saveAs;
        this.fileSize = fileSize;
        this.cookies = cookies;
        setConnect();
    }


    public HttpURLConnection setConnect() throws IOException {
        initHttpURLConnection();
        String cookie = "";
        for (String key : cookies.keySet()) {
            cookie = setCookie(key, cookie);
        }
        huc.setRequestProperty("Cookie", cookie);
        if (resumeDownLoad()) {
            huc.setRequestProperty("Range", "bytes=" + fileSize + "-");
        }
        logger.debug(Utils.getLocalizedString("request.properties") + ": " + huc.getRequestProperties());
        huc.connect();
        return huc;
    }

    private boolean resumeDownLoad() {
        return observer.tryResumeDownload() && fileSize != 0;
    }

    private String setCookie(String key, String cookie) {
        if (!cookie.equals("")) {
            cookie += "; ";
        }
        cookie += key + "=" + cookies.get(key);
        return cookie;
    }

    private void initHttpURLConnection() throws IOException {
        urlToDownload = this.url;
        huc = this.url.toString().startsWith("https")? (HttpsURLConnection) urlToDownload.openConnection():(HttpURLConnection) urlToDownload.openConnection();
        huc.setInstanceFollowRedirects(true);
        huc.setConnectTimeout(TIMEOUT);
        huc.setReadTimeout(TIMEOUT);
        huc.setRequestProperty("accept", "*/*");
        if (!referrer.equals("")) {
            huc.setRequestProperty("Referer", referrer); // Sic
        }
        huc.setRequestProperty("User-agent", AbstractRipper.USER_AGENT);
    }

    public int handleRespond(int statusCode, boolean redirected) throws IOException {
        // 這邊之後要用polymorphism重構
        int action = ISSUE.NORMAL.getNum();
        logger.debug("Status code: " + statusCode);

        if (statusCode != ISSUE.NORMAL.getNum() && observer.tryResumeDownload() && saveAs.exists()) {
            throw new IOException(Utils.getLocalizedString("server.doesnt.support.resuming.downloads"));
        }

        int errorCode = statusCode / 100;
        if (errorCode == ISSUE.REDIRECT.getNum()) { // 3xx Redirect
            // Don't increment retries on the first redirect
            action = !redirected ? ISSUE.REDIRECT.getNum() : ISSUE.NORMAL.getNum();
            redirectError(statusCode);
        }

        if (errorCode == ISSUE.CLIENT.getNum()) { // 4xx errors
            clientError(statusCode);
            // Not retriable, drop out.
            action = ISSUE.CLIENT.getNum();
        }

        if (errorCode == ISSUE.SERVER.getNum()) { // 5xx errors
            serverError(statusCode);
        }

        if (huc.getContentLength() == ISSUE.IMGURHTTP.getNum() && urlToDownload.getHost().endsWith("imgur.com")) {
            imgurError();
            action = ISSUE.IMGURHTTP.getNum();
        }
        return action;
    }

    private void imgurError() {
        // Imgur image with 503 bytes is "404"
        logger.error("[!] Imgur image is 404 (503 bytes long): " + url);
        observer.downloadErrored(url, "Imgur image is 404: " + url.toExternalForm());
    }

    private void serverError(int statusCode) throws IOException {
        observer.downloadErrored(url, Utils.getLocalizedString("retriable.status.code") + " " + statusCode
                + " while downloading " + url.toExternalForm());
        // Throw exception so download can be retried
        throw new IOException(Utils.getLocalizedString("retriable.status.code") + " " + statusCode);
    }

    private void clientError(int statusCode) {
        logger.error("[!] " + Utils.getLocalizedString("nonretriable.status.code") + " " + statusCode
                + " while downloading from " + url);
        observer.downloadErrored(url, Utils.getLocalizedString("nonretriable.status.code") + " "
                + statusCode + " while downloading " + url.toExternalForm());
    }

    private void redirectError(int statusCode) throws IOException {
        String location = huc.getHeaderField("Location");
        urlToDownload = new URL(location);
        // Throw exception so download can be retried
        throw new IOException("Redirect status code " + statusCode + " - redirect to " + location);
    }


}
