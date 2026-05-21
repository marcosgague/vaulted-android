package com.example.vaulted;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SteamAchievementsResponse {

    @SerializedName("playerstats")
    public PlayerStats playerStats;

    public static class PlayerStats {
        @SerializedName("success")
        public boolean success;

        @SerializedName("achievements")
        public List<SteamAchievement> achievements;
    }

    public static class SteamAchievement {
        @SerializedName("apiname")
        public String apiName;

        @SerializedName("achieved")
        public int achieved;

        @SerializedName("unlocktime")
        public long unlockTime;

        @SerializedName("name")
        public String name;

        @SerializedName("description")
        public String description;

        @SerializedName("icon")
        public String icon;

        @SerializedName("icongray")
        public String iconGray;

        public boolean isUnlocked() {
            return achieved == 1;
        }
    }}
