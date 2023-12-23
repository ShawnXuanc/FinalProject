package com.rarchives.ripme.ripper;

import com.rarchives.ripme.utils.Utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.rarchives.ripme.App.logger;

public class ClientCode  extends ErrorCodeHandler{

    public boolean handleError(AbstractRipper observer, URL url, HttpURLConnection huc, int statusCode) throws Exception {
        codeUrl = url;
        logger.error("[!] " + Utils.getLocalizedString("nonretriable.status.code") + " " + statusCode
                + " while downloading from " + codeUrl);
        observer.downloadErrored(codeUrl, Utils.getLocalizedString("nonretriable.status.code") + " "
                + statusCode + " while downloading " + codeUrl.toExternalForm());
        return true; // Not retriable, drop out.
    }
    @Override
    public URL getUrlToDownload() {
        return codeUrl;
    }
}

