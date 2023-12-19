package com.rarchives.ripme.ripper;

import java.util.List;
import java.util.Arrays;

public class Regex {
//    public String albumRegex1 = "^https?://(www\\.|m\\.)?imgur\\.com/(a|gallery)/([a-zA-Z0-9]{5,}).*$";
//    public String albumRegex2 = "^https?://(www\\.|m\\.)?imgur\\.com/(a|gallery|t)/[a-zA-Z0-9]*/([a-zA-Z0-9]{5,}).*$";
//    public String userRegex = "^https?://([a-zA-Z0-9\\-]{3,})\\.imgur\\.com/?$";
//    public String userImageRegex = "^https?://([a-zA-Z0-9\\-]{3,})\\.imgur\\.com/all.*$";
//    public String userAlbumRegex = "^https?://([a-zA-Z0-9\\-]{3,})\\.imgur\\.com/([a-zA-Z0-9\\-_]+).*$";
//    public String subredditRegex = "^https?://(www\\.|m\\.)?imgur\\.com/r/([a-zA-Z0-9\\-_]{3,})(/top|/new)?(/all|/year|/month|/week|/day)?/?$";
//    public String albumRegex3 = "^https?://(i\\.|www\\.|m\\.)?imgur\\.com/r/(\\w+)/([a-zA-Z0-9,]{5,}).*$";
//    public String singleImageRegex = "^https?://(i\\.|www\\.|m\\.)?imgur\\.com/([a-zA-Z0-9]{5,})$";
//    public String seriesOfImageRegex = "^https?://(i\\.|www\\.|m\\.)?imgur\\.com/([a-zA-Z0-9,]{5,}).*$";
    private List<String> regexList;

    public Regex() {
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

    public List<String> getList() {
        return this.regexList;
    }
}
