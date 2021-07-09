package org.pyrrha_platform.db;

import androidx.room.*;

@Database(entities = {PyrrhaTable.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PyrrhaDao pyrrhaDao();
}