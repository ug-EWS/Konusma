package com.example.konusma;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class HistoryEntry {
    String name;
    String path;

    HistoryEntry(String _name, String _filename) {
        name = _name;
        path = _filename;
    }

    HistoryEntry(String jsonString) {
        try {
            fromJSONObject(new JSONObject(jsonString));
        } catch (JSONException e) {
            name = "";
            path = "";
        }
    }

    HistoryEntry(JSONObject jsonObject) {
        fromJSONObject(jsonObject);
    }

    private void fromJSONObject(JSONObject jsonObject) {
        name = jsonObject.optString("name");
        path = jsonObject.optString("path");
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", name)
                    .put("path", path);
        } catch (JSONException ignored) {
        }
        return jsonObject;
    }
}
