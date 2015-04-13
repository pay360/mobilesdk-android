package com.paypoint.sdk.library.payment.response;

/**
 * Created by HendryP on 08/04/2015.
 */
public class PaymentCard {

    private String cardUsageType;

    private String cardScheme;

    private String lastFour;

    public String getCardUsageType() {
        return cardUsageType;
    }

    public String getCardScheme() {
        return cardScheme;
    }

    public String getLastFour() {
        return lastFour;
    }
}
