package com.example.vaulted;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SteamProfileResponse {

    @SerializedName("response")
    public Response response;

    public static class Response {
        @SerializedName("players")
        public List<Player> players;
    }

    public static class Player {
        @SerializedName("steamid")
        public String steamId;

        @SerializedName("personaname")
        public String personaName;

        @SerializedName("avatarfull")
        public String avatarFull;

        @SerializedName("profileurl")
        public String profileUrl;
    }
}
