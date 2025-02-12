package at.lukaswolf.freeroomsapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import at.lukaswolf.freeroomsapp.FreeRoomsApp;
import at.lukaswolf.freeroomsapp.R;
import at.lukaswolf.freeroomsapp.cookies.SimpleCookieJar;
import at.lukaswolf.freeroomsapp.manager.LoginManager;
import okhttp3.OkHttpClient;

public class LoginActivity extends AppCompatActivity {

    private EditText editUsername;
    private EditText editPassword;
    private TextView textLoginStatus;
    private OkHttpClient httpClient;
    private LoginManager loginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        textLoginStatus = findViewById(R.id.textLoginStatus);

        Button btnDoLogin = findViewById(R.id.btnDoLogin);

        httpClient = FreeRoomsApp.getHttpClient();
        loginManager = FreeRoomsApp.getLoginManager();

        btnDoLogin.setOnClickListener(view -> doLoginOnClick());
    }

    private void doLoginOnClick() {
        final String user = editUsername.getText().toString().trim();
        final String pass = editPassword.getText().toString().trim();

        new Thread(() -> {
            boolean success = false;
            try {
                success = loginManager.doLoginRequest(user, pass);
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> textLoginStatus.setText("Login fehlgeschlagen!"));
            }

            if (success) {
                loginManager.saveCredentials(user, pass, ((SimpleCookieJar)httpClient.cookieJar()).loadForRequest().get(0));
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {

                runOnUiThread(() -> textLoginStatus.setText("Login fehlgeschlagen!"));
            }
        }).start();
    }
}
