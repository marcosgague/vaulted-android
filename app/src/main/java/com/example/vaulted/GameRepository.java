package com.example.vaulted;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GameRepository {

    private static final String TAG = "GameRepository";

    private final GameDao         gameDao;
    private final AchievementDao  achievementDao;
    private final VaultedDatabase db;

    public interface SyncCallback {
        void onComplete(boolean success);
    }

    public GameRepository(Context context) {
        db             = VaultedDatabase.getInstance(context);
        gameDao        = db.gameDao();
        achievementDao = db.achievementDao();
    }

    public LiveData<List<GameEntity>> getGamesByUser(String uid) {
        return gameDao.getGamesByUser(uid);
    }

    public LiveData<List<GameEntity>> getTopGamesByUser(String uid, int limit) {
        return gameDao.getTopGamesByUser(uid, limit);
    }

    public LiveData<List<GameEntity>> searchGamesByUser(String uid, String query, int limit) {
        return gameDao.searchGamesByUser(uid, query, limit);
    }

    public LiveData<List<GameEntity>> getGamesByPlatform(String uid, String platform) {
        return gameDao.getGamesByPlatform(uid, platform);
    }

    public LiveData<Integer> getTotalHours(String uid) {
        return gameDao.getTotalHours(uid);
    }

    public LiveData<Integer> getTotalGames(String uid) {
        return gameDao.getTotalGames(uid);
    }

    public LiveData<List<AchievementEntity>> getAchievementsByGame(String externalId) {
        return achievementDao.getByGame(externalId);
    }

    public LiveData<List<AchievementEntity>> getAchievementsByUser(String uid) {
        return achievementDao.getByUser(uid);
    }

    public void syncSteamGames(String steamId, String apiKey, String userId) {
        SteamApiService api = RetrofitClient.getSteamClient()
                .create(SteamApiService.class);

        api.getOwnedGames(apiKey, steamId, true, true, "json")
                .enqueue(new Callback<SteamOwnedGamesResponse>() {
                    @Override
                    public void onResponse(Call<SteamOwnedGamesResponse> call,
                                           Response<SteamOwnedGamesResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<SteamOwnedGamesResponse.SteamGame> steamGames =
                                    response.body().getGames();
                            if (steamGames != null) {
                                List<GameEntity> entities = mapSteamToEntities(steamGames, userId);
                                // toda la inserción se manda a disco en segundo plano para que la sincronización completa no congele la interfaz
                                AppExecutors.getInstance().diskIO()
                                        .execute(() -> gameDao.insertAll(entities));
                            }
                        } else {
                            Log.e(TAG, "Steam error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<SteamOwnedGamesResponse> call, Throwable t) {
                        Log.e(TAG, "Steam API error: " + t.getMessage());
                    }
                });
    }

    public void syncAchievements(String steamId, String apiKey,
                                 String appId, SyncCallback callback) {
        SteamApiService api = RetrofitClient.getSteamClient()
                .create(SteamApiService.class);

        Log.d(TAG, "=== syncAchievements llamado con appId: [" + appId + "]");

        api.getPlayerAchievements(apiKey, steamId, appId, "english")
                .enqueue(new Callback<SteamAchievementsResponse>() {
                    @Override
                    public void onResponse(Call<SteamAchievementsResponse> call,
                                           Response<SteamAchievementsResponse> response) {

                        if (!response.isSuccessful()
                                || response.body() == null
                                || response.body().playerStats == null
                                || response.body().playerStats.achievements == null) {
                            Log.e(TAG, "=== PlayerAchievements vacío, code: " + response.code());
                            callback.onComplete(false);
                            return;
                        }

                        List<SteamAchievementsResponse.SteamAchievement> playerList =
                                response.body().playerStats.achievements;

                        Log.d(TAG, "=== Logros del jugador: " + playerList.size());

                        api.getSchemaForGame(apiKey, appId, "english")
                                .enqueue(new Callback<SteamSchemaResponse>() {
                                    @Override
                                    public void onResponse(Call<SteamSchemaResponse> call,
                                                           Response<SteamSchemaResponse> response) {

                                        java.util.Map<String, String> iconMap     = new java.util.HashMap<>();
                                        java.util.Map<String, String> iconGrayMap = new java.util.HashMap<>();

                                        // aquí se mezclan dos fuentes: una dice qué tiene el jugador y la otra aporta los nombres e iconos buenos
                                        if (response.isSuccessful()
                                                && response.body() != null
                                                && response.body().game != null
                                                && response.body().game.availableGameStats != null
                                                && response.body().game.availableGameStats.achievements != null) {

                                            for (SteamSchemaResponse.SchemaAchievement s :
                                                    response.body().game.availableGameStats.achievements) {
                                                if (s.apiName != null) {
                                                    if (s.icon     != null) iconMap.put(s.apiName, s.icon);
                                                    if (s.iconGray != null) iconGrayMap.put(s.apiName, s.iconGray);
                                                }
                                            }
                                        }

                                        Log.d(TAG, "=== Iconos en schema: " + iconMap.size());

                                        List<AchievementEntity> entities = new ArrayList<>();
                                        for (SteamAchievementsResponse.SteamAchievement a : playerList) {
                                            String iconUrl;
                                            if (a.isUnlocked()) {
                                                iconUrl = iconMap.getOrDefault(a.apiName,
                                                        a.icon != null ? a.icon : "");
                                            } else {
                                                iconUrl = iconGrayMap.getOrDefault(a.apiName,
                                                        a.iconGray != null ? a.iconGray : "");
                                            }

                                            entities.add(new AchievementEntity(
                                                    appId,
                                                    a.name        != null ? a.name        : a.apiName,
                                                    a.description != null ? a.description : "",
                                                    "steam",
                                                    a.isUnlocked(),
                                                    iconUrl
                                            ));
                                        }

                                        Log.d(TAG, "=== Entidades a insertar: " + entities.size());
                                        Log.d(TAG, "=== appId usado en insert: [" + appId + "]");

                                        AppExecutors.getInstance().diskIO().execute(() -> {
                                            try {
                                                // se borra antes de insertar para que una resincronización no deje restos de una versión anterior
                                                achievementDao.deleteByGame(appId);
                                                achievementDao.insertAll(entities);
                                                Log.d(TAG, "=== Insert completado OK");
                                                AppExecutors.getInstance().mainThread()
                                                        .execute(() -> callback.onComplete(true));
                                            } catch (Exception e) {
                                                Log.e(TAG, "=== Error en insert: " + e.getMessage(), e);
                                                AppExecutors.getInstance().mainThread()
                                                        .execute(() -> callback.onComplete(false));
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(Call<SteamSchemaResponse> call, Throwable t) {
                                        Log.e(TAG, "=== Schema error: " + t.getMessage());

                                        List<AchievementEntity> entities = new ArrayList<>();
                                        for (SteamAchievementsResponse.SteamAchievement a : playerList) {
                                            String iconUrl = a.isUnlocked()
                                                    ? (a.icon     != null ? a.icon     : "")
                                                    : (a.iconGray != null ? a.iconGray : "");

                                            entities.add(new AchievementEntity(
                                                    appId,
                                                    a.name        != null ? a.name        : a.apiName,
                                                    a.description != null ? a.description : "",
                                                    "steam",
                                                    a.isUnlocked(),
                                                    iconUrl
                                            ));
                                        }

                                        AppExecutors.getInstance().diskIO().execute(() -> {
                                            try {
                                                achievementDao.deleteByGame(appId);
                                                achievementDao.insertAll(entities);
                                                AppExecutors.getInstance().mainThread()
                                                        .execute(() -> callback.onComplete(true));
                                            } catch (Exception e) {
                                                Log.e(TAG, "=== Error en insert: " + e.getMessage(), e);
                                                AppExecutors.getInstance().mainThread()
                                                        .execute(() -> callback.onComplete(false));
                                            }
                                        });
                                    }
                                });
                    }

                    @Override
                    public void onFailure(Call<SteamAchievementsResponse> call, Throwable t) {
                        Log.e(TAG, "=== Achievements error: " + t.getMessage());
                        callback.onComplete(false);
                    }
                });
    }

    private List<GameEntity> mapSteamToEntities(
            List<SteamOwnedGamesResponse.SteamGame> steamGames, String userId) {

        List<GameEntity> entities = new ArrayList<>();
        for (SteamOwnedGamesResponse.SteamGame sg : steamGames) {
            entities.add(new GameEntity(
                    String.valueOf(sg.appId),
                    sg.name,
                    "steam",
                    sg.getCoverUrl(),
                    sg.getHoursPlayed(),
                    userId,
                    sg.rtimeLastPlayed,
                    sg.getHoursLast2Weeks(),
                    sg.getHeaderUrl()
            ));
        }
        return entities;
    }

    private List<AchievementEntity> mapAchievements(
            List<SteamAchievementsResponse.SteamAchievement> list, String appId) {

        List<AchievementEntity> entities = new ArrayList<>();
        for (SteamAchievementsResponse.SteamAchievement a : list) {
            String iconUrl = a.isUnlocked()
                    ? (a.icon     != null ? a.icon     : "")
                    : (a.iconGray != null ? a.iconGray : "");

            entities.add(new AchievementEntity(
                    appId,
                    a.name        != null ? a.name        : a.apiName,
                    a.description != null ? a.description : "",
                    "steam",
                    a.isUnlocked(),
                    iconUrl
            ));
        }
        return entities;
    }

    public void insertGame(GameEntity game) {
        AppExecutors.getInstance().diskIO()
                .execute(() -> gameDao.insert(game));
    }
}
