/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment.request;

import com.google.gson.annotations.SerializedName;

/**
 * Optional payment customer details
 */
public class CustomerDetails {

    @SerializedName("email")
    private String email;

    @SerializedName("dob")
    private String dateOfBirth;

    @SerializedName("telephone")
    private String telephone;

    public CustomerDetails setEmail(String email) {
        this.email = email;
        return this;
    }

    public CustomerDetails setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    public CustomerDetails setTelephone(String telephone) {
        this.telephone = telephone;
        return this;
    }
}
