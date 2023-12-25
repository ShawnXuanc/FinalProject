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
    URL urlToDownload;
    URL url;
    private HttpURLConnection huc;
    private AbstractRipper observer;
    private String referrer;
    private Map<String, String> cookies;
    private final int TIMEOUT;
    private long fileSize;
    private File saveAs;
    boolean redirected = false;
    int tries;
    // private ErrorCodeHandlerSelector selector;

    public HttpHandler(URL url, AbstractRipper observer, String referrer, Map<String, String> cookies, File saveAs, long fileSize) throws IOException {
        this.urlToDownload = url;
        this.url = url;
        this.observer = observer;
        this.TIMEOUT = Utils.getConfigInteger("download.timeout", 60000);
        this.referrer = referrer;
        this.saveAs = saveAs;
        this.fileSize = fileSize;
        this.cookies = cookies;
        initHttpURLConnection();
    }

    public HttpURLConnection getConnection() {
        return huc;
    }

    public boolean getRedirected() {
        return redirected;
    }
    public int getTries() {
        return tries;
    }
    public void initHttpURLConnection() throws IOException {
        huc = checkUrlTitle() ? (HttpsURLConnection) urlToDownload.openConnection() : (HttpURLConnection) urlToDownload.openConnection();
        setConnection();
        setCookie();
        if (observer.tryResumeDownload() && fileSize != 0) {
            huc.setRequestProperty("Range", "bytes=" + fileSize + "-");
        }
        logger.debug(Utils.getLocalizedString("request.properties") + ": " + huc.getRequestProperties());
        huc.connect();
    }

    private void setConnection() {
        huc.setInstanceFollowRedirects(true);
        huc.setConnectTimeout(TIMEOUT);
        huc.setReadTimeout(TIMEOUT);
        huc.setRequestProperty("accept", "*/*");
        if (!referrer.isEmpty()) {
            huc.setRequestProperty("Referer", referrer); // Sic
        }
        huc.setRequestProperty("User-agent", AbstractRipper.USER_AGENT);
    }

    private void setCookie() {
        String cookie = "";
        for (String key : cookies.keySet()) {
            cookie = getCookie(key, cookie);
        }
        huc.setRequestProperty("Cookie", cookie);
    }

    private String getCookie(String key, String cookie) {
        if (!cookie.isEmpty()) {
            cookie += "; ";
        }
        cookie += key + "=" + cookies.get(key);
        return cookie;
    }

    private boolean checkUrlTitle() {
        return this.url.toString().startsWith("https");
    }

    public boolean handleRespond(int statusCode) throws Exception {
        logger.debug("Status code: " + statusCode);
        // If the server doesn't allow resuming downloads error out
        handleDownloadResumption(statusCode);

        ErrorCodeHandlerSelector selector = new ErrorCodeHandlerSelector(observer, huc, url);

        urlToDownload = selector.getUrlToDownload();
        return selector.select(statusCode);
    }

    private void handleDownloadResumption(int statusCode) throws IOException {
        if (statusCode != 206 && observer.tryResumeDownload() && saveAs.exists()) {
            throw new IOException(Utils.getLocalizedString("server.doesnt.support.resuming.downloads"));
        }
    }

}
