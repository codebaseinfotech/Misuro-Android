package com.CB.MisureFinestre.offline;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {OfflineCustomerEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;

    public abstract OfflineCustomerDao offlineDao();

    public static AppDatabase get(Context c) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                    c.getApplicationContext(),
                    AppDatabase.class,
                    "offline_db"
            ).allowMainThreadQueries().build();
        }
        return INSTANCE;
    }
}