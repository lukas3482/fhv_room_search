package at.lukaswolf.fhv_room_search.manager;

import java.io.IOException;

import at.lukaswolf.fhv_room_search.enums.Room;
import lombok.AllArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@AllArgsConstructor
public class RestManager {
    private final OkHttpClient httpClient;

    public boolean selectRooms() throws IOException {
        String url = "https://a5.fhv.at/ajax/122/EventPlanerSite/SessionSaveJsonPage?roomIds=" + Room.getRoomIdStr();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            return response.isSuccessful();
        }
    }

    public String loadSchedule(String dateStr) throws IOException {
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
}
