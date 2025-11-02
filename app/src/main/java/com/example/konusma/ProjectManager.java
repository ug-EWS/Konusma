package com.example.konusma;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;

public class ProjectManager {
    private final ArrayList<Project> projects;

    ProjectManager(String _jsonString) {
        projects = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(_jsonString);
            for (int i = 0; i < array.length(); i++)
                projects.add(new Project(array.getJSONObject(i)));
        } catch (JSONException ignored) {
        }
    }

    public void createNewProject(String name) {
        if (projects.isEmpty()) projects.add(new Project(name)); else projects.add(0, new Project(name));
    }

    public Project getProjectAt(int index) {
        return projects.get(index);
    }

    public int getIndexOf(Project project) {
        return projects.indexOf(project);
    }

    public int getProjectCount() {
        return projects.size();
    }

    public void sort() {
        projects.sort(Comparator.comparing(project -> project.isProgress() ? 0 : project.isComplete() ? 1 : 2, Comparator.naturalOrder()));
    }

    public void removeProject(int index) {
        projects.remove(index);
    }

    public String toJsonString() {
        JSONArray array = new JSONArray();
        for (Project i : projects) array.put(i.toJSONObject());
        return array.toString();
    }

    public static class Project {
        public String name;
        public int status;
        public ArrayList<HistoryEntry> files;

        public static final int COMPLETE = 0;
        public static final int PROGRESS = 1;
        public static final int CANCELED = 2;

        Project(String _name) {
            name = _name;
            status = PROGRESS;
            files = new ArrayList<>();
        }

        Project(JSONObject _jsonObject) {
            files = new ArrayList<>();
            try {
                name = _jsonObject.optString("name");
                status = _jsonObject.optInt("status", 1);
                JSONArray array = _jsonObject.getJSONArray("files");
                for (int i = 0; i < array.length(); i++)
                    files.add(new HistoryEntry(array.getJSONObject(i)));
            } catch (JSONException ignored) {
            }
        }

        public boolean isComplete() {
            return status == COMPLETE;
        }

        public boolean isProgress() {
            return status == PROGRESS;
        }

        public boolean isCanceled() {
            return status == CANCELED;
        }

        public void completeProject() {
            status = COMPLETE;
        }

        public void continueProject() {
            status = PROGRESS;
        }

        public void restartProject() {
            files.clear();
            status = PROGRESS;
        }

        public ArrayList<HistoryEntry> cancelProject() {
            status = CANCELED;
            return files;
        }

        public void addFile(HistoryEntry _file) {
            if (files.isEmpty()) files.add(_file);
            else files.add(0, _file);
        }

        public HistoryEntry getFileAt(int index) {
            return files.get(index);
        }

        public int getFileCount() {
            return files.size();
        }

        public HistoryEntry removeFileAt(int index) {
            HistoryEntry file = files.get(index);
            files.remove(index);
            return file;
        }

        public void removeAllFiles() {
            files.clear();
        }

        public JSONObject toJSONObject() {
            JSONObject object = new JSONObject();
            try {
                object.put("name", name);
                object.put("status", status);
                JSONArray array = new JSONArray();
                for (HistoryEntry i : files) array.put(i.toJSONObject());
                object.put("files", array);
            } catch (JSONException ignored) {
            }
            return object;
        }
    }
}