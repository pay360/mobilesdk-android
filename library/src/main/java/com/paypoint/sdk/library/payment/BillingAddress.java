/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.payment;

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

    /**
     * Address line 1
     * @param line1 address line 1
     * @return BillingAddress for chaining
     */
    public BillingAddress setLine1(String line1) {
        this.line1 = line1;
        return this;
    }

    /**
     * Address line 2
     * @param line2 address line 2
     * @return BillingAddress for chaining
     */
    public BillingAddress setLine2(String line2) {
        this.line2 = line2;
        return this;
    }

    /**
     * Address line 3
     * @param line3 address line 3
     * @return BillingAddress for chaining
     */
    public BillingAddress setLine3(String line3) {
        this.line3 = line3;
        return this;
    }

    /**
     * Address line 4
     * @param line4 address line 4
     * @return BillingAddress for chaining
     */
    public BillingAddress setLine4(String line4) {
        this.line4 = line4;
        return this;
    }

    /**
     * Address city
     * @param city address city
     * @return BillingAddress for chaining
     */
    public BillingAddress setCity(String city) {
        this.city = city;
        return this;
    }

    /**
     * Address region
     * @param region address region
     * @return BillingAddress for chaining
     */
    public BillingAddress setRegion(String region) {
        this.region = region;
        return this;
    }

    /**
     * Address postcode
     * @param postcode address postcode
     * @return BillingAddress for chaining
     */
    public BillingAddress setPostcode(String postcode) {
        this.postcode = postcode;
        return this;
    }

    /**
     * Address country code
     * @param countryCode address country code
     * @return BillingAddress for chaining
     */
    public BillingAddress setCountryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }
}
