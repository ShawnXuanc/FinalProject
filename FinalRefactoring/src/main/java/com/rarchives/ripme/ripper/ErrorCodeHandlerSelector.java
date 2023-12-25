package com.rarchives.ripme.ripper;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import com.rarchives.ripme.ripper.ISSUE.*;

public class ErrorCodeHandlerSelector {
    private Map<Integer, ErrorCodeHandler> selector = new HashMap<>();
    private URL url;
    private HttpURLConnection huc;
    private AbstractRipper observer;
    private URL urlToDownload;

    public ErrorCodeHandlerSelector(AbstractRipper observer, HttpURLConnection huc, URL url) {
        this.observer = observer;
        this.huc = huc;
        this.url = url;
        urlToDownload = url;
        setSelector();
    }
    private void setSelector() {
        selector.put(ISSUE.REDIRECT.value(), new RedirectCode());
        selector.put(ISSUE.CLIENT.value(), new ClientCode());
        selector.put(ISSUE.SERVER.value(), new ServerCode());
        selector.put(ISSUE.IMGURHTTP.value(), new Imgur404Error());
    }

    public boolean select(int statusCode) throws Exception {
        int errorCode = statusCode/100;
        if (huc.getContentLength() == ISSUE.IMGURHTTP.value()) {
            errorCode = ISSUE.IMGURHTTP.value();
        }
        ErrorCodeHandler handler = selector.get(errorCode);
        if (handler == null) return false;
        boolean action = handler.handleError(observer, url, huc, statusCode);
        urlToDownload = handler.getUrlToDownload();
        return action;
    }

    public URL getUrlToDownload() {
        return urlToDownload;
    }
}



