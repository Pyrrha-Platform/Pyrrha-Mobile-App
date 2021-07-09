package org.pyrrha_platform.db;

import androidx.room.*;

import java.util.List;

@Dao
public interface PyrrhaDao {
    @Query("SELECT * FROM PyrrhaTable LIMIT 20")
    List<PyrrhaTable> getAll();

    @Insert
    void insertAll(PyrrhaTable... pyrrhaTable);

    @Update
    public void updateUsers(PyrrhaTable... pyrrhaTable);

    @Delete
    void delete(PyrrhaTable pyrrhaTable);
}