package com.campus.activity.activity.util;

import com.campus.activity.activity.entity.Activity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ActivityDiffUtils {

    private static final Set<String> IGNORE_KEYS = Set.of("updatedAt", "createdAt", "version", "updatedBy", "createdBy");

    public static String diff(ObjectMapper objectMapper, Activity before, Activity after) {
        try {
            Map<String, Object> beforeMap = objectMapper.convertValue(before, Map.class);
            Map<String, Object> afterMap = objectMapper.convertValue(after, Map.class);

            Map<String, Map<String, Object>> changed = new HashMap<>();
            for (String key : afterMap.keySet()) {
                if (IGNORE_KEYS.contains(key)) {
                    continue;
                }
                Object b = beforeMap.get(key);
                Object a = afterMap.get(key);
                if (b == null && a == null) {
                    continue;
                }
                if (b == null || a == null || !b.equals(a)) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("before", b);
                    item.put("after", a);
                    changed.put(key, item);
                }
            }
            return objectMapper.writeValueAsString(changed);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            return "{}";
        }
    }
}
