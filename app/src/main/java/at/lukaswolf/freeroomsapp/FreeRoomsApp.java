package at.lukaswolf.freeroomsapp;

import android.app.Application;
import android.content.SharedPreferences;

import at.lukaswolf.freeroomsapp.cookies.SimpleCookieJar;
import at.lukaswolf.freeroomsapp.manager.LoginManager;
import at.lukaswolf.freeroomsapp.manager.RestManager;
import lombok.Getter;
import okhttp3.Cookie;
import okhttp3.OkHttpClient;
import okhttp3.Cache;

import java.io.File;

public class FreeRoomsApp extends Application {

    @Getter
    private static OkHttpClient httpClient;
    @Getter
    private static LoginManager loginManager;
    @Getter
    private static RestManager restManager;

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
        cookieJar.saveCookie( cookie);

        httpClient = new OkHttpClient.Builder()
                .cache(cache)
                .cookieJar(cookieJar)
                .build();

        restManager = new RestManager(httpClient);
        loginManager = new LoginManager(prefs, httpClient, restManager);
    }
}
