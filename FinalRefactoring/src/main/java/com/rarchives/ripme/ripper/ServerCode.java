package com.rarchives.ripme.ripper;

import com.rarchives.ripme.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ServerCode extends ErrorCodeHandler {
    @Override
    public int errorHandle(int statusCode, AbstractRipper observer, File saveAs, HttpURLConnection huc, URL url) throws Exception {
        serverError(observer, statusCode, url);
        return ISSUE.NORMAL.getNum();
    }

    private void serverError(AbstractRipper observer, int statusCode, URL url) throws IOException {
        observer.downloadErrored(url, Utils.getLocalizedString("retriable.status.code") + " " + statusCode
                + " while downloading " + url.toExternalForm());
        // Throw exception so download can be retried
        throw new IOException(Utils.getLocalizedString("retriable.status.code") + " " + statusCode);
    }
}
