package com.rarchives.ripme.ripper;

import com.rarchives.ripme.ripper.rippers.ImgurRipper;
import com.rarchives.ripme.ui.RipStatusMessage.STATUS;

import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;

public abstract class GidHandler {
    protected Pattern p;
    protected Matcher m;
    public String gid;
    public ALBUM_TYPE gidType;
    public URL gidUrl;

    public GidHandler() {
    }

    public abstract String buildGid(URL url, Pattern p, Matcher m) throws Exception;


    public URL setUrl( ) {
        return gidUrl;
    }

    public ALBUM_TYPE getType() {
        return this.gidType ;
    }
}


