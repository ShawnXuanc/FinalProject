package com.rarchives.ripme.ripper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserGid extends GidHandler{

    public UserGid() {

    }

    @Override
    public String buildGid(URL url, Pattern p, Matcher m) throws Exception {
        gid = m.group(1);
        if (gid.equals("www")) {
            throw new MalformedURLException("Cannot rip the www.imgur.com homepage");
        }
        gidType = ALBUM_TYPE.USER;
        return "user_" + gid;
    }

    public String buildGid(URL url, String regex) {return null;};
}
