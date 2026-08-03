package it.dhd.oxygencustomizer.utils;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import it.dhd.oxygencustomizer.BuildConfig;
import it.dhd.oxygencustomizer.utils.overlay.OverlayUtil;

public class PrefManager {
    private static final String TAG = "PrefManager";

    private static final String TYPE = "type";
    private static final String VALUE = "value";
    private static final String T_BOOLEAN = "boolean";
    private static final String T_STRING = "string";
    private static final String T_INTEGER = "integer";
    private static final String T_LONG = "long";
    private static final String T_FLOAT = "float";
    private static final String T_STRING_SET = "string_set";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @SuppressWarnings("UnusedReturnValue")
    public static boolean exportPrefs(SharedPreferences preferences, final @NonNull OutputStream outputStream) {
        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonObject();

            for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                JsonObject item = new JsonObject();

                if (value instanceof Boolean) {
                    item.addProperty(TYPE, T_BOOLEAN);
                    item.addProperty(VALUE, (Boolean) value);
                } else if (value instanceof String) {
                    item.addProperty(TYPE, T_STRING);
                    item.addProperty(VALUE, (String) value);
                } else if (value instanceof Integer) {
                    item.addProperty(TYPE, T_INTEGER);
                    item.addProperty(VALUE, (Integer) value);
                } else if (value instanceof Long) {
                    item.addProperty(TYPE, T_LONG);
                    item.addProperty(VALUE, (Long) value);
                } else if (value instanceof Float) {
                    item.addProperty(TYPE, T_FLOAT);
                    item.addProperty(VALUE, (Float) value);
                } else if (value instanceof Set) {
                    item.addProperty(TYPE, T_STRING_SET);
                    JsonArray arr = new JsonArray();
                    for (Object s : (Set<?>) value) {
                        arr.add(new JsonPrimitive(String.valueOf(s)));
                    }
                    item.add(VALUE, arr);
                } else {
                    Log.w(TAG, "Skipping unsupported type " + value.getClass().getName() + " for key " + key);
                    continue;
                }

                root.add(key, item);
            }

            gson.toJson(root, writer);
            writer.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error exporting preferences", BuildConfig.DEBUG ? e : null);
            return false;
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean importPath(SharedPreferences sharedPreferences, final @NonNull InputStream inputStream) {
        JsonObject root;
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing preferences file", BuildConfig.DEBUG ? e : null);
            return false;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            try {
                String key = entry.getKey();
                JsonObject item = entry.getValue().getAsJsonObject();
                String type = item.get(TYPE).getAsString();
                JsonElement val = item.get(VALUE);

                switch (type) {
                    case T_BOOLEAN: {
                        boolean b = val.getAsBoolean();
                        editor.putBoolean(key, b);
                        if (key.contains("overlay")) {
                            if (b) {
                                OverlayUtil.enableOverlay(key);
                            } else {
                                OverlayUtil.disableOverlay(key);
                            }
                        }
                        break;
                    }
                    case T_STRING:
                        editor.putString(key, val.getAsString());
                        break;
                    case T_INTEGER:
                        editor.putInt(key, val.getAsInt());
                        break;
                    case T_LONG:
                        editor.putLong(key, val.getAsLong());
                        break;
                    case T_FLOAT:
                        editor.putFloat(key, val.getAsFloat());
                        break;
                    case T_STRING_SET: {
                        Set<String> set = new HashSet<>();
                        for (JsonElement elem : val.getAsJsonArray()) {
                            set.add(elem.getAsString());
                        }
                        editor.putStringSet(key, set);
                        break;
                    }
                    default:
                        Log.w(TAG, "Skipping unknown type " + type + " for key " + key);
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error importing key " + entry.getKey(), BuildConfig.DEBUG ? e : null);
            }
        }

        return editor.commit();
    }

    public static void clearPrefs(SharedPreferences preferences) {
        preferences.edit().clear().commit();
    }
}
