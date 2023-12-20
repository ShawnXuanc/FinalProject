package com.rarchives.ripme.ripper.rippers;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rarchives.ripme.ripper.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import com.rarchives.ripme.ui.RipStatusMessage.STATUS;
import com.rarchives.ripme.utils.Http;
import com.rarchives.ripme.utils.Utils;

public class ImgurRipper extends AlbumRipper {

    private static final String DOMAIN = "imgur.com",
                                HOST   = "imgur";

    private final int SLEEP_BETWEEN_ALBUMS;

    private Document albumDoc;

//    enum ALBUM_TYPE {
//        ALBUM,
//        USER,
//        USER_ALBUM,
//        USER_IMAGES,
//        SINGLE_IMAGE,
//        SERIES_OF_IMAGES,
//        SUBREDDIT
//    }

    private ALBUM_TYPE albumType;
    public ImgurRipper(URL url) throws IOException {
        super(url);
        SLEEP_BETWEEN_ALBUMS = 1;
    }

    /**
     * Imgur ripper does not return the same URL except when ripping
     * many albums at once (USER). In this case, we want duplicates.
     */
    @Override
    public boolean allowDuplicates() {
        return albumType == ALBUM_TYPE.USER;
    }

    public boolean canRip(URL url) {
        if (!url.getHost().endsWith(DOMAIN)) {
           return false;
        }
        try {
            getGID(url);
        } catch (Exception e) {
            // Can't get GID, can't rip it.
            return false;
        }
        return true;
    }

    public URL sanitizeURL(URL url) throws MalformedURLException {
        String u = removeFragment(url);
        u = replaceProblem(u);
        return new URL(u);
    }

    private static String replaceProblem(String u) {
        u = u.replace("imgur.com/gallery/", "imgur.com/a/");
        u = u.replace("https?://m\\.imgur\\.com", "http://imgur.com");
        u = u.replace("https?://i\\.imgur\\.com", "http://imgur.com");
        return u;
    }

    private static String removeFragment(URL url) {
        String u = url.toExternalForm();
        if (u.indexOf('#') >= 0) {
            u = u.substring(0, u.indexOf('#'));
        }
        return u;
    }

    public String getAlbumTitle(URL url) throws MalformedURLException {
        String gid = getGID(url);
        if (this.albumType == ALBUM_TYPE.ALBUM) {
            try {
                // Attempt to use album title as GID
                if (albumDoc == null) {
                    albumDoc = Http.url(url).get();
                }
                String title = null;
                final String defaultTitle1 = "Imgur: The most awesome images on the Internet";
                final String defaultTitle2 = "Imgur: The magic of the Internet";
                title = initTitle();
                // This is here encase the album is unnamed, to prevent
                // Imgur: The most awesome images on the Internet from being added onto the album name
                String albumTitle = buildAlbumTitle(title, defaultTitle1, defaultTitle2, gid);

                return albumTitle;
            } catch (IOException e) {
                // Fall back to default album naming convention
            }
        }
        return getHost() + "_" + gid;
    }

    private String buildAlbumTitle(String title, String defaultTitle1, String defaultTitle2, String gid) {
        Elements elems;
        if (checkTitle(title, defaultTitle1, defaultTitle2)) {
            LOGGER.debug("Album is untitled or imgur is returning the default title");
            // We set the title to "" here because if it's found in the next few attempts it will be changed
            // but if it's nto found there will be no reason to set it later
            title = "";
            LOGGER.debug("Trying to use title tag to get title");
            elems = albumDoc.select("title");
            if (elems != null && !checkElems(elems, defaultTitle1, defaultTitle2)) {
                title = elems.text();
            } else {
                LOGGER.debug("Was unable to get album title or album was untitled");
            }
        }

        String albumTitle = "imgur_";

        albumTitle += gid;
        if (title != null) {
            albumTitle += "_" + title;
        }
        return albumTitle;
    }

    private static boolean checkTitle(String title, String defaultTitle1, String defaultTitle2) {
        return title.contains(defaultTitle1) || title.contains(defaultTitle2);
    }

