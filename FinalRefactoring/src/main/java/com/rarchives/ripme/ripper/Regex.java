package com.rarchives.ripme.ripper;

import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.net.URL;
public class Regex {

    private List<String> regexList;
    private List<GidHandler> handlerList;
    private URL regexUrl;
    private HashMap<String, GidHandler> gidMatcher = new HashMap<>();

    public Regex(URL url) {
        regexUrl = url;
        initRegex();
    }


    public void initRegex() {
        createRegexList();
        createHandler();
        createHashTable();
    }

    public void createRegexList() {
        regexList = Arrays.asList(
                "^https?://(www\\.|m\\.)?imgur\\.com/(a|gallery)/([a-zA-Z0-9]{5,}).*$",
                "^https?://(www\\.|m\\.)?imgur\\.com/(a|gallery|t)/[a-zA-Z0-9]*/([a-zA-Z0-9]{5,}).*$",
                "^https?://([a-zA-Z0-9\\-]{3,})\\.imgur\\.com/?$",
                "^https?://([a-zA-Z0-9\\-]{3,})\\.imgur\\.com/all.*$",
                "^https?://([a-zA-Z0-9\\-]{3,})\\.imgur\\.com/([a-zA-Z0-9\\-_]+).*$",
                "^https?://(www\\.|m\\.)?imgur\\.com/r/([a-zA-Z0-9\\-_]{3,})(/top|/new)?(/all|/year|/month|/week|/day)?/?$",
                "^https?://(i\\.|www\\.|m\\.)?imgur\\.com/r/(\\w+)/([a-zA-Z0-9,]{5,}).*$",
                "^https?://(i\\.|www\\.|m\\.)?imgur\\.com/([a-zA-Z0-9]{5,})$",
                "^https?://(i\\.|www\\.|m\\.)?imgur\\.com/([a-zA-Z0-9,]{5,}).*$"
        );

    }


    public void createHandler() {
        handlerList = Arrays.asList(
                new AlbumGid(),
                new AlbumGid(),
                new UserGid(),
                new UserImageGid(),
                new UserAlbumGid(),
                new SubredditGid(),
                new SubredditOrImageGid(),
                new SingleImageGid(),
                new SeriesOfImagesGid()

        );
    }

    public void createHashTable() {
        int num = regexList.size();
        for (int i = 0; i < num; ++i) {
            gidMatcher.put(regexList.get(i), handlerList.get(i));
        }
    }

    public List<String> getRegexList() {
        return this.regexList;
    }

    public List<GidHandler> getHandlerList() {
        return this.handlerList;
    }

    public HashMap<String, GidHandler> getMatcher() {
        return this.gidMatcher;
    }
}

