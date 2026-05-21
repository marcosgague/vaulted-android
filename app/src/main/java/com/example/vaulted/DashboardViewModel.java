package com.example.vaulted;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class DashboardViewModel extends AndroidViewModel {

    private final GameRepository repository;
    private final String uid;

    public final LiveData<Integer>          totalHours;
    public final LiveData<Integer>          totalGames;
    public final LiveData<List<GameEntity>> recentGames;

    public DashboardViewModel(Application application) {
        super(application);
        repository = new GameRepository(application);
        uid = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser().getUid()
                : "";

        totalHours  = repository.getTotalHours(uid);
        totalGames  = repository.getTotalGames(uid);
        recentGames = repository.getGamesByUser(uid);
    }

    public void syncSteam(String steamId, String apiKey) {
        repository.syncSteamGames(steamId, apiKey, uid);
    }
}
