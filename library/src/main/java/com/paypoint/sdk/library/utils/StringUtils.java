/*
 * Copyright (c) 2015. PayPoint
 */

package com.paypoint.sdk.library.utils;

/**
 * Who:  Pete
 * When: 15/04/2015
 * What: String utility functions
 */
public class StringUtils {

    public static String deleteWhitespace(String str) {
        if (str == null) {
            return null;
        }

        return str.replace(" ", "");
    }
}
