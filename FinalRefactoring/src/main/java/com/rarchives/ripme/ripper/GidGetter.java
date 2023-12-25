package com.rarchives.ripme.ripper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;

public class GidGetter {
    URL gidUrl;
    URL Url;
    String gid;
    Pattern p;
    Matcher m ;
    ALBUM_TYPE albumType;
    Regex regex;
    HashMap<String, GidHandler> gidMatcher;

    public GidGetter(URL url) {
        this.Url = url;
        regex = new Regex(url);
        gidMatcher = regex.getMatcher();
    }

    public ALBUM_TYPE getGidType() {
        return albumType;
    }

    public URL getUrl() {
        return gidUrl;
    }

    public String getGid() {
        String urlString = Url.toExternalForm();
        Set<String> Keys = gidMatcher.keySet();
        for (String regexString : Keys) {
            p = Pattern.compile(regexString);
            m = p.matcher(urlString);
            if (m.matches()) {
                GidHandler handler = gidMatcher.get(regexString);
                try {
                    gid = handler.buildGid(Url, p, m);
                    albumType = handler.getType();
                    this.gidUrl = handler.setUrl();
                    return gid;
                } catch (Exception e) {
                    System.out.println("not found");
                }
            }
        }
        return null;
    }

}


