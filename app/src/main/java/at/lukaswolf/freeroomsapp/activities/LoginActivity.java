package at.lukaswolf.freeroomsapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import at.lukaswolf.freeroomsapp.MyApp;
import at.lukaswolf.freeroomsapp.R;
import at.lukaswolf.freeroomsapp.SimpleCookieJar;
import okhttp3.Cookie;
import okhttp3.OkHttpClient;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText editUsername;
    private EditText editPassword;
    private TextView textLoginStatus;
    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        textLoginStatus = findViewById(R.id.textLoginStatus);

        Button btnDoLogin = findViewById(R.id.btnDoLogin);

        // Globaler Client
        httpClient = MyApp.getHttpClient();

        btnDoLogin.setOnClickListener(view -> doLoginOnClick());
    }

    private void doLoginOnClick() {
        final String user = editUsername.getText().toString().trim();
        final String pass = editPassword.getText().toString().trim();

        // Netzwerkaufruf im Thread
        new Thread(() -> {
            boolean success = false;
            try {
                success = doLoginRequest(user, pass);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (success) {
                // Speichere Credentials in SharedPreferences
                saveCredentials(user, pass, ((SimpleCookieJar)httpClient.cookieJar()).loadForRequest().get(0));
                // Wechsle zur MainActivity
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                // Fehler anzeigen
                runOnUiThread(() -> textLoginStatus.setText("Login fehlgeschlagen!"));
            }
        }).start();
    }

    private boolean doLoginRequest(String username, String password) throws IOException {
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
            return response.isSuccessful();
        }
    }

    private void saveCredentials(String username, String password, Cookie cookie) {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("USERNAME", username);
        editor.putString("PASSWORD", password);
        editor.putString("PHPSESSID", cookie.value());
        editor.apply();
    }
}
