package com.CB.MisureFinestre.offline;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "offline_customer")
public class OfflineCustomerEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String customerJson;     // full customer + pieces JSON
    public String imagesJson;       // image file paths JSON

    public boolean isSynced;         // false = pending
}
