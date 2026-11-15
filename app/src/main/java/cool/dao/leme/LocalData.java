package cool.dao.leme;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class LocalData {
    static final String PREFS = "dao_leme";
    final LinkedHashMap<String, JSONObject> records = new LinkedHashMap<>();
    final ArrayList<TypeOption> types = new ArrayList<>();
    boolean reminderEnabled;
    String reminderTime = "23:45";
    String theme = "system";

    static final class TypeOption {
        final String id;
        String name;
        boolean active;

        TypeOption(String id, String name, boolean active) {
            this.id = id;
            this.name = name;
            this.active = active;
        }
    }

    static LocalData load(Context context) {
        String saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("native_state", "");
        LocalData data = new LocalData();
        if (!saved.isEmpty()) {
            try { data.mergeJson(saved); }
            catch (JSONException exception) { data = new LocalData(); }
        }
        data.ensureActiveTypes();
        return data;
    }

    private void ensureActiveTypes() {
        for (TypeOption type : types) if (type.active) return;
        for (String name : new String[]{"干导", "看片导", "睡前导", "晨导"}) {
            TypeOption existing = findType(name);
            if (existing == null) types.add(new TypeOption(UUID.randomUUID().toString(), name, true));
            else existing.active = true;
        }
    }

    private TypeOption findType(String name) {
        for (TypeOption type : types) if (type.name.equals(name)) return type;
        return null;
    }

    JSONObject toJson() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("version", 1);
        root.put("exportedAt", java.time.Instant.now().toString());
        JSONObject savedRecords = new JSONObject();
        for (Map.Entry<String, JSONObject> record : records.entrySet()) savedRecords.put(record.getKey(), record.getValue());
        root.put("records", savedRecords);
        JSONArray savedTypes = new JSONArray();
        for (TypeOption type : types) {
            JSONObject item = new JSONObject();
            item.put("id", type.id);
            item.put("name", type.name);
            item.put("active", type.active);
            savedTypes.put(item);
        }
        root.put("types", savedTypes);
        JSONObject settings = new JSONObject();
        settings.put("reminderEnabled", reminderEnabled);
        settings.put("reminderTime", reminderTime);
        settings.put("theme", theme);
        root.put("settings", settings);
        return root;
    }

    void save(Context context) {
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("native_state", toJson().toString()).apply();
        } catch (JSONException exception) {
            throw new IllegalStateException("Could not serialize local data", exception);
        }
        updateTodayPreference(context);
        ReminderReceiver.updateSchedule(context, reminderEnabled, reminderTime);
    }

    void updateTodayPreference(Context context) {
        String date = LocalDate.now().toString();
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("recorded_date", date).putBoolean("recorded", records.containsKey(date)).apply();
    }

    // Validate the entire backup before changing any live data. Old app exports use this same schema.
    void mergeJson(String raw) throws JSONException {
        JSONObject incoming = new JSONObject(raw);
        JSONObject incomingRecords = incoming.optJSONObject("records");
        JSONArray incomingTypes = incoming.optJSONArray("types");
        if (incomingRecords == null || incomingTypes == null) throw new JSONException("Missing records or types");

        LinkedHashMap<String, JSONObject> validRecords = new LinkedHashMap<>();
        for (Iterator<String> keys = incomingRecords.keys(); keys.hasNext();) {
            String date = keys.next();
            LocalDate parsed;
            try { parsed = LocalDate.parse(date); }
            catch (DateTimeParseException exception) { throw new JSONException("Invalid date: " + date); }
            if (!date.equals(parsed.toString())) throw new JSONException("Date must be yyyy-MM-dd");
            JSONObject record = incomingRecords.optJSONObject(date);
            if (record == null || !("导了".equals(record.optString("state")) || "没导".equals(record.optString("state")))) {
                throw new JSONException("Invalid daily state");
            }
            if (!parsed.isAfter(LocalDate.now())) validRecords.put(date, new JSONObject(record.toString()));
        }
        ArrayList<TypeOption> validTypes = new ArrayList<>();
        for (int index = 0; index < incomingTypes.length(); index++) {
            JSONObject item = incomingTypes.optJSONObject(index);
            if (item == null) throw new JSONException("Invalid type");
            String name = item.optString("name", "").trim();
            if (name.isEmpty() || name.length() > 20) throw new JSONException("Invalid type name");
            validTypes.add(new TypeOption(item.optString("id", UUID.randomUUID().toString()), name, item.optBoolean("active", true)));
        }

        Map<String, String> mappedIds = new LinkedHashMap<>();
        for (TypeOption incomingType : validTypes) {
            TypeOption target = findType(incomingType.name);
            if (target == null) {
                String id = incomingType.id;
                for (TypeOption type : types) if (type.id.equals(id)) { id = UUID.randomUUID().toString(); break; }
                if (id.isEmpty()) id = UUID.randomUUID().toString();
                target = new TypeOption(id, incomingType.name, incomingType.active);
                types.add(target);
            }
            mappedIds.put(incomingType.id, target.id);
        }
        for (Map.Entry<String, JSONObject> saved : validRecords.entrySet()) {
            JSONObject record = saved.getValue();
            String oldId = record.optString("typeId", "");
            if (mappedIds.containsKey(oldId)) record.put("typeId", mappedIds.get(oldId));
            if ("没导".equals(record.optString("state"))) { record.remove("typeId"); record.remove("typeName"); }
            records.put(saved.getKey(), record);
        }
        JSONObject settings = incoming.optJSONObject("settings");
        if (settings != null) {
            reminderEnabled = settings.optBoolean("reminderEnabled", reminderEnabled);
            String time = settings.optString("reminderTime", reminderTime);
            if (time.matches("(?:[01]\\d|2[0-3]):[0-5]\\d")) reminderTime = time;
            String requestedTheme = settings.optString("theme", theme);
            if ("system".equals(requestedTheme) || "light".equals(requestedTheme) || "dark".equals(requestedTheme)) theme = requestedTheme;
        }
        ensureActiveTypes();
    }

    Map<String, Statistics.Entry> statisticsRecords() {
        Map<String, Statistics.Entry> result = new LinkedHashMap<>();
        for (Map.Entry<String, JSONObject> item : records.entrySet()) {
            result.put(item.getKey(), new Statistics.Entry(item.getValue().optString("state", ""), item.getValue().optString("typeName", "")));
        }
        return result;
    }
}