    private static boolean checkElems(Elements elems, String defaultTitle1, String defaultTitle2) {
        return checkTitle(elems.text(), defaultTitle1, defaultTitle2);
    }

    private String initTitle() {
        String title = null;
        Elements elems;
        LOGGER.info("Trying to get album title");
        elems = albumDoc.select("meta[property=og:title]");
        if (elems != null) {
            title = elems.attr("content");
            LOGGER.debug("Title is " + title);
        }
        return title;
    }


    @Override
    public void rip() throws IOException {
        switch (albumType) {
            case ALBUM:
                // Fall-through
            case USER_ALBUM:
                LOGGER.info("Album type is USER_ALBUM");
                // Don't call getAlbumTitle(this.url) with this
                // as it seems to cause the album to be downloaded to a subdir.
                ripAlbum(this.url);
                break;
            case SERIES_OF_IMAGES:
                LOGGER.info("Album type is SERIES_OF_IMAGES");
                ripAlbum(this.url);
                break;
            case SINGLE_IMAGE:
                LOGGER.info("Album type is SINGLE_IMAGE");
                ripSingleImage(this.url);
                break;
            case USER:
                LOGGER.info("Album type is USER");
                ripUserAccount(url);
                break;
            case SUBREDDIT:
                LOGGER.info("Album type is SUBREDDIT");
                ripSubreddit(url);
                break;
            case USER_IMAGES:
                LOGGER.info("Album type is USER_IMAGES");
                ripUserImages(url);
                break;
        }
        waitForThreads();
    }

    private void ripSingleImage(URL url) throws IOException {
        String strUrl = url.toExternalForm();
        Document document = getDocument(strUrl);
        Matcher m = getEmbeddedJsonMatcher(document);
        if (m.matches()) {
            JSONObject json = new JSONObject(m.group(1)).getJSONObject("image");
            addURLToDownload(extractImageUrlFromJson(json), "");
        }
    }

    private void ripAlbum(URL url) throws IOException {
        ripAlbum(url, "");
    }

    private void ripAlbum(URL url, String subdirectory) throws IOException {
        int index = 0;
        this.sendUpdate(STATUS.LOADING_RESOURCE, url.toExternalForm());
        index = 0;
        ImgurAlbum album = getImgurAlbum(url);
        for (ImgurImage imgurImage : album.images) {
            stopCheck();

            String saveAs = initSaveAsPath(subdirectory);

            createDirFile(saveAs);
            
            index += 1;
            if (checkDownloadFile()) {
                saveAs += String.format("%03d_", index);
            }
            saveAs += imgurImage.getSaveAs();
            saveAs = saveAs.replaceAll("\\?\\d", "");

            addURLToDownload(imgurImage.url, new File(saveAs));
        }
    }

    private static boolean checkDownloadFile() {
        return Utils.getConfigBoolean("download.save_order", true);
    }

    private String initSaveAsPath(String subdirectory) throws IOException {
        String saveAs = workingDir.getCanonicalPath();
        if (!saveAs.endsWith(File.separator)) {
            saveAs += File.separator;
        }
        if (subdirectory != null && !subdirectory.equals("")) {
            saveAs += subdirectory;
        }
        if (!saveAs.endsWith(File.separator)) {
            saveAs += File.separator;
        }
        return saveAs;
    }

    private static void createDirFile(String saveAs) {
        File subdirFile = new File(saveAs);
        if (!subdirFile.exists()) {
            subdirFile.mkdirs();
        }
    }

    public static ImgurAlbum getImgurSeries(URL url) throws IOException {
        Pattern p = Pattern.compile("^.*imgur\\.com/([a-zA-Z0-9,]*).*$");
        Matcher m = p.matcher(url.toExternalForm());
        ImgurAlbum album = new ImgurAlbum(url);
        if (m.matches()) {
            String[] imageIds = m.group(1).split(",");
            for (String imageId : imageIds) {
                // TODO: Fetch image with ID imageId
                LOGGER.debug("Fetching image info for ID " + imageId);
                try {
                    JSONObject json = Http.url("https://api.imgur.com/2/image/" + imageId + ".json").getJSON();
                    JSONObject image = json.getJSONObject("image");
                    JSONObject links = image.getJSONObject("links");

                    if (!json.has("image") || !image.has("links") || !links.has("original"))
                        continue;

                    String original = links.getString("original");
                    ImgurImage theImage = new ImgurImage(new URL(original));
                    album.addImage(theImage);
                } catch (Exception e) {
                    LOGGER.error("Got exception while fetching imgur ID " + imageId, e);
                }
            }
        }
        return album;
    }

