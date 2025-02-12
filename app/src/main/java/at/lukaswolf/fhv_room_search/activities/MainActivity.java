package at.lukaswolf.fhv_room_search.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import at.lukaswolf.fhv_room_search.FHVRoomSearch;
import at.lukaswolf.fhv_room_search.R;
import at.lukaswolf.fhv_room_search.manager.LoginManager;
import at.lukaswolf.fhv_room_search.manager.RestManager;
import at.lukaswolf.fhv_room_search.manager.RoomEvaluator;

public class MainActivity extends AppCompatActivity {

    private EditText editDate;
    private EditText editHours;
    private CheckBox checkDebug;
    private TextView textResult;
    private LoginManager loginManager;
    private RestManager restManager;
    private Handler uiHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editDate   = findViewById(R.id.editDate);
        editHours  = findViewById(R.id.editHours);
        checkDebug = findViewById(R.id.checkDebug);
        textResult = findViewById(R.id.textResult);

        Button btnCheck = findViewById(R.id.btnCheck);
        Button btnLogout = findViewById(R.id.btnLogout);

        loginManager = FHVRoomSearch.getLoginManager();
        restManager = FHVRoomSearch.getRestManager();
        uiHandler = new Handler(Looper.getMainLooper());

        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String savedUser = prefs.getString("USERNAME", null);
        String savedPass = prefs.getString("PASSWORD", null);

        if (savedUser == null || savedPass == null) {
            Toast.makeText(this, "Bitte einloggen!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if(!loginManager.checkIfStillLoggedIn()){
            startActivity(new Intent(this, LoginActivity.class));
        }

        btnCheck.setOnClickListener(view -> onCheckClicked());
        btnLogout.setOnClickListener(view -> {
            loginManager.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void onCheckClicked() {
        textResult.setText("Starte Abfrage...");

        String dateInput = editDate.getText().toString().trim();
        String finalDate;
        if (dateInput.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            finalDate = sdf.format(new Date()); // heute
        } else {
            finalDate = dateInput;
        }

        int hoursToCheck = 3;
        try {
            String hStr = editHours.getText().toString().trim();
            if (!hStr.isEmpty()) {
                hoursToCheck = Integer.parseInt(hStr);
            }
        } catch (NumberFormatException e) {
            // Es wurde keine gültige Zahl eingegeben -> 3 wird verwendet
        }

        boolean debugMode = checkDebug.isChecked();

        int finalHoursToCheck = hoursToCheck;
        Thread worker = new Thread(() -> {
            try {
                if (!restManager.selectRooms()) {
                    postResult("Raum-Auswahl fehlgeschlagen. Evtl. Session ungültig?");
                    reLogin();
                    return;
                }

                String scheduleJson = restManager.loadSchedule(finalDate);
                if (scheduleJson == null) {
                    postResult("Stundenplan-Abfrage fehlgeschlagen -> evtl. Session ungültig");
                    reLogin();
                    return;
                }

                if (scheduleJson.trim().equals("[]")) {
                    postResult("Session ist nicht mehr gültig -> Re-Login");
                    reLogin();
                    return;
                }

                RoomEvaluator evaluator = new RoomEvaluator(scheduleJson, finalHoursToCheck, debugMode);
                postResult(evaluator.evaluateRooms());
            } catch (Exception e) {
                e.printStackTrace();
                postResult("Fehler: " + e.getMessage());
            }
        });
        worker.start();
    }

    private void postResult(final String text) {
        uiHandler.post(() -> textResult.setText(text));
    }

    private void reLogin() {
        new Thread(() -> {
            boolean success = false;
            try {
                success = loginManager.doLoginRequest();
            } catch (IOException e) {
                e.printStackTrace();
                loginManager.logout();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
            if (success) {
                postResult("Re-Login erfolgreich. Du kannst jetzt weiterarbeiten.");
            } else {
                loginManager.logout();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        }).start();
    }
}
