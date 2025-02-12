package at.lukaswolf.freeroomsapp.manager;

import org.json.*;

import java.text.SimpleDateFormat;
import java.util.*;

import at.lukaswolf.freeroomsapp.enums.Room;

public class RoomEvaluator {
    private final String scheduleJson;
    private final int hoursToCheck;
    private final boolean debugMode;
    private final long nowMillis;
    private final long endCheckMillis;
    private final Map<Integer, List<long[]>> roomEvents;

    public RoomEvaluator(String scheduleJson, int hoursToCheck, boolean debugMode) {
        this.scheduleJson = scheduleJson;
        this.hoursToCheck = hoursToCheck;
        this.debugMode = debugMode;
        this.nowMillis = System.currentTimeMillis();
        this.endCheckMillis = nowMillis + hoursToCheck * 3600_000L;
        this.roomEvents = new HashMap<>();
    }

    public String evaluateRooms() {
        StringBuilder sb = new StringBuilder();
        sb.append("Ergebnisse (").append(hoursToCheck).append("h):\n\n");
        parseScheduleJson();
        processRooms(sb);
        return sb.toString();
    }

    private void parseScheduleJson() {
        try {
            JSONObject root = new JSONObject(scheduleJson);
            JSONArray dataArray = root.optJSONArray("data");
            if (dataArray != null) {
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject ev = dataArray.getJSONObject(i);
                    String startStr = ev.optString("start_date", "");
                    String endStr = ev.optString("end_date", "");
                    String roomIds = ev.optString("roomIds", "");
                    if (startStr.isEmpty() || endStr.isEmpty() || roomIds.isEmpty()) {
                        continue;
                    }
                    long startMs = parseDateToMillis(startStr);
                    long endMs = parseDateToMillis(endStr);
                    addRoomEvents(roomIds, startMs, endMs);
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException("JSON-Fehler: " + e.getMessage());
        }
    }

    private void addRoomEvents(String roomIds, long startMs, long endMs) {
        for (String s : roomIds.split(",")) {
            s = s.trim();
            if (!s.isEmpty()) {
                int rId = Integer.parseInt(s);
                roomEvents.putIfAbsent(rId, new ArrayList<>());
                roomEvents.get(rId).add(new long[]{startMs, endMs});
            }
        }
    }

    private void processRooms(StringBuilder sb) {
        for (Room roomInfo : Room.values()) {
            int rId = roomInfo.getId();
            String rName = roomInfo.getName();

            if (!roomEvents.containsKey(rId)) {
                sb.append(rName).append(" ist den ganzen Zeitraum frei.\n");
                continue;
            }

            List<long[]> intervals = roomEvents.get(rId);
            intervals.sort(Comparator.comparingLong(a -> a[0]));
            evaluateRoomAvailability(sb, rName, intervals);
        }
    }

    private void evaluateRoomAvailability(StringBuilder sb, String rName, List<long[]> intervals) {
        boolean isCurrentlyBelegt = false;
        Long nextStart = null;

        for (long[] interval : intervals) {
            long start = interval[0];
            long end = interval[1];
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

        appendRoomAvailability(sb, rName, isCurrentlyBelegt, nextStart);
    }

    private void appendRoomAvailability(StringBuilder sb, String rName, boolean isCurrentlyBelegt, Long nextStart) {
        if (isCurrentlyBelegt) {
            return;
        }
        if (nextStart == null) {
            sb.append(rName).append(" ist den ganzen Zeitraum frei.\n");
        } else if (nextStart > endCheckMillis) {
            sb.append(rName).append(" ist frei bis > ")
                    .append(formatMillis(nextStart))
                    .append(" (~ ")
                    .append(String.format(Locale.getDefault(), "%.1f", (endCheckMillis - nowMillis) / 3600000.0))
                    .append("h)\n");
        } else {
            double hrs = (nextStart - nowMillis) / 3600000.0;
            if (hrs <= 0 && debugMode) {
                sb.append(rName).append(" ist belegt (Start jetzt).\n");
            } else if (hrs > 0) {
                sb.append(rName).append(" ist frei bis ")
                        .append(formatMillis(nextStart))
                        .append(" (~ ")
                        .append(String.format(Locale.getDefault(), "%.1f", hrs))
                        .append("h)\n");
            }
        }
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
}