    private static JSONObject getLinkObject(JSONObject image) {
        JSONObject links = image.getJSONObject("links");
        if (!links.has("original")) {
            return null;
        }
        return links;
    }

    private static JSONObject getimageObject(JSONObject json) {
        JSONObject image = json.getJSONObject("image");
        if (!image.has("links")) {
            return null;
        }
        return image;
    }

    private static JSONObject getJsonObject(String imageId) throws IOException {
        JSONObject json = Http.url("https://api.imgur.com/2/image/" + imageId + ".json").getJSON();
        if (!json.has("image")) {
            return null;
        }
        return json;
    }

    public static ImgurAlbum getImgurAlbum(URL url) throws IOException {
        String strUrl = url.toExternalForm();
        if (!strUrl.contains(",")) {
            strUrl += "/all";
        }

        ImgurAlbum url1 = getImgurAlbum(url, strUrl);
        if (url1 != null) return url1;

        Document doc = getDocument(url);

        // Fall back to parsing HTML elements
        // NOTE: This does not always get the highest-resolution images!
        ImgurAlbum imgurAlbum = new ImgurAlbum(url);
        for (Element thumb : doc.select("div.image")) {
            String image;
            image = getImage(thumb);
            if (image == null) continue;
            
            if (checkType(image)) {
                image = image.replace(".gif", ".mp4");
            }
            
            ImgurImage imgurImage = new ImgurImage(new URL(image));
            imgurAlbum.addImage(imgurImage);
        }
        return imgurAlbum;
    }

    private static boolean checkType(String image) {
        return image.endsWith(".gif") && Utils.getConfigBoolean("prefer.mp4", false);
    }
    
    // still can do something
    private static String getImage(Element thumb) {
        String image;
        if (!thumb.select("a.zoom").isEmpty()) {
            // Clickably full-size
            image = "http:" + thumb.select("a").attr("href");
        } else if (!thumb.select("img").isEmpty()) {
            image = "http:" + thumb.select("img").attr("src");
        } else {
            // Unable to find image in this div
            LOGGER.error("[!] Unable to find image in div: " + thumb.toString());
            return null;
        }
        return image;
    }

    private static Document getDocument(URL url) throws IOException {
        Document doc;
        // TODO If album is empty, use this to check for cached images:
        // http://i.rarchives.com/search.cgi?cache=http://imgur.com/a/albumID
        // At the least, get the thumbnails.

        LOGGER.info("[!] Falling back to /noscript method");

        String newUrl = url.toExternalForm() + "/noscript";
        LOGGER.info("    Retrieving " + newUrl);
        doc = Jsoup.connect(newUrl)
                            .userAgent(USER_AGENT)
                            .get();
        return doc;
    }

    private static ImgurAlbum getImgurAlbum(URL url, String strUrl) throws IOException {
        LOGGER.info("    Retrieving " + strUrl);
        Document doc = getAlbumData("https://api.imgur.com/3/album/" + strUrl.split("/a/")[1]);
        // Try to use embedded JSON to retrieve images
        LOGGER.info(Jsoup.clean(doc.body().toString(), Whitelist.none()));

        try {
            JSONObject json = new JSONObject(Jsoup.clean(doc.body().toString(), Whitelist.none()));
            JSONArray jsonImages = json.getJSONObject("data").getJSONArray("images");
            return createImgurAlbumFromJsonArray(url, jsonImages);
        } catch (JSONException e) {
            LOGGER.debug("Error while parsing JSON at " + url + ", continuing", e);
        }
        return null;
    }

    private static Matcher getEmbeddedJsonMatcher(Document doc) {
        Pattern p = Pattern.compile("^.*widgetFactory.mergeConfig\\('gallery', (.*?)\\);.*$", Pattern.DOTALL);
        return p.matcher(doc.body().html());
    }

