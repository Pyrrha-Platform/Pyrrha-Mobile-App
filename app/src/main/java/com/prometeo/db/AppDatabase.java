package com.prometeo.db;

import androidx.room.*;

@Database(entities = {PrometeoTable.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PrometeoDao prometeoDao();
}