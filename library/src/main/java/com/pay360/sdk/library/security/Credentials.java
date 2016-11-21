/*
 * Copyright (c) 2016 Capita plc
 */

package com.pay360.sdk.library.security;

import android.text.TextUtils;

import com.pay360.sdk.library.exception.InvalidCredentialsException;

/**
 * Credentials required for Pay360 authentication - retrieve these will a call to YOUR server
 * then pass into {@link com.pay360.sdk.library.payment.PaymentManager#setCredentials(Credentials)} )}
 */
public class Credentials {

    private String installationId;

    private String token;

    public Credentials setInstallationId(String installationId) {
        this.installationId = installationId;
        return this;
    }

    public Credentials setToken(String token) {
        this.token = token;
        return this;
    }

    public String getInstallationId() {
        return installationId;
    }

    public String getToken() {
        return token;
    }

    public void validateData() throws InvalidCredentialsException {

        if (TextUtils.isEmpty(installationId)) {
            throw new InvalidCredentialsException();
        }

        if (TextUtils.isEmpty(token)) {
            throw new InvalidCredentialsException();
        }
    }
}
