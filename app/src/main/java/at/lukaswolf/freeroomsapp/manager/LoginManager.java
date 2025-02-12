package at.lukaswolf.freeroomsapp.manager;

import android.content.SharedPreferences;

import java.io.IOException;

import lombok.AllArgsConstructor;
import okhttp3.Cookie;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@AllArgsConstructor
public class LoginManager {

    private final SharedPreferences prefs;
    private final OkHttpClient httpClient;

    public boolean doLoginRequest() throws IOException {
        String savedUser = prefs.getString("USERNAME", null);
        String savedPass = prefs.getString("PASSWORD", null);
        return this.doLoginRequest(savedUser, savedPass);
    }

    public boolean doLoginRequest(String username, String password) throws IOException {
        RequestBody formData = new FormBody.Builder()
                .add("domain-id", "8")
                .add("password", password)
                .add("username", username)
                .add("permanent-login", "0")
                .build();

        Request request = new Request.Builder()
                .url("https://a5.fhv.at/ajax/120/LoginResponsive/LoginHandler")
                .post(formData)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            this.saveCookie(httpClient.cookieJar().loadForRequest(null).get(0));
            return response.isSuccessful();
        }
    }

    public void logout(){
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("USERNAME");
        editor.remove("PASSWORD");
        editor.remove("PHPSESSID");
        editor.apply();
    }

    public void saveCredentials(String username, String password, Cookie cookie) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("USERNAME", username);
        editor.putString("PASSWORD", password);
        this.saveCookie(cookie);
        editor.apply();
    }

    private void saveCookie(Cookie cookie){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("PHPSESSID", cookie.value());
        editor.apply();
    }
}
