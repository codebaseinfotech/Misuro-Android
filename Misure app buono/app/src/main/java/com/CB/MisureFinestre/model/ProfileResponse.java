package com.CB.MisureFinestre.model;

public class ProfileResponse {
    public boolean success;
    public User user;

    public class User {
        public int id;
        public String name;
        public String phone;
        public String email;
        public String company_name;
        public String company_code;
        public String address;
    }


}
