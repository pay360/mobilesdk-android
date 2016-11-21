/*
 * Copyright (c) 2016 Capita plc
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
