package com.CB.MisureFinestre.offline;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.CB.MisureFinestre.offline.customerGetData.CustomerCacheDao;
import com.CB.MisureFinestre.offline.customerGetData.CustomerCacheEntity;

@Database(
        entities = {CustomerCacheEntity.class, OfflineCustomerEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase INSTANCE;

    public abstract CustomerCacheDao customerCacheDao();
    public abstract OfflineCustomerDao offlineDao();

    public static synchronized AppDatabase get(Context c) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(c.getApplicationContext(), AppDatabase.class, "misure_db")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        return INSTANCE;
    }
}
