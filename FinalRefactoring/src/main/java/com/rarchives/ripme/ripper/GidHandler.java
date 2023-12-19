package com.rarchives.ripme.ripper;

import com.rarchives.ripme.ripper.rippers.ImgurRipper;
import com.rarchives.ripme.ui.RipStatusMessage.STATUS;

import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;

public abstract class GidHandler {
    protected Pattern p = null;
    protected Matcher m = null;
    public String gid = null;
    public ALBUM_TYPE gidType = null;
    protected String urlString;
    public URL gidUrl = null;

    public GidHandler(URL url) {
    }

    public abstract String buildGid(URL url, Pattern p, Matcher m) throws Exception;
    // public abstract String buildGid(URL url, String regex);

    public URL setUrl( ) {
        return gidUrl;
    }

    public ALBUM_TYPE getType() {
        return this.gidType ;
    }
}
