package com.CB.MisureFinestre.model;

public class LoginResponse {
    private boolean success;
    private String token;
    private User user;

    public boolean isSuccess() {
        return success;
    }

    public String getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }

    public static class User {
        private int id;
        private String name;
        private String email;
        private String phone;
        private String address;
        private String company_name;
        private String company_code;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getPhone() {
            return phone;
        }

        public String getAddress() {
            return address;
        }

        public String getCompany_name() {
            return company_name;
        }

        public String getCompany_code() {
            return company_code;
        }
    }
}

