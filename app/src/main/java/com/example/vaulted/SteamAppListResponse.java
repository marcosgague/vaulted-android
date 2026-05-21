package com.example.vaulted;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SteamAppListResponse {

    @SerializedName("applist")
    public AppList appList;

    public static class AppList {
        @SerializedName("apps")
        public List<SteamApp> apps;
    }

    public static class SteamApp {
        @SerializedName("appid")
        public int appId;

        @SerializedName("name")
        public String name;
    }
}
