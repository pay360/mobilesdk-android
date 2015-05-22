/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment.request;

import com.google.gson.annotations.SerializedName;

/**
 * Optional payment billing address
 *
 */
public class BillingAddress {

    @SerializedName("line1")
    private String line1;

    @SerializedName("line2")
    private String line2;

    @SerializedName("line3")
    private String line3;

    @SerializedName("line4")
    private String line4;

    @SerializedName("city")
    private String city;

    @SerializedName("region")
    private String region;

    @SerializedName("postcode")
    private String postcode;

    @SerializedName("countryCode")
    private String countryCode;

    public BillingAddress setLine1(String line1) {
        this.line1 = line1;
        return this;
    }

    public BillingAddress setLine2(String line2) {
        this.line2 = line2;
        return this;
    }

    public BillingAddress setLine3(String line3) {
        this.line3 = line3;
        return this;
    }

    public BillingAddress setLine4(String line4) {
        this.line4 = line4;
        return this;
    }

    public BillingAddress setCity(String city) {
        this.city = city;
        return this;
    }

    public BillingAddress setRegion(String region) {
        this.region = region;
        return this;
    }

    public BillingAddress setPostcode(String postcode) {
        this.postcode = postcode;
        return this;
    }

    public BillingAddress setCountryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }
}
