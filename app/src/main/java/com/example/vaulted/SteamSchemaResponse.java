package com.example.vaulted;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SteamSchemaResponse {

    @SerializedName("game")
    public Game game;

    public static class Game {
        @SerializedName("availableGameStats")
        public AvailableGameStats availableGameStats;
    }

    public static class AvailableGameStats {
        @SerializedName("achievements")
        public List<SchemaAchievement> achievements;
    }

    public static class SchemaAchievement {
        @SerializedName("name")
        public String apiName;

        @SerializedName("displayName")
        public String displayName;

        @SerializedName("description")
        public String description;

        @SerializedName("icon")
        public String icon;

        @SerializedName("icongray")
        public String iconGray;
    }
}
