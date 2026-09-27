package me.uc_hussein.ultrassettings.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/** Fluent %token% substitution applied BEFORE MiniMessage parsing. */
public final class PlaceholderUtil {

    private final Map<String, String> values = new LinkedHashMap<>();

    public static PlaceholderUtil create() {
        return new PlaceholderUtil();
    }

    public PlaceholderUtil put(String key, Object value) {
        values.put(key, String.valueOf(value));
        return this;
    }

    public String apply(String template) {
        if (template == null) return "";
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }

    public java.util.List<String> applyAll(java.util.List<String> templates) {
        return templates.stream().map(this::apply).toList();
    }
}
