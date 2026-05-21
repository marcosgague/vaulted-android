package com.example.vaulted;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AchievementEntity> achievements);

    @Query("SELECT * FROM achievements WHERE gameExternalId = :gameExternalId")
    LiveData<List<AchievementEntity>> getByGame(String gameExternalId);

    @Query("SELECT * FROM achievements " +
            "WHERE gameExternalId IN (SELECT externalId FROM games WHERE userId = :uid) " +
            "ORDER BY unlocked DESC, name ASC")
    LiveData<List<AchievementEntity>> getByUser(String uid);

    @Query("SELECT COUNT(*) FROM achievements WHERE unlocked = 1 " +
            "AND gameExternalId IN (SELECT externalId FROM games WHERE userId = :uid)")
    LiveData<Integer> getUnlockedCount(String uid);

    @Query("SELECT COUNT(*) FROM achievements " +
            "WHERE gameExternalId IN (SELECT externalId FROM games WHERE userId = :uid)")
    LiveData<Integer> getTotalCount(String uid);

    @Query("DELETE FROM achievements WHERE gameExternalId = :appId")
    void deleteByGame(String appId);

    @Query("DELETE FROM achievements WHERE gameExternalId IN (SELECT externalId FROM games WHERE userId = :uid)")
    void deleteByUserGames(String uid);

    @Query("DELETE FROM achievements")
    void deleteAll();
}
