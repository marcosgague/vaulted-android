package com.example.vaulted;

public class GameRecommendation {
    public final GameEntity game;
    public final String reason;

    public GameRecommendation(GameEntity game, String reason) {
        this.game = game;
        this.reason = reason;
    }
}
