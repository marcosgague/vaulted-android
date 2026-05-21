package com.example.vaulted;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SteamOwnedGamesResponse {

    @SerializedName("response")
    private Response response;

    public List<SteamGame> getGames() {
        if (response == null) return null;
        return response.games;
    }

    public static class Response {
        @SerializedName("game_count")
        public int gameCount;

        @SerializedName("games")
        public List<SteamGame> games;
    }

    public static class SteamGame {
        @SerializedName("appid")
        public int appId;

        @SerializedName("name")
        public String name;

        @SerializedName("playtime_forever")
        public int playtimeForever;

        @SerializedName("playtime_2weeks")
        public int playtime2weeks;

        @SerializedName("rtime_last_played")
        public int rtimeLastPlayed;

        @SerializedName("img_icon_url")
        public String imgIconUrl;

        public String getCoverUrl() {
            return "https://cdn.akamai.steamstatic.com/steam/apps/" + appId + "/header.jpg";
        }

        public String getHeaderUrl() {
            return "https://cdn.akamai.steamstatic.com/steam/apps/" + appId + "/capsule_616x353.jpg";
        }

        public int getHoursPlayed() {
            return playtimeForever / 60;
        }

        public int getHoursLast2Weeks() {
            return playtime2weeks / 60;
        }
    }
}
