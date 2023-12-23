package com.rarchives.ripme.ripper;

import java.net.HttpURLConnection;
import java.net.URL;

public abstract class ErrorCodeHandler {
    URL codeUrl = null;
    public abstract boolean handleError(AbstractRipper observer, URL url, HttpURLConnection huc, int statusCode) throws Exception;
    public abstract URL getUrlToDownload();
}
