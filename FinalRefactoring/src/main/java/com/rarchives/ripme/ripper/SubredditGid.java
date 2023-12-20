package com.rarchives.ripme.ripper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubredditGid extends GidHandler{
    public SubredditGid(URL url) {
        super(url);
    }

    @Override
    public String buildGid(URL url, Pattern p, Matcher m) throws Exception {
        gidType = ALBUM_TYPE.SUBREDDIT;
        String album = m.group(2);
        for (int i = 3; i <= m.groupCount(); i++) {
            if (m.group(i) != null) {
                album += "_" + m.group(i).replace("/", "");
            }
        }
        return album;
    }

    public String buildGid(URL url, String regex) {return null;};
}
