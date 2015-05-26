/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.security;

import android.text.TextUtils;

import com.paypoint.sdk.library.exception.InvalidCredentialsException;

/**
 * Credentials required for PayPoint authentication
 */
public class PayPointCredentials {

    private String installationId;

    private String token;

    public PayPointCredentials setInstallationId(String installationId) {
        this.installationId = installationId;
        return this;
    }

    public PayPointCredentials setToken(String token) {
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
