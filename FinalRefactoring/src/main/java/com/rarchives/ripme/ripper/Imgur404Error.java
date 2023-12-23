package com.rarchives.ripme.ripper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.rarchives.ripme.App.logger;

public class Imgur404Error extends ErrorCodeHandler{

    public boolean handleError(AbstractRipper observer, URL url, HttpURLConnection huc, int statusCode) throws Exception {
        if (url.getHost().endsWith("imgur.com")) {
            logger.error("[!] Imgur image is 404 (503 bytes long): " + url);
            observer.downloadErrored(url, "Imgur image is 404: " + url.toExternalForm());
        }
        return true;
    }

    @Override
    public URL getUrlToDownload() {
        return codeUrl;
    }
}
