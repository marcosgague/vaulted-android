package com.example.vaulted;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class GameDetailViewModel extends AndroidViewModel {

    private final GameRepository repo;
    private LiveData<java.util.List<AchievementEntity>> achievementsLiveData;
    private final MutableLiveData<Boolean> syncInProgress = new MutableLiveData<>(false);
    private boolean hasSynced = false;

    public GameDetailViewModel(@NonNull Application application) {
        super(application);
        repo = new GameRepository(application);
    }

    public LiveData<java.util.List<AchievementEntity>> getAchievements(String externalId) {
        if (achievementsLiveData == null) {
            achievementsLiveData = repo.getAchievementsByGame(externalId);
        }
        return achievementsLiveData;
    }

    public LiveData<Boolean> getSyncInProgress() {
        return syncInProgress;
    }

    public void syncIfNeeded(String steamId, String apiKey, String externalId) {
        if (hasSynced) return;
        hasSynced = true;
        syncInProgress.setValue(true);
        repo.syncAchievements(steamId, apiKey, externalId, success -> {
            syncInProgress.postValue(false);
        });
    }
}