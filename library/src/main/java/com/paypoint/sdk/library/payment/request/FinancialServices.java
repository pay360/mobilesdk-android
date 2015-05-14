/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment.request;

import com.google.gson.annotations.SerializedName;

public class FinancialServices {

    @SerializedName("dateOfBirth")
    private String dateOfBirth;

    @SerializedName("surname")
    private String surname;

    @SerializedName("accountNumber")
    private String accountNumber;

    @SerializedName("postCode")
    private String postCode;

    public FinancialServices setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    public FinancialServices setSurname(String surname) {
        this.surname = surname;
        return this;
    }

    public FinancialServices setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
        return this;
    }

    public FinancialServices setPostCode(String postCode) {
        this.postCode = postCode;
        return this;
    }
}
