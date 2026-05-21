package com.example.vaulted;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SteamAppSearchRepository {

    public interface SearchCallback {
        void onResults(List<GameEntity> games);
    }

    public interface AppsByIdCallback {
        void onResults(List<GameEntity> games);
    }

    private static List<SteamAppListResponse.SteamApp> cachedApps;
    private final SteamApiService api;

    public SteamAppSearchRepository() {
        api = RetrofitClient.getSteamClient().create(SteamApiService.class);
    }

    public void search(String query, SearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onResults(new ArrayList<>());
            return;
        }

        // la lista global de steam pesa lo suyo, así que si ya está cargada se reaprovecha sin pedirla otra vez
        if (cachedApps != null) {
            callback.onResults(filterApps(query));
            return;
        }

        api.getAppList().enqueue(new Callback<SteamAppListResponse>() {
            @Override
            public void onResponse(@NonNull Call<SteamAppListResponse> call,
                                   @NonNull Response<SteamAppListResponse> response) {
                SteamAppListResponse body = response.body();
                if (!response.isSuccessful()
                        || body == null
                        || body.appList == null
                        || body.appList.apps == null) {
                    callback.onResults(new ArrayList<>());
                    return;
                }

                cachedApps = body.appList.apps;
                callback.onResults(filterApps(query));
            }

            @Override
            public void onFailure(@NonNull Call<SteamAppListResponse> call, @NonNull Throwable t) {
                callback.onResults(new ArrayList<>());
            }
        });
    }

    public void getAppsByIds(List<String> appIds, AppsByIdCallback callback) {
        if (appIds == null || appIds.isEmpty()) {
            callback.onResults(new ArrayList<>());
            return;
        }

        // esta ruta suele entrar cuando solo tenemos ids guardados y hace falta reconstruir nombre e imagen del juego
        if (cachedApps != null) {
            callback.onResults(findAppsByIds(appIds));
            return;
        }

        api.getAppList().enqueue(new Callback<SteamAppListResponse>() {
            @Override
            public void onResponse(@NonNull Call<SteamAppListResponse> call,
                                   @NonNull Response<SteamAppListResponse> response) {
                SteamAppListResponse body = response.body();
                if (!response.isSuccessful()
                        || body == null
                        || body.appList == null
                        || body.appList.apps == null) {
                    callback.onResults(createFallbackApps(appIds));
                    return;
                }

                cachedApps = body.appList.apps;
                callback.onResults(findAppsByIds(appIds));
            }

            @Override
            public void onFailure(@NonNull Call<SteamAppListResponse> call, @NonNull Throwable t) {
                callback.onResults(createFallbackApps(appIds));
            }
        });
    }

    private List<GameEntity> filterApps(String query) {
        String lower = query.trim().toLowerCase(Locale.ROOT);
        List<GameEntity> results = new ArrayList<>();

        for (SteamAppListResponse.SteamApp app : cachedApps) {
            if (app.name == null || app.name.trim().isEmpty()) continue;
            if (!app.name.toLowerCase(Locale.ROOT).contains(lower)) continue;

            String appId = String.valueOf(app.appId);
            results.add(new GameEntity(
                    appId,
                    app.name,
                    "steam",
                    "https://cdn.akamai.steamstatic.com/steam/apps/" + appId + "/header.jpg",
                    0,
                    "",
                    0,
                    0,
                    "https://cdn.akamai.steamstatic.com/steam/apps/" + appId + "/capsule_616x353.jpg"
            ));

            if (results.size() >= 25) break;
        }

        return results;
    }

    private List<GameEntity> findAppsByIds(List<String> appIds) {
        List<GameEntity> results = new ArrayList<>();
        java.util.Set<String> pendingIds = new java.util.HashSet<>(appIds);

        // se recorre la caché una sola vez y se resuelven solo los ids pendientes
        for (SteamAppListResponse.SteamApp app : cachedApps) {
            String appId = String.valueOf(app.appId);
            if (!pendingIds.contains(appId)) continue;

            results.add(createSteamGame(appId, app.name));
            pendingIds.remove(appId);
            if (pendingIds.isEmpty()) break;
        }

        for (String missingId : pendingIds) {
            results.add(createSteamGame(missingId, "Juego recomendado"));
        }
        return results;
    }

    private List<GameEntity> createFallbackApps(List<String> appIds) {
        List<GameEntity> results = new ArrayList<>();
        // si steam falla, al menos devolvemos fichas mínimas para que recomendaciones y búsquedas no se queden rotas
        for (String appId : appIds) {
            results.add(createSteamGame(appId, "Juego recomendado"));
        }
        return results;
    }

    private GameEntity createSteamGame(String appId, String title) {
        String safeTitle = title != null && !title.trim().isEmpty()
                ? title.trim()
                : "Juego recomendado";
        return new GameEntity(
                appId,
                safeTitle,
                "steam",
                "https://cdn.akamai.steamstatic.com/steam/apps/" + appId + "/header.jpg",
                0,
                "",
                0,
                0,
                "https://cdn.akamai.steamstatic.com/steam/apps/" + appId + "/capsule_616x353.jpg"
        );
    }
}
