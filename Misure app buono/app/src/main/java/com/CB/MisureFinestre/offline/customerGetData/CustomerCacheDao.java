package com.CB.MisureFinestre.offline.customerGetData;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CustomerCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CustomerCacheEntity> list);

    @Query("SELECT * FROM customer_cache ORDER BY id DESC")
    List<CustomerCacheEntity> getAll();

    @Query("DELETE FROM customer_cache")
    void clear();
}
