package com.paypoint.sdk.library.security;

import android.text.TextUtils;

import com.paypoint.sdk.library.exception.CredentialMissingException;

/**
 * Created by HendryP on 09/04/2015.
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

    public void validateData() throws CredentialMissingException {

        if (TextUtils.isEmpty(installationId)) {
            throw new CredentialMissingException();
        }

        if (TextUtils.isEmpty(token)) {
            throw new CredentialMissingException();
        }
    }
}
