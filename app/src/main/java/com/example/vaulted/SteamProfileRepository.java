package com.example.vaulted;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SteamProfileRepository {

    public interface ProfileCallback {
        void onProfileLoaded(@NonNull SteamProfileResponse.Player player);

        void onError(@Nullable String message);
    }

    private final SteamApiService api;

    public SteamProfileRepository() {
        api = RetrofitClient.getSteamClient().create(SteamApiService.class);
    }

    public void loadProfile(String apiKey, String steamId, ProfileCallback callback) {
        api.getPlayerSummaries(apiKey, steamId).enqueue(new Callback<SteamProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<SteamProfileResponse> call,
                                   @NonNull Response<SteamProfileResponse> response) {
                SteamProfileResponse body = response.body();
                if (!response.isSuccessful() || body == null || body.response == null
                        || body.response.players == null || body.response.players.isEmpty()) {
                    callback.onError("Perfil de Steam no encontrado");
                    return;
                }

                callback.onProfileLoaded(body.response.players.get(0));
            }

            @Override
            public void onFailure(@NonNull Call<SteamProfileResponse> call, @NonNull Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }
}
