package com.rarchives.ripme.ripper;

import com.rarchives.ripme.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class RedirectCode extends ErrorCodeHandler{
    @Override
    public int errorHandle(int statusCode, AbstractRipper observer, File saveAs, HttpURLConnection huc, URL url) throws Exception {
        return ISSUE.REDIRECT.getNum();
    }
}
