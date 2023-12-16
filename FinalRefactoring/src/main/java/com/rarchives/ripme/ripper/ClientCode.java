package com.rarchives.ripme.ripper.rippers;

import com.rarchives.ripme.ripper.AbstractRipper;
import com.rarchives.ripme.ripper.ErrorCodeHandler;
import com.rarchives.ripme.ripper.ISSUE;
import com.rarchives.ripme.utils.Utils;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.rarchives.ripme.App.logger;

public class ClientCode extends ErrorCodeHandler {
    @Override
    public int errorHandle(int statusCode, AbstractRipper observer, File saveAs, HttpURLConnection huc, URL url) throws Exception {
        clientError(observer, statusCode, url);
        return ISSUE.REDIRECT.getNum();
    }

    private void clientError(AbstractRipper observer, int statusCode, URL url) {
        logger.error("[!] " + Utils.getLocalizedString("nonretriable.status.code") + " " + statusCode
                + " while downloading from " + url);
        observer.downloadErrored(url, Utils.getLocalizedString("nonretriable.status.code") + " "
                + statusCode + " while downloading " + url.toExternalForm());
    }
}
