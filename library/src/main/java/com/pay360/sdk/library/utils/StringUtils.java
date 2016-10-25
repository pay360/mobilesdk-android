/*
 * Copyright (c) 2015. PayPoint
 */

package com.pay360.sdk.library.utils;

/**
 * String utility functions
 */
public class StringUtils {

    public static String deleteWhitespace(String str) {
        if (str == null) {
            return null;
        }

        return str.replace(" ", "");
    }
}
