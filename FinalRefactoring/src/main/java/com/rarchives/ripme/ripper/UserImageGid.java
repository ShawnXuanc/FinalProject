package com.rarchives.ripme.ripper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserImageGid extends GidHandler{
    public UserImageGid() {
    }

    @Override
    public String buildGid(URL url, Pattern p, Matcher m) throws Exception {
        gidType = ALBUM_TYPE.USER_IMAGES;
        return m.group(1) + "_images";
    }


}
