package com.rarchives.ripme.ripper;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubredditOrImageGid extends GidHandler{
    public SubredditOrImageGid(URL url) {
        super(url);
    }

    @Override
    public String buildGid(URL url, Pattern p, Matcher m) throws Exception {
        gidType = ALBUM_TYPE.ALBUM;
        String subreddit = m.group(m.groupCount() - 1);
        gid = m.group(m.groupCount());
        this.gidUrl = new URL("http://imgur.com/r/" + subreddit + "/" + gid);
        return "r_" + subreddit + "_" + gid;
    }

    public String buildGid(URL url, String regex) {return null;};
}
