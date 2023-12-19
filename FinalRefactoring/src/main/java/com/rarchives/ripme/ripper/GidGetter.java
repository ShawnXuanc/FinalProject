package com.rarchives.ripme.ripper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;

public class GidGetter {
    URL gidUrl;
    URL url;
    String gid;
    Pattern p = null;
    Matcher m = null;
    public ALBUM_TYPE albumType;
    Regex regex = new Regex();
    List<GidHandler> HandlerList;
    public HashMap<String, GidHandler> gidMatcher = new HashMap<>();

    public GidGetter(URL url, Pattern p, Matcher m) {
        this.url = url;
        this.p = p;
        this.m = m;
        // gidHandlers = Arrays.asList(new AlbumGid(url, regex.getList().get(0)));
        gidMatcher.put("^https?://(www\\.|m\\.)?imgur\\.com/(a|gallery)/([a-zA-Z0-9]{5,}).*$", new AlbumGid(url));
    }

    public ALBUM_TYPE getGidType() {
        return albumType;
    }

    public URL getUrl() {
        return gidUrl;
    }

    public String getGid() {
        String urlString = url.toExternalForm();
        Set<String> Keys = gidMatcher.keySet();
        for (String regex : Keys) {
            System.out.println(regex);
            p = Pattern.compile(regex);
            m = p.matcher(urlString);
            if (m.matches()) {
                GidHandler handler = gidMatcher.get(regex);
                try {
                    gid = handler.buildGid(url, p, m);
                    albumType = handler.getType();
                    this.gidUrl = handler.setUrl();
                    return gid;
                } catch (Exception e) {
                    System.out.println("EXCEPTION");
                }
            }
        }
        return null;
    }

    public void initGidMatcher() {
        for (String rs : regex.getList()) {
            System.out.println(rs);
        }
    }

}
