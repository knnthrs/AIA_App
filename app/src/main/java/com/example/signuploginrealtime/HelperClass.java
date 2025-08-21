package com.example.signuploginrealtime;

public class HelperClass {

    public String fullname, email, phone;

    public HelperClass() {
        // Default constructor required for Firebase
    }

    public HelperClass(String fullname, String email, String phone) {
        this.fullname = fullname;
        this.email = email;
        this.phone = phone;
    }
}
