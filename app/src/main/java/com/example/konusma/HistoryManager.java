package com.example.konusma;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;

public class HistoryManager {
    private ArrayList<HistoryEntry> entries;
    private ArrayList<Integer> visibleEntries;

    HistoryManager(String jsonString) {
        try {
            fromJSONArray(new JSONArray(jsonString));
        } catch (JSONException e) {
            entries = new ArrayList<>();
        }
    }

    private void fromJSONArray(JSONArray jsonArray) {
        entries = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                entries.add(new HistoryEntry(jsonArray.getJSONObject(i)));
            } catch (JSONException e) {
                try {
                    entries.add(new HistoryEntry(jsonArray.getString(i)));
                } catch (JSONException f) {
                    entries.add(new HistoryEntry("{}"));
                }
            }
        }
    }

    private JSONArray toJSONArray() {
        JSONArray jsonArray = new JSONArray();
        for (HistoryEntry i : entries) {
            jsonArray.put(i.toJSONObject());
        }
        return jsonArray;
    }

    public String toJsonString() {
        return toJSONArray().toString();
    }

    public void addEntry(HistoryEntry entry) {
        if (entries.isEmpty()) entries.add(entry);
        else entries.add(0, entry);
    }

    public HistoryEntry getEntryAt(int index) {
        return entries.get(index);
    }

    public HistoryEntry getVisibleEntryAt(int index) {
        return visibleEntries == null ? getEntryAt(index) : entries.get(visibleEntries.get(index));
    }

    public int getEntryCount() {
        return entries.size();
    }

    public int getVisibleEntriesCount() {
        return visibleEntries == null ? getEntryCount() : visibleEntries.size();
    }

    public void removeEntryAt(int index) {
        entries.remove(index);
    }

    public void removeVisibleEntryAt(int index) {
        if (visibleEntries == null) removeEntryAt(index);
        else {
            entries.remove((int)visibleEntries.get(index));
            visibleEntries.remove(index);
        }
    }

    public void removeEntriesAt(ArrayList<Integer> indexes) {
        indexes.sort((Comparator.reverseOrder()));
        for (Integer i : indexes) removeVisibleEntryAt(i);
    }

    public void find(String query) {
        visibleEntries = new ArrayList<>();
        if (!query.isEmpty()) {
            query = query.toLowerCase();
            for (int i = 0; i < entries.size(); i++) {
                String content = entries.get(i).name.toLowerCase();
                if (content.contains(query)) visibleEntries.add(i);
            }
        }
    }

    public void cancelSearch() {
        visibleEntries = null;
    }

    public void removeIfPathNotExist() {
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (!new File(entries.get(i).path).exists()) removeEntryAt(i);
        }
    }
}