    private static ImgurAlbum createImgurAlbumFromJsonArray(URL url, JSONArray jsonImages) throws MalformedURLException {
        ImgurAlbum imgurAlbum = new ImgurAlbum(url);
        int imagesLength = jsonImages.length();
        for (int i = 0; i < imagesLength; i++) {
            JSONObject ob = jsonImages.getJSONObject(i);
            imgurAlbum.addImage(new ImgurImage( new URL(ob.getString("link"))));
        }
        return imgurAlbum;
    }

    private static ImgurImage createImgurImageFromJson(JSONObject json) throws MalformedURLException {
        return new ImgurImage(extractImageUrlFromJson(json));
    }

    private static URL extractImageUrlFromJson(JSONObject json) throws MalformedURLException {
        String ext = json.getString("ext");
        if (ext.equals(".gif") && Utils.getConfigBoolean("prefer.mp4", false)) {
            ext = ".mp4";
        }
        return  new URL(
                "http://i.imgur.com/"
                        + json.getString("hash")
                        + ext);
    }

    private static Document getDocument(String strUrl) throws IOException {
        return Jsoup.connect(strUrl)
                                .userAgent(USER_AGENT)
                                .timeout(10 * 1000)
                                .maxBodySize(0)
                                .get();
    }

    private static Document getAlbumData(String strUrl) throws IOException {
        return Jsoup.connect(strUrl)
                .userAgent(USER_AGENT)
                .timeout(10 * 1000)
                .maxBodySize(0)
                .header("Authorization", "Client-ID " + Utils.getConfigString("imgur.client_id", "546c25a59c58ad7"))
                .ignoreContentType(true)
                .get();
    }


    /**
     * Rips all albums in an imgur user's account.
     * @param url
     *      URL to imgur user account (http://username.imgur.com)
     * @throws IOException
     */
    private void ripUserAccount(URL url) throws IOException {
        LOGGER.info("Retrieving " + url);
        sendUpdate(STATUS.LOADING_RESOURCE, url.toExternalForm());
        Document doc = Http.url(url).get();
        for (Element album : doc.select("div.cover a")) {
            stopCheck();
            if (checkAlbunAttr(album)) {
                continue;
            }

            RipAlbum(album);

        }
    }

    private void RipAlbum(Element album) throws MalformedURLException {
        String albumID = album.attr("href").substring(album.attr("href").lastIndexOf('/') + 1);
        URL albumURL = new URL("http:" + album.attr("href") + "/noscript");
        try {
            ripAlbum(albumURL, albumID);
            Thread.sleep(SLEEP_BETWEEN_ALBUMS * 1000);
        } catch (Exception e) {
            LOGGER.error("Error while ripping album: " + e.getMessage(), e);
        }
    }

    private static boolean checkAlbunAttr(Element album) {
        return !album.hasAttr("href")
                || !album.attr("href").contains("imgur.com/a/");
    }

    private void ripUserImages(URL url) throws IOException {
        int page = 0; int imagesFound = 0; int imagesTotal = 0;
        String jsonUrl = url.toExternalForm().replace("/all", "/ajax/images");
        if (jsonUrl.contains("#")) {
            jsonUrl = jsonUrl.substring(0, jsonUrl.indexOf("#"));
        }

        while (true) {
            try {
                page++;
                JSONObject jsonData = getJsonData(jsonUrl, page);
                if (jsonData.has("count")) {
                    imagesTotal = jsonData.getInt("count");
                }

                imagesFound += addImageFileToQueue(jsonData);
                if (imagesFound >= imagesTotal) {
                    break;
                }
                Thread.sleep(1000);
            } catch (Exception e) {
                LOGGER.error("Error while ripping user images: " + e.getMessage(), e);
                break;
            }
        }
    }

    private static JSONObject getJsonData(String jsonUrl, int page) throws IOException {
        String mid = "?sort=0&order=1&album=0&page=";
        String end = "&perPage=60";
        String jsonUrlWithParams = jsonUrl + mid + page + end;
        JSONObject json = Http.url(jsonUrlWithParams).getJSON();
        JSONObject jsonData = json.getJSONObject("data");
        return jsonData;
    }

