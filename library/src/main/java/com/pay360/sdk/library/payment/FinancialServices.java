/*
 * Copyright (c) 2015. PayPoint
 */

package com.pay360.sdk.library.payment;

import com.google.gson.annotations.SerializedName;

/**
 * Optional payment financial services
 */
public class FinancialServices {

    @SerializedName("dateOfBirth")
    private String dateOfBirth;

    @SerializedName("surname")
    private String surname;

    @SerializedName("accountNumber")
    private String accountNumber;

    @SerializedName("postCode")
    private String postCode;

    /**
     * Sets the date of birth
     * @param dateOfBirth date of birth
     * @return FinancialServices for chaining
     */
    public FinancialServices setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    /**
     * Sets the account number
     * @param surname surname
     * @return FinancialServices for chaining
     */
    public FinancialServices setSurname(String surname) {
        this.surname = surname;
        return this;
    }

    /**
     * Sets the account number
     * @param accountNumber account number
     * @return FinancialServices for chaining
     */
    public FinancialServices setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
        return this;
    }

    /**
     * Sets the post code
     * @param postCode post code
     * @return FinancialServices for chaining
     */
    public FinancialServices setPostCode(String postCode) {
        this.postCode = postCode;
        return this;
    }
}
