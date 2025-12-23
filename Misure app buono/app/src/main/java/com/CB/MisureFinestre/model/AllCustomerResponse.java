package com.CB.MisureFinestre.model;

import java.util.List;

public class AllCustomerResponse {
    public boolean success;
    public String status;
    public List<Customer> data;

    public class Customer {
        public int id;
        public int user_id;
        public String customer;
        public String date;
        public String delivery;
        public String location;
        public String glass_window;
        public String color;
        public String cremonese;
        public String persian;
        public String flat;
        public int spacers;
        public String roller_shutter;
        public String dumpster;
        public String mosquito_net;
        public String marble_base;
        public String pdf_url;
    }
}
