package at.lukaswolf.freeroomsapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import at.lukaswolf.freeroomsapp.MyApp;
import at.lukaswolf.freeroomsapp.R;
import okhttp3.Cookie;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.FormBody;
import okhttp3.Response;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class MainActivity extends AppCompatActivity {

    private EditText editDate;
    private EditText editHours;
    private CheckBox checkDebug;
    private TextView textResult;

    private OkHttpClient httpClient;

    private static final String[][] ROOM_MAP = {
            {"39",  "U130 eLab"},
            {"41",  "U204"},
            {"42",  "U205"},
            {"43",  "U206"},
            {"44",  "U207"},
            {"45",  "U210"},
            {"46",  "U212"},
            {"47",  "U214"},
            {"48",  "U215"},
            {"297", "U216"},
            {"49",  "U225"},
            {"50",  "U226"},
            {"51",  "U227"},
            {"52",  "U304"},
            {"53",  "U305"},
            {"4",   "U306"},
            {"55",  "U307"},
            {"56",  "U310"},
            {"57",  "U311"},
            {"58",  "U312"},
            {"59",  "U313"},
            {"60",  "U314"},
            {"61",  "U315"},
            {"82",  "U325"},
            {"63",  "U326"},
            {"64",  "U327"},
            {"65",  "U328"},
            {"66",  "U329"},
            {"67",  "U330"},
            {"68",  "U404"},
            {"69",  "U405"},
            {"70",  "U406"},
            {"71",  "U407"},
            {"72",  "U410"},
            {"73",  "U411"},
            {"74",  "U412"},
            {"75",  "U413"},
            {"76",  "U414"},
            {"77",  "U415"},
            {"78",  "U425"},
            {"79",  "U426"},
            {"80",  "U427"},
            {"81",  "U428"},
            {"62",  "U429"},
            {"83",  "U430"}
    };

    private String roomIdsStr;
    private Handler uiHandler;

    // Gespeicherte Login-Daten
    private String savedUser;
    private String savedPass;

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

        httpClient = MyApp.getHttpClient();
        uiHandler = new Handler(Looper.getMainLooper());

        // Raum-IDs in Kommaliste
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ROOM_MAP.length; i++) {
            sb.append(ROOM_MAP[i][0]);
            if (i < ROOM_MAP.length - 1) sb.append(",");
        }
        roomIdsStr = sb.toString();

        // Schauen, ob wir gespeicherte Credentials haben
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        savedUser = prefs.getString("USERNAME", null);
        savedPass = prefs.getString("PASSWORD", null);

        // Falls nicht vorhanden => LoginActivity starten
        if (savedUser == null || savedPass == null) {
            Toast.makeText(this, "Bitte einloggen!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Sonst versuchen wir sofort, ob wir NOCH eingeloggt sind:
        //   -> selectRooms + loadSchedule => wenn [] => nicht eingeloggt => LoginActivity
        checkIfStillLoggedIn();

        // Button-Listener
        btnCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCheckClicked();
            }
        });

        btnLogout.setOnClickListener(view -> {
            SharedPreferences preferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("USERNAME");
            editor.remove("PASSWORD");
            editor.remove("PHPSESSID");
            editor.apply();

            startActivity(new Intent(this, LoginActivity.class));
        });
    }

    private void checkIfStillLoggedIn() {
        Thread t = new Thread(() -> {
            try {
                // selectRooms
                boolean selOk = selectRooms();
                if (!selOk) {
                    postResult("Raum-Auswahl fehlgeschlagen -> evtl. nicht eingeloggt");
                    reLogin(); // erneut loggen
                    return;
                }

                // loadSchedule
                // Wir holen testweise das JSON: wenn "[]", nicht eingeloggt
                String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                String schedule = loadSchedule(dateStr);
                if (schedule == null) {
                    postResult("Stundenplan-Abfrage fehlgeschlagen. (evtl. nicht eingeloggt)");
                    reLogin();
                    return;
                }

                // Prüfen, ob schedule == "[]"
                if (schedule.trim().equals("[]")) {
                    // nicht eingeloggt
                    postResult("Leere Array-Antwort => Session ungültig => bitte neu einloggen");
                    reLogin();
                    return;
                }

                // Alles okay => wir tun nichts, user kann normal weiter machen

            } catch (Exception e) {
                e.printStackTrace();
                postResult("Fehler: " + e.getMessage());
                // evtl. reLogin()
            }
        });
        t.start();
    }

    /**
     * Falls loadSchedule "[]" zurückgibt,
     * führen wir einen Login mit den gespeicherten Credentials durch.
     */
    private void reLogin() {
        new Thread(() -> {
            boolean success = false;
            try {
                success = doLoginRequest(savedUser, savedPass);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (success) {
                postResult("Re-Login erfolgreich. Du kannst jetzt weiterarbeiten.");
            } else {
                // Falls das auch scheitert => LoginActivity anzeigen
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        }).start();
    }

    private boolean doLoginRequest(String user, String pass) throws IOException {
        RequestBody formData = new FormBody.Builder()
                .add("domain-id", "8")
                .add("password", pass)
                .add("username", user)
                .add("permanent-login", "0")
                .build();

        Request request = new Request.Builder()
                .url("https://a5.fhv.at/ajax/120/LoginResponsive/LoginHandler")
                .post(formData)
                .build();
        System.out.println("Looooooggggginnnn!!!!");
        try (Response response = httpClient.newCall(request).execute()) {
            this.saveCookie(httpClient.cookieJar().loadForRequest(null).get(0));
            return response.isSuccessful();
        }
    }

    private void saveCookie(Cookie cookie){
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("PHPSESSID", cookie.value());
        editor.apply();
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
            // Standard 3
        }

        boolean debugMode = checkDebug.isChecked();

        int finalHoursToCheck = hoursToCheck;
        Thread worker = new Thread(() -> {
            try {
                // selectRooms
                if (!selectRooms()) {
                    postResult("Raum-Auswahl fehlgeschlagen. Evtl. Session ungültig?");
                    reLogin();
                    return;
                }

                // schedule
                String scheduleJson = loadSchedule(finalDate);
                if (scheduleJson == null) {
                    postResult("Stundenplan-Abfrage fehlgeschlagen -> evtl. Session ungültig");
                    reLogin();
                    return;
                }

                if (scheduleJson.trim().equals("[]")) {
                    postResult("Schedule == [] => nicht eingeloggt => Re-Login");
                    reLogin();
                    return;
                }

                // Daten auswerten
                String resultText = evaluateRooms(scheduleJson, finalHoursToCheck, debugMode);
                postResult(resultText);

            } catch (Exception e) {
                e.printStackTrace();
                postResult("Fehler: " + e.getMessage());
            }
        });
        worker.start();
    }

    private boolean selectRooms() throws IOException {
        String url = "https://a5.fhv.at/ajax/122/EventPlanerSite/SessionSaveJsonPage?roomIds=" + roomIdsStr;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    private String loadSchedule(String dateStr) throws IOException {
        String url = "https://a5.fhv.at/ajax/122/EventPlanerSite/EventDateSiteJsonPage"
                + "?from=" + dateStr
                + "&to=" + dateStr;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            return response.body().string();
        }
    }

    private String evaluateRooms(String scheduleJson, int hoursToCheck, boolean debugMode) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ergebnisse (").append(hoursToCheck).append("h)\n\n");

        long nowMillis = System.currentTimeMillis();
        long endCheckMillis = nowMillis + hoursToCheck * 3600_000L;

        java.util.HashMap<Integer, java.util.ArrayList<long[]>> roomEvents = new java.util.HashMap<>();

        try {
            JSONObject root = new JSONObject(scheduleJson);
            JSONArray dataArray = root.optJSONArray("data");
            if (dataArray != null) {
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject ev = dataArray.getJSONObject(i);
                    String startStr = ev.optString("start_date", "");
                    String endStr   = ev.optString("end_date", "");
                    String roomIds  = ev.optString("roomIds", "");
                    if (startStr.isEmpty() || endStr.isEmpty() || roomIds.isEmpty()) {
                        continue;
                    }
                    long startMs = parseDateToMillis(startStr);
                    long endMs   = parseDateToMillis(endStr);

                    String[] splitted = roomIds.split(",");
                    for (String s : splitted) {
                        s = s.trim();
                        if (!s.isEmpty()) {
                            int rId = Integer.parseInt(s);
                            roomEvents.putIfAbsent(rId, new java.util.ArrayList<>());
                            roomEvents.get(rId).add(new long[]{startMs, endMs});
                        }
                    }
                }
            }

            for (String[] roomInfo : ROOM_MAP) {
                int rId = Integer.parseInt(roomInfo[0]);
                String rName = roomInfo[1];

                if (!roomEvents.containsKey(rId)) {
                    sb.append(rName).append(" ist den ganzen Zeitraum frei.\n");
                    continue;
                }

                java.util.ArrayList<long[]> intervals = roomEvents.get(rId);
                intervals.sort((a, b) -> Long.compare(a[0], b[0]));

                boolean isCurrentlyBelegt = false;
                Long nextStart = null;

                for (long[] interval : intervals) {
                    long start = interval[0];
                    long end   = interval[1];
                    if (start <= nowMillis && nowMillis < end) {
                        isCurrentlyBelegt = true;
                        if (debugMode) {
                            sb.append(rName).append(" ist derzeit belegt.\n");
                        }
                        break;
                    }
                    if (start > nowMillis) {
                        nextStart = start;
                        break;
                    }
                }

                if (isCurrentlyBelegt) {
                    continue;
                }

                if (nextStart == null) {
                    sb.append(rName).append(" ist den ganzen Zeitraum frei.\n");
                } else {
                    if (nextStart > endCheckMillis) {
                        long duration = endCheckMillis - nowMillis;
                        double hrs = duration / 3600000.0;
                        sb.append(rName)
                                .append(" ist frei bis > ")
                                .append(formatMillis(nextStart))
                                .append(" (~ ")
                                .append(String.format(Locale.getDefault(), "%.1f", hrs))
                                .append("h)\n");
                    } else {
                        double hrs = (nextStart - nowMillis) / 3600000.0;
                        if (hrs <= 0 && debugMode) {
                            sb.append(rName).append(" ist belegt (Start jetzt).\n");
                        } else if (hrs > 0) {
                            sb.append(rName)
                                    .append(" ist frei bis ")
                                    .append(formatMillis(nextStart))
                                    .append(" (~ ")
                                    .append(String.format(Locale.getDefault(), "%.1f", hrs))
                                    .append("h)\n");
                        }
                    }
                }
            }

        } catch (JSONException e) {
            return "JSON-Fehler: " + e.getMessage();
        }
        return sb.toString();
    }

    private long parseDateToMillis(String dateTimeStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date d = sdf.parse(dateTimeStr);
            if (d != null) {
                return d.getTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0L;
    }

    private String formatMillis(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    private void postResult(final String text) {
        uiHandler.post(() -> textResult.setText(text));
    }
}
