package com.example.signuploginrealtime.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExerciseInfo {

    @SerializedName("id")
    private Integer id;

    // Not always provided by Wger; we’ll generate if null
    @SerializedName("uuid")
    private String uuid;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("language")
    private Integer language;

    /**
     * Wger can return:
     * - "category": 8
     * - or "category": { "id": 8, "name": "Chest" }
     * So we keep it as Object and unwrap in getters.
     */
    @SerializedName("category")
    private Object category; // Number or Map

    /**
     * Wger can return arrays of IDs or arrays of objects.
     * e.g. "muscles": [1, 2] OR [{id:1, name:"Pectoralis", name_en:"Chest"}, ...]
     */
    @SerializedName("muscles")
    private List<Object> muscles; // Number or Map entries

    @SerializedName("muscles_secondary")
    private List<Object> musclesSecondary; // Number or Map entries

    @SerializedName("equipment")
    private List<Object> equipment; // Number or Map entries

    public ExerciseInfo() {
        // Safeguard nullable lists
        this.muscles = new ArrayList<>();
        this.musclesSecondary = new ArrayList<>();
        this.equipment = new ArrayList<>();
    }

    // ---------- Basic getters/setters ----------

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUuid() {
        if (uuid == null || uuid.isEmpty()) {
            uuid = UUID.randomUUID().toString();
        }
        return uuid;
    }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public Integer getLanguage() { return language; }
    public void setLanguage(Integer language) { this.language = language; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    /** Strip simple HTML tags if present */
    public String getCleanDescription() {
        return description != null ? description.replaceAll("<.*?>", "").trim() : "";
    }

    public Object getCategoryRaw() { return category; }
    public void setCategory(Object category) { this.category = category; }

    // ---------- Category helpers ----------

    public Integer getCategoryId() {
        if (category == null) return null;
        if (category instanceof Number) return ((Number) category).intValue();
        if (category instanceof Map) {
            Object idObj = ((Map<?, ?>) category).get("id");
            if (idObj instanceof Number) return ((Number) idObj).intValue();
        }
        return null;
    }

    public String getCategoryName() {
        if (category instanceof Map) {
            Object nameObj = ((Map<?, ?>) category).get("name");
            if (nameObj instanceof String) return (String) nameObj;
        }
        return null;
    }

    // ---------- Muscles / Equipment raw access ----------

    public List<Object> getMusclesRaw() { return muscles; }
    public void setMusclesRaw(List<Object> muscles) {
        this.muscles = muscles != null ? muscles : new ArrayList<>();
    }

    public List<Object> getMusclesSecondaryRaw() { return musclesSecondary; }
    public void setMusclesSecondaryRaw(List<Object> musclesSecondary) {
        this.musclesSecondary = musclesSecondary != null ? musclesSecondary : new ArrayList<>();
    }

    public List<Object> getEquipmentRaw() { return equipment; }
    public void setEquipmentRaw(List<Object> equipment) {
        this.equipment = equipment != null ? equipment : new ArrayList<>();
    }

    // ---------- Friendly convenience getters your code expects ----------

    /** Alias for your adapter/logic: returns readable primary muscle names when possible */
    public List<String> getMuscleNames() {
        return extractNamesFromMixedList(muscles, "Muscle");
    }

    /** Optional: secondary muscles as names */
    public List<String> getSecondaryMuscleNames() {
        return extractNamesFromMixedList(musclesSecondary, "Muscle");
    }

    /** Equipment names (or "Equipment #id" fallbacks if API only returns IDs) */
    public List<String> getEquipmentNames() {
        return extractNamesFromMixedList(equipment, "Equipment");
    }

    // ---------- Friendly convenience setters (if you want to set names manually) ----------

    /** Allows setting muscle names directly (will overwrite raw list with simple strings) */
    public void setMuscleNames(List<String> muscleNames) {
        this.muscles = new ArrayList<>();
        if (muscleNames != null) this.muscles.addAll(muscleNames);
    }

    public void setSecondaryMuscleNames(List<String> muscleNames) {
        this.musclesSecondary = new ArrayList<>();
        if (muscleNames != null) this.musclesSecondary.addAll(muscleNames);
    }

    public void setEquipmentNames(List<String> equipmentNames) {
        this.equipment = new ArrayList<>();
        if (equipmentNames != null) this.equipment.addAll(equipmentNames);
    }

    // ---------- Helpers ----------

    @SuppressWarnings("unchecked")
    private List<String> extractNamesFromMixedList(List<Object> list, String fallbackPrefix) {
        List<String> out = new ArrayList<>();
        if (list == null) return out;

        for (Object item : list) {
            if (item == null) continue;

            if (item instanceof String) {
                // already a name
                out.add((String) item);
            } else if (item instanceof Number) {
                // only ID provided
                out.add(fallbackPrefix + " #" + ((Number) item).intValue());
            } else if (item instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) item;
                Object nameEn = map.get("name_en");
                Object name = map.get("name");
                if (nameEn instanceof String) {
                    out.add((String) nameEn);
                } else if (name instanceof String) {
                    out.add((String) name);
                } else {
                    // Unknown object structure, try id fallback
                    Object idObj = map.get("id");
                    if (idObj instanceof Number) {
                        out.add(fallbackPrefix + " #" + ((Number) idObj).intValue());
                    }
                }
            } else {
                // Unrecognized type; skip
            }
        }
        return out;
    }

    @Override
    public String toString() {
        return "ExerciseInfo{" +
                "id=" + id +
                ", uuid='" + getUuid() + '\'' +
                ", name='" + name + '\'' +
                ", categoryId=" + getCategoryId() +
                ", categoryName='" + getCategoryName() + '\'' +
                '}';
    }
}
