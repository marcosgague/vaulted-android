package com.example.vaulted;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(
        tableName = "games",
        primaryKeys = {"externalId", "userId"}
)
public class GameEntity {

    @NonNull
    public String externalId = "";

    @NonNull
    public String userId = "";

    public String title;
    public String platform;
    public String coverUrl;
    public String headerUrl;
    public int    hoursPlayed;
    public int    lastPlayed;
    public int    hoursLast2Weeks;

    public GameEntity() {}

    public GameEntity(String externalId, String title, String platform,
                      String coverUrl, int hoursPlayed, String userId,
                      int lastPlayed, int hoursLast2Weeks, String headerUrl) {
        this.externalId      = externalId;
        this.title           = title;
        this.platform        = platform;
        this.coverUrl        = coverUrl;
        this.hoursPlayed     = hoursPlayed;
        this.userId          = userId;
        this.lastPlayed      = lastPlayed;
        this.hoursLast2Weeks = hoursLast2Weeks;
        this.headerUrl       = headerUrl;
    }
}
