package com.example.vaulted;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "achievements")
public class AchievementEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String  gameExternalId;
    public String  name;
    public String  description;
    public String  platform;
    public boolean unlocked;
    public String  iconUrl;

    public AchievementEntity() {}

    public AchievementEntity(String gameExternalId, String name, String description,
                             String platform, boolean unlocked, String iconUrl) {
        this.gameExternalId = gameExternalId;
        this.name           = name;
        this.description    = description;
        this.platform       = platform;
        this.unlocked       = unlocked;
        this.iconUrl        = iconUrl;
    }
}
