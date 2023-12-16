package com.rarchives.ripme.ripper;

import java.io.File;


import java.net.HttpURLConnection;
import java.net.URL;

public abstract class ErrorCodeHandler {
    int action = ISSUE.NORMAL.getNum();
    int errorCode = 0;
    public abstract int errorHandle(int statusCode, AbstractRipper observe, File saveAs, HttpURLConnection huc, URL url) throws Exception;
}
