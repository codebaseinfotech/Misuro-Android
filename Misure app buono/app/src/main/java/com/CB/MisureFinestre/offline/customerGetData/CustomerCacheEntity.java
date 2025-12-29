package com.CB.MisureFinestre.offline.customerGetData;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "customer_cache")
public class CustomerCacheEntity {

    @PrimaryKey
    public int id;
    public int user_id;
    public String customer;
    public String location;
    public String date;
    public String pdf_url;

    public boolean isOffline; // false for API data
}