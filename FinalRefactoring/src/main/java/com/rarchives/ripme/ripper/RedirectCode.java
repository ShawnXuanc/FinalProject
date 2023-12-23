package com.rarchives.ripme.ripper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class RedirectCode extends ErrorCodeHandler {

    public boolean handleError(AbstractRipper observer,URL url, HttpURLConnection huc, int statusCode) throws Exception {
        String location = huc.getHeaderField("Location");
        codeUrl = new URL(location);
        // Throw exception so download can be retried
        throw new IOException("Redirect status code " + statusCode + " - redirect to " + location);
    }

    @Override
    public URL getUrlToDownload() {
        return codeUrl;
    }
}
