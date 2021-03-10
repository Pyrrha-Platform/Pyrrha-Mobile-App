package com.prometeo.db;

import androidx.room.*;

import java.util.List;

@Dao
public interface PrometeoDao {
    @Query("SELECT * FROM PrometeoTable LIMIT 20")
    List<PrometeoTable> getAll();

    @Insert
    void insertAll(PrometeoTable... prometeoTable);

    @Update
    public void updateUsers(PrometeoTable... prometeoTable);

    @Delete
    void delete(PrometeoTable prometeoTable);
}