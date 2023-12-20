package com.rarchives.ripme.ripper;

import com.rarchives.ripme.ripper.ALBUM_TYPE;
import com.rarchives.ripme.ripper.GidHandler;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SingleImageGid extends GidHandler {
    public SingleImageGid(URL url) {
        super(url);
    }

    @Override
    public String buildGid(URL url, Pattern p, Matcher m) throws Exception {
        gidType = ALBUM_TYPE.SINGLE_IMAGE;
        return  m.group(m.groupCount());
    }

    public String buildGid(URL url, String regex) {return null;};
}
