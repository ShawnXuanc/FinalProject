package com.rarchives.ripme.ripper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SeriesOfImagesGid extends GidHandler{
    public SeriesOfImagesGid(URL url) {
        super(url);
    }

    @Override
    public String buildGid(URL url, Pattern p, Matcher m) throws Exception {
        // Series of imgur images
        gidType = ALBUM_TYPE.SERIES_OF_IMAGES;
        gid = m.group(m.groupCount());
        if (!gid.contains(",")) {
            throw new MalformedURLException("Imgur image doesn't contain commas");
        }
        return gid.replaceAll(",", "-");
    }

    public String buildGid(URL url, String regex) {return null;};
}
