package com.connectsdk.service.upnp;

import android.util.Xml;
import com.connectsdk.core.ImageInfo;
import com.connectsdk.core.MediaInfo;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/* JADX INFO: loaded from: classes3.dex */
public class DLNAMediaInfoParser {
    private static final String ALBUM = "upnp:album";
    private static final String APOS = "&amp;apos;";
    private static final String ARTIST = "r:albumArtist";
    private static final String CREATOR = "dc:creator";
    private static final String GENRE = "upnp:genre";
    private static final String GT = "&gt;";
    private static final String LT = "&lt;";
    private static final String RADIOTITLE = "r:streamContent";
    private static final String THUMBNAIL = "upnp:albumArtURI";
    private static final String TITLE = "dc:title";

    public static String getAlbum(String str) {
        return getData(str, "upnp:album");
    }

    public static String getArtist(String str) {
        return getData(str, "dc:creator");
    }

    private static String getData(String str, String str2) {
        if (str.contains(toEndTag(str2))) {
            return toString(str.substring(toStartTag(str2).length() + str.indexOf(toStartTag(str2)), str.indexOf(toEndTag(str2))));
        }
        if (str.contains("&lt;")) {
            return "";
        }
        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
        try {
            xmlPullParserNewPullParser.setFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces", false);
            xmlPullParserNewPullParser.setInput(new StringReader(str));
            for (int iNextTag = xmlPullParserNewPullParser.nextTag(); iNextTag != 1; iNextTag = xmlPullParserNewPullParser.next()) {
                if (iNextTag == 2 && xmlPullParserNewPullParser.getName().equals(str2) && xmlPullParserNewPullParser.next() == 4) {
                    return xmlPullParserNewPullParser.getText();
                }
            }
        } catch (IOException e2) {
            e2.printStackTrace();
        } catch (XmlPullParserException e3) {
            e3.printStackTrace();
        }
        return "";
    }

    public static String getGenre(String str) {
        return getData(str, "upnp:genre");
    }

    public static MediaInfo getMediaInfo(String str) {
        String url = getURL(str);
        String title = getTitle(str);
        String mimeType = getMimeType(str);
        String str2 = getArtist(str) + "\n" + getAlbum(str);
        String thumbnail = getThumbnail(str);
        ArrayList arrayList = new ArrayList();
        arrayList.add(new ImageInfo(thumbnail));
        return new MediaInfo(url, mimeType, title, str2, arrayList);
    }

    public static String getMimeType(String str) {
        if (!str.contains("protocolInfo")) {
            return "";
        }
        int iIndexOf = str.indexOf("*:") + 2;
        return str.substring(iIndexOf, str.substring(iIndexOf).indexOf(":") + iIndexOf);
    }

    public static String getThumbnail(String str) {
        return URLDecoder.decode(getData(str, "upnp:albumArtURI"));
    }

    public static String getTitle(String str) {
        return getData(str, getData(str, "r:streamContent").equals("") ? "dc:title" : "r:streamContent");
    }

    public static String getURL(String str) {
        if (!str.contains("&lt;")) {
            return getData(str, "res");
        }
        if (!str.contains(toEndTag("res"))) {
            return "";
        }
        return URLDecoder.decode(str.substring(str.indexOf("&lt;res") + str.substring(str.indexOf("&lt;res")).indexOf("&gt;") + 4, str.indexOf(toEndTag("res"))));
    }

    private static String toEndTag(String str) {
        return toStartTag(("/" + str));
    }

    private static String toStartTag(String str) {
        return ("&lt;" + str + "&gt;");
    }

    private static String toString(String str) {
        StringBuilder sb = new StringBuilder();
        if (!str.contains("&amp;apos;")) {
            return str;
        }
        sb.append(str.substring(0, str.indexOf("&amp;apos;")));
        sb.append("'");
        sb.append(str.substring(str.indexOf("&amp;apos;") + 10));
        return sb.toString();
    }

    public static MediaInfo getMediaInfo(String str, String str2) {
        String url = getURL(str);
        String title = getTitle(str);
        String mimeType = getMimeType(str);
        String str3 = getArtist(str) + "\n" + getAlbum(str);
        String thumbnail = getThumbnail(str);
        try {
            new URL(thumbnail).openConnection().connect();
        } catch (Exception unused) {
            thumbnail = (str2 + thumbnail);
        }
        ArrayList arrayList = new ArrayList();
        arrayList.add(new ImageInfo(thumbnail));
        return new MediaInfo(url, mimeType, title, str3, arrayList);
    }
}
