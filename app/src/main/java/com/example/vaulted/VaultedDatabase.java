package com.example.vaulted;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities  = {GameEntity.class, AchievementEntity.class},
        version   = 4,
        exportSchema = false
)
public abstract class VaultedDatabase extends RoomDatabase {

    private static volatile VaultedDatabase INSTANCE;

    public abstract GameDao        gameDao();
    public abstract AchievementDao achievementDao();

    public static VaultedDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (VaultedDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    VaultedDatabase.class,
                                    "vaulted_db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
