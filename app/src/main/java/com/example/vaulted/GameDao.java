package com.example.vaulted;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<GameEntity> games);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(GameEntity game);

    @Query("SELECT * FROM games WHERE userId = :uid ORDER BY lastPlayed DESC")
    LiveData<List<GameEntity>> getGamesByUser(String uid);

    @Query("SELECT * FROM games WHERE userId = :uid ORDER BY hoursPlayed DESC LIMIT :limit")
    LiveData<List<GameEntity>> getTopGamesByUser(String uid, int limit);

    @Query("SELECT * FROM games WHERE userId = :uid AND title LIKE '%' || :query || '%' ORDER BY hoursPlayed DESC LIMIT :limit")
    LiveData<List<GameEntity>> searchGamesByUser(String uid, String query, int limit);

    @Query("SELECT * FROM games WHERE userId = :uid AND platform = :platform ORDER BY lastPlayed DESC")
    LiveData<List<GameEntity>> getGamesByPlatform(String uid, String platform);

    @Query("SELECT COALESCE(SUM(hoursPlayed), 0) FROM games WHERE userId = :uid")
    LiveData<Integer> getTotalHours(String uid);

    @Query("SELECT COUNT(*) FROM games WHERE userId = :uid")
    LiveData<Integer> getTotalGames(String uid);

    @Query("DELETE FROM games WHERE userId = :uid")
    void deleteAllByUser(String uid);
}
