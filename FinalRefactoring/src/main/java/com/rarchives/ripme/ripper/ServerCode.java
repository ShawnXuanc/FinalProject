package com.rarchives.ripme.ripper;

import com.rarchives.ripme.utils.Utils;

import java.net.HttpURLConnection;
import java.net.URL;

import static com.rarchives.ripme.App.logger;

public class ServerCode extends ErrorCodeHandler {

    public boolean handleError(AbstractRipper observer, URL urlToDownload, HttpURLConnection huc, int statusCode) throws Exception {
        codeUrl = urlToDownload;
        logger.error("[!] " + Utils.getLocalizedString("nonretriable.status.code") + " " + statusCode
                + " while downloading from " + urlToDownload);
        observer.downloadErrored(urlToDownload, Utils.getLocalizedString("nonretriable.status.code") + " "
                + statusCode + " while downloading " + urlToDownload.toExternalForm());
        return true; // Not retriable, drop out.
    }

    @Override
    public URL getUrlToDownload() {
        return codeUrl;
    }
}