    private int addImageFileToQueue(JSONObject jsonData) throws MalformedURLException {
        int imagesFound = 0;
        String urlHead = "http://i.imgur.com/";
        JSONArray images = jsonData.getJSONArray("images");
        for (int i = 0; i < images.length(); i++) {
            imagesFound++;
            JSONObject image = images.getJSONObject(i);
            String imageUrl = urlHead + image.getString("hash") + image.getString("ext");
            String prefix = "";
            if (checkDownloadFile()) {
                prefix = String.format("%03d_", imagesFound);
            }
            addURLToDownload(new URL(imageUrl), prefix);
        }
        return imagesFound;
    }

    private void ripSubreddit(URL url) throws IOException {
        int page = 0;
        while (true) {
            stopCheck();
            Document doc = getURLData(url, page);

            Elements imgs = addImageURLQueue(doc);
            if (imgs.isEmpty()) {
                break;
            }

            page++;

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while waiting to load next album: ", e);
                break;
            }
        }
    }

    private Elements addImageURLQueue(Document doc) throws MalformedURLException {
        Elements imgs = doc.select(".post img");
        String urlTitle = "http:";
        for (Element img : imgs) {
            String image = img.attr("src");
            if (image.startsWith("//")) {
                image = urlTitle + image;
            }
            if (image.contains("b.")) {
                image = image.replace("b.", ".");
            }
            URL imageURL = new URL(image);
            addURLToDownload(imageURL);
        }
        return imgs;
    }

    private static Document getURLData(URL url, int page) throws IOException {
        String pageURL = url.toExternalForm();
        String head = "page/";
        String end = "/miss?scrolled";
        if (!pageURL.endsWith("/")) {
            pageURL += "/";
        }
        pageURL += head + page + end;
        LOGGER.info("    Retrieving " + pageURL);
        Document doc = Http.url(pageURL).get();
        return doc;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        String gid = null;
        Pattern p;
        Matcher m;
        try {
            GidGetter getter = new GidGetter(url);
            gid = getter.getGid();
            albumType = getter.getGidType();
            this.url = getter.getUrl();
        } catch (Exception e) {
            throw new MalformedURLException("Unsupported imgur URL format: " + url.toExternalForm());
        }
        return gid;
    }

    public ALBUM_TYPE getAlbumType() {
        return albumType;
    }

    public static class ImgurImage {
        String title = "";
        String description = "";
        String extension   = "";
        public URL url = null;

        ImgurImage(URL url) {
            this.url = url;
            String tempUrl = url.toExternalForm();
            this.extension = tempUrl.substring(tempUrl.lastIndexOf('.'));
            if (this.extension.contains("?")) {
                this.extension = this.extension.substring(0, this.extension.indexOf("?"));
            }
        }

        ImgurImage(URL url, String title) {
            this(url);
            this.title = title;
        }

        public ImgurImage(URL url, String title, String description) {
            this(url, title);
            this.description = description;
        }

        String getSaveAs() {
            String saveAs = this.title;
            String u = url.toExternalForm();
            u = u.contains("?") ? u.substring(0, u.indexOf("?")) : u;

            saveAs = getSaveAsPath(u, saveAs);
            return saveAs + this.extension;
        }

        private static String getSaveAsPath(String u, String saveAs) {
            String imgId = u.substring(u.lastIndexOf('/') + 1, u.lastIndexOf('.'));
            if (saveAs == null || saveAs.equals("")) {
                saveAs = imgId;
            } else {
                saveAs = saveAs + "_" + imgId;
            }
            saveAs = Utils.filesystemSafe(saveAs);
            return saveAs;
        }
    }

    public static class ImgurAlbum {
        String title = null;
        public URL    url = null;
        public List<ImgurImage> images = new ArrayList<>();
        ImgurAlbum(URL url) {
            this.url = url;
        }
        public ImgurAlbum(URL url, String title) {
            this(url);
            this.title = title;
        }
        void addImage(ImgurImage image) {
            images.add(image);
        }
    }

}
