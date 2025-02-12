package at.lukaswolf.freeroomsapp;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * Speichert Cookies (z.B. Session-Cookies) über alle Requests hinweg.
 */
public class SimpleCookieJar implements CookieJar {

    private final List<Cookie> allCookies = new ArrayList<>();

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        allCookies.clear();
        allCookies.add(cookies.get(cookies.size() -1));
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        return allCookies;
    }

    public List<Cookie> loadForRequest() {
        return allCookies;
    }

    public void addCookie(Cookie cookie){
        allCookies.clear();
        allCookies.add(cookie);
    }
}
