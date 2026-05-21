package com.example.vaulted;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SteamApiService {

    @GET("ISteamApps/GetAppList/v2/")
    Call<SteamAppListResponse> getAppList();

    @GET("IPlayerService/GetOwnedGames/v1/")
    Call<SteamOwnedGamesResponse> getOwnedGames(
            @Query("key") String apiKey,
            @Query("steamid") String steamId,
            @Query("include_appinfo") boolean includeAppInfo,
            @Query("include_played_free_games") boolean includeFreeGames,
            @Query("format") String format
    );

    @GET("ISteamUser/GetPlayerSummaries/v2/")
    Call<SteamProfileResponse> getPlayerSummaries(
            @Query("key") String apiKey,
            @Query("steamids") String steamIds
    );

    @GET("ISteamUserStats/GetPlayerAchievements/v1/")
    Call<SteamAchievementsResponse> getPlayerAchievements(
            @Query("key") String apiKey,
            @Query("steamid") String steamId,
            @Query("appid") String appId,
            @Query("l") String language
    );

    @GET("ISteamUserStats/GetSchemaForGame/v2/")
    Call<SteamSchemaResponse> getSchemaForGame(
            @Query("key") String apiKey,
            @Query("appid") String appId,
            @Query("l") String language
    );
}
