package com.rarchives.ripme.ripper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserAlbumGid extends GidHandler{
    public UserAlbumGid() {

    }

    @Override
    public String buildGid(URL url, Pattern p, Matcher m) throws Exception {
        gidType = ALBUM_TYPE.USER_ALBUM;
        return m.group(1) + "-" + m.group(2);
    }

    public String buildGid(URL url, String regex) {return null;};
}
