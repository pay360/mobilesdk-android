/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.utils;

import android.text.TextUtils;

/**
 * CV2 utility functions
 */
public class Cv2Utils {

    private static final int CV2_LENGTH_MIN = 3;
    private static final int CV2_LENGTH_MAX = 4;

    public static boolean isValidCv2Number(String cv2) {
        if (TextUtils.isEmpty(cv2)) {
            return false;
        }

        if (cv2.length() < CV2_LENGTH_MIN ||
            cv2.length() > CV2_LENGTH_MAX) {
            return false;
        }

        // check all numeric
        if (!TextUtils.isDigitsOnly(cv2)) {
            return false;
        }

        return true;
    }
}
