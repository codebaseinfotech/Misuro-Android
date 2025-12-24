package com.CB.MisureFinestre.offline;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface OfflineCustomerDao {
    @Insert
    void insert(OfflineCustomerEntity entity);

    @Query("SELECT * FROM offline_customer WHERE isSynced = 0")
    List<OfflineCustomerEntity> getPending();

    @Query("UPDATE offline_customer SET isSynced = 1 WHERE id = :id")
    void markSynced(int id);

}
