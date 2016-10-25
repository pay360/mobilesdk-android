/*
 * Copyright (c) 2016 Capita plc
 */

package com.pay360.sdk.library.payment;

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

    /**
     * Customer email address
     * @param email email address
     * @return CustomerDetails for chaining
     */
    public CustomerDetails setEmail(String email) {
        this.email = email;
        return this;
    }

    /**
     * Customer date of birth
     * @param dateOfBirth date of birth
     * @return CustomerDetails for chaining
     */
    public CustomerDetails setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    /**
     * Customer telephone number
     * @param telephone telephone number
     * @return CustomerDetails for chaining
     */
    public CustomerDetails setTelephone(String telephone) {
        this.telephone = telephone;
        return this;
    }
}
