package com.rarchives.ripme.ripper;

import com.rarchives.ripme.ripper.ErrorCodeHandler;
import com.rarchives.ripme.ripper.rippers.ClientCode;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ErrorCodeHandlerSelector {
    Map<Integer, ErrorCodeHandler> selector = new HashMap<>();
    URL url;
    private HttpURLConnection huc;
    private AbstractRipper observer;
    private File saveAs;

    public ErrorCodeHandlerSelector(int statusCode, AbstractRipper observer, File saveAs, HttpURLConnection huc, URL url) {
        this.observer = observer;
        this.saveAs = saveAs;
        this.huc = huc;
        this.url = url;
        selector.put(ISSUE.REDIRECT.getNum(), new RedirectCode());
        selector.put(ISSUE.CLIENT.getNum(), new ClientCode());
        selector.put(ISSUE.SERVER.getNum(), new ServerCode());
    }

    public int  select(int statusCode) throws Exception {
        int errorCode = statusCode/4;
        return selector.get(errorCode).errorHandle(statusCode, observer, saveAs, huc, url);
    }
}
