package org.json;

public class JSONObject {
    public JSONObject() {}
    public JSONObject(String source) {}
    public JSONObject put(String name, Object value) { return this; }
    public JSONObject put(String name, int value) { return this; }
    public String optString(String name) { return ""; }
    public int optInt(String name, int fallback) { return fallback; }
    public JSONObject getJSONObject(String name) { return null; }
    public JSONObject optJSONObject(String name) { return null; }
    public String toString(int indentSpaces) { return ""; }
}
