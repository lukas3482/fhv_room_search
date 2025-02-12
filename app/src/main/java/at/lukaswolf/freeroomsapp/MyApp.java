package at.lukaswolf.freeroomsapp;

import android.app.Application;
import android.content.SharedPreferences;

import okhttp3.Cookie;
import okhttp3.OkHttpClient;
import okhttp3.Cache;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyApp extends Application {

    private static OkHttpClient httpClient;

    @Override
    public void onCreate() {
        super.onCreate();

        File cacheDir = new File(getCacheDir(), "okhttp-cache");
        Cache cache = new Cache(cacheDir, 5 * 1024 * 1024);
        SimpleCookieJar cookieJar = new SimpleCookieJar();

        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        Cookie cookie = new Cookie.Builder()
                .domain("a5.fhv.at")
                .path("/")
                .name("PHPSESSID")
                .value(prefs.getString("PHPSESSID", "le66nqidscqise2orphb4c232q"))
                .httpOnly()
                .build();
        List<Cookie> cookies = new ArrayList<>();
        cookies.add(cookie);
        cookieJar.saveFromResponse(null, cookies);

        httpClient = new OkHttpClient.Builder()
                .cache(cache)
                .cookieJar(cookieJar)
                .build();

    }

    public static OkHttpClient getHttpClient() {
        return httpClient;
    }
}
