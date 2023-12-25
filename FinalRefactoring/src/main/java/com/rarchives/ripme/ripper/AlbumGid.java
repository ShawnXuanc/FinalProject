package com.rarchives.ripme.ripper;


import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlbumGid extends GidHandler{

    public AlbumGid() {

    }

    @Override
    public String buildGid(URL url, Pattern p, Matcher m) throws Exception {
        gidType = ALBUM_TYPE.ALBUM;
        gid = m.group(m.groupCount());
        this.gidUrl = new URL("http://imgur.com/a/" + gid);
        return gid;
    }

}
