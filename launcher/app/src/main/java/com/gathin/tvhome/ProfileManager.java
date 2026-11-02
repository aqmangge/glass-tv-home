package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages multiple user profiles, active profile selection, and 3 modern avatar types:
 * 1. "lottie" -> Watak Tunggal (Single Character Lottie with custom expressions & colors)
 * 2. "gif"    -> Animated GIF (from Giphy Search or local cache)
 * 3. "custom" -> Uploaded Mobile Image/GIF or TV storage
 */
public final class ProfileManager {
    private static final String PREFS_NAME = "profile_prefs";
    private static final String KEY_LEGACY_NAME = "profile_name";
    private static final String KEY_LEGACY_AVATAR = "avatar_index";
    private static final String KEY_PROFILES_JSON = "profiles_list_json";
    private static final String KEY_ACTIVE_PROFILE_ID = "active_profile_id";

    public static final String AVATAR_TYPE_LOTTIE = "lottie";
    public static final String AVATAR_TYPE_GIF = "gif";
    public static final String AVATAR_TYPE_CUSTOM = "custom";

    public static class UserProfile {
        public String id;
        public String name;
        public boolean isChild;
        public boolean isPrimary;

        // Modern 3-option avatar fields
        public String avatarType; // "lottie", "gif", "custom"
        public String avatarPath; // local file path or remote URL for GIF/custom
        public int lottieCharColor;
        public int lottieBgColor;
        public int lottieFaceIndex;

        // Legacy compatibility
        public int avatarIndex;

        public UserProfile(String id, String name, boolean isChild, boolean isPrimary) {
            this.id = id != null ? id : "user_" + System.currentTimeMillis();
            this.name = name != null && !name.trim().isEmpty() ? name.trim() : "Pengguna";
            this.isChild = isChild;
            this.isPrimary = isPrimary;
            this.avatarType = AVATAR_TYPE_LOTTIE;
            this.avatarPath = "";
            this.lottieCharColor = AvatarThemeManager.DEFAULT_CHAR_COLOR;
            this.lottieBgColor = AvatarThemeManager.DEFAULT_BG_COLOR;
            this.lottieFaceIndex = AvatarThemeManager.DEFAULT_FACE_INDEX;
            this.avatarIndex = 0;
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", id);
                obj.put("name", name);
                obj.put("isChild", isChild);
                obj.put("isPrimary", isPrimary);
                obj.put("avatarType", avatarType != null ? avatarType : AVATAR_TYPE_LOTTIE);
                obj.put("avatarPath", avatarPath != null ? avatarPath : "");
                obj.put("lottieCharColor", lottieCharColor);
                obj.put("lottieBgColor", lottieBgColor);
                obj.put("lottieFaceIndex", lottieFaceIndex);
                obj.put("avatarIndex", avatarIndex);
            } catch (JSONException ignored) {}
            return obj;
        }

