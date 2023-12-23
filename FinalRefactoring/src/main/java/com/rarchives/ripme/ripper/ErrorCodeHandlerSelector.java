package com.rarchives.ripme.ripper;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
        selector.put(3, new RedirectCode());
        selector.put(4, new ClientCode());
        selector.put(5, new ServerCode());
        selector.put(503, new Imgur404Error());
    }

    public boolean select(int statusCode) throws Exception {
        int errorCode = statusCode/100;
        if (huc.getContentLength() == 503) {
            errorCode = 503;
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
