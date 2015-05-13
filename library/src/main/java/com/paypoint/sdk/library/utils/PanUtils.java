/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.utils;

import android.text.TextUtils;

/**
 * PAN utility functions
 */
public class PanUtils {

    private static final int PAN_LENGTH_MIN = 13;
    private static final int PAN_LENGTH_MAX = 19;

    public static boolean checkLuhn(String pan) {
        int sum = 0;
        int length = pan.length();
        for (int i = 0; i < length; i++) {


            // get digits in reverse order
            int digit = Integer.parseInt(pan.substring(length - i - 1, length -i));

            // every 2nd number multiply with 2
            if (i % 2 == 1) {
                digit *= 2;
            }
            sum += digit > 9 ? digit - 9 : digit;
        }
        return sum % 10 == 0;
    }

    public static boolean isValidCardNumber(String pan) {
        if (TextUtils.isEmpty(pan)) {
            return false;
        }

        if (pan.length() < PAN_LENGTH_MIN ||
            pan.length() > PAN_LENGTH_MAX) {
           return false;
        }

        // check all numeric
        if (!TextUtils.isDigitsOnly(pan)) {
            return false;
        }

        return true;
    }
}
