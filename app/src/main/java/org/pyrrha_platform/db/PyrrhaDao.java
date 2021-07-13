package org.pyrrha_platform.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PyrrhaDao {
    @Query("SELECT * FROM PyrrhaTable LIMIT 20")
    List<PyrrhaTable> getAll();

    @Insert
    void insertAll(PyrrhaTable... pyrrhaTable);

    @Update
    void updateUsers(PyrrhaTable... pyrrhaTable);

    @Delete
    void delete(PyrrhaTable pyrrhaTable);
}