        public static UserProfile fromJson(JSONObject obj) {
            if (obj == null) return new UserProfile("default", "Sara", false, true);
            String id = obj.optString("id", "user_" + System.currentTimeMillis());
            String name = obj.optString("name", "Sara");
            boolean isChild = obj.optBoolean("isChild", false);
            boolean isPrimary = obj.optBoolean("isPrimary", false);

            UserProfile p = new UserProfile(id, name, isChild, isPrimary);
            p.avatarType = obj.optString("avatarType", AVATAR_TYPE_LOTTIE);
            p.avatarPath = obj.optString("avatarPath", "");
            p.lottieCharColor = obj.optInt("lottieCharColor", AvatarThemeManager.DEFAULT_CHAR_COLOR);
            p.lottieBgColor = obj.optInt("lottieBgColor", AvatarThemeManager.DEFAULT_BG_COLOR);
            p.lottieFaceIndex = obj.optInt("lottieFaceIndex", AvatarThemeManager.DEFAULT_FACE_INDEX);
            p.avatarIndex = obj.optInt("avatarIndex", 0);
            return p;
        }
    }

    private ProfileManager() {}

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized List<UserProfile> getAllProfiles(Context context) {
        List<UserProfile> list = new ArrayList<>();
        if (context == null) return list;

        String json = getPrefs(context).getString(KEY_PROFILES_JSON, null);
        if (json != null && !json.trim().isEmpty()) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    list.add(UserProfile.fromJson(arr.getJSONObject(i)));
                }
            } catch (JSONException e) {
                list.clear();
            }
        }

        // Default initial setup if empty (styled like Netflix / Nuvio reference)
        if (list.isEmpty()) {
            UserProfile p1 = new UserProfile("user_primary", "Sara", false, true);
            p1.lottieCharColor = 0xFF00A0FF; // Electric Blue
            p1.lottieBgColor = 0xFF141720;
            p1.lottieFaceIndex = 1; // Senyum

            UserProfile p2 = new UserProfile("user_2", "Mathew", false, false);
            p2.lottieCharColor = 0xFFF59E0B; // Sunny Amber
            p2.lottieBgColor = 0xFF1E293B;
            p2.lottieFaceIndex = 6; // Ceria

            UserProfile p3 = new UserProfile("user_3", "Aeena", true, false);
            p3.lottieCharColor = 0xFFFB7185; // Coral Pink
            p3.lottieBgColor = 0xFF0F172A;
            p3.lottieFaceIndex = 2; // Kenyit

            list.add(p1);
            list.add(p2);
            list.add(p3);
            saveProfiles(context, list);
        }

        // Ensure at least one profile is primary
        boolean hasPrimary = false;
        for (UserProfile p : list) {
            if (p.isPrimary) {
                hasPrimary = true;
                break;
            }
        }
        if (!hasPrimary && !list.isEmpty()) {
            list.get(0).isPrimary = true;
            saveProfiles(context, list);
        }

        return list;
    }

    public static synchronized void saveProfiles(Context context, List<UserProfile> list) {
        if (context == null || list == null) return;
        JSONArray arr = new JSONArray();
        for (UserProfile p : list) {
            arr.put(p.toJson());
        }
        getPrefs(context).edit().putString(KEY_PROFILES_JSON, arr.toString()).apply();
    }

    public static synchronized UserProfile getActiveProfile(Context context) {
        List<UserProfile> list = getAllProfiles(context);
        if (context == null || list.isEmpty()) {
            return new UserProfile("default", "Sara", false, true);
        }

        String activeId = getPrefs(context).getString(KEY_ACTIVE_PROFILE_ID, null);
        if (activeId != null) {
            for (UserProfile p : list) {
                if (activeId.equals(p.id)) return p;
            }
        }

        UserProfile first = list.get(0);
        getPrefs(context).edit().putString(KEY_ACTIVE_PROFILE_ID, first.id).apply();
        return first;
    }

    public static synchronized void setActiveProfile(Context context, String profileId) {
        if (context == null || profileId == null) return;
        List<UserProfile> list = getAllProfiles(context);
        for (UserProfile p : list) {
            if (profileId.equals(p.id)) {
                getPrefs(context).edit()
                        .putString(KEY_ACTIVE_PROFILE_ID, p.id)
                        .putString(KEY_LEGACY_NAME, p.name)
                        .apply();
                return;
            }
        }
    }

    public static synchronized UserProfile addProfile(Context context, String name, boolean isChild) {
        if (context == null) return null;
        List<UserProfile> list = getAllProfiles(context);
        String id = "user_" + System.currentTimeMillis();
        UserProfile newProf = new UserProfile(id, name, isChild, false);
        list.add(newProf);
        saveProfiles(context, list);
        return newProf;
    }

    public static synchronized boolean deleteProfile(Context context, String profileId) {
        if (context == null || profileId == null) return false;
        List<UserProfile> list = getAllProfiles(context);
        if (list.size() <= 1) return false; // Cannot delete the last remaining profile

        UserProfile toRemove = null;
        for (UserProfile p : list) {
            if (profileId.equals(p.id)) {
                toRemove = p;
                break;
            }
        }

        if (toRemove == null) return false;
        list.remove(toRemove);
        saveProfiles(context, list);

        String activeId = getPrefs(context).getString(KEY_ACTIVE_PROFILE_ID, "");
        if (profileId.equals(activeId)) {
            setActiveProfile(context, list.get(0).id);
        }
        return true;
    }

    public static synchronized void updateProfile(Context context, UserProfile updated) {
        if (context == null || updated == null || updated.id == null) return;
        List<UserProfile> list = getAllProfiles(context);
        for (int i = 0; i < list.size(); i++) {
            if (updated.id.equals(list.get(i).id)) {
                list.set(i, updated);
                saveProfiles(context, list);

                String activeId = getPrefs(context).getString(KEY_ACTIVE_PROFILE_ID, "");
                if (updated.id.equals(activeId)) {
                    getPrefs(context).edit().putString(KEY_LEGACY_NAME, updated.name).apply();
                }
                return;
            }
        }
    }

    public static String getProfileName(Context context) {
        return getActiveProfile(context).name;
    }

    public static void setProfileName(Context context, String name) {
        if (context == null) return;
        UserProfile active = getActiveProfile(context);
        active.name = name;
        updateProfile(context, active);
    }
}
