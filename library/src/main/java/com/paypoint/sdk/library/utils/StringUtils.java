package com.paypoint.sdk.library.utils;

/**
 * Who:  Pete
 * When: 15/04/2015
 * What:
 */
public class StringUtils {

    public static String deleteWhitespace(String str) {
        if (str == null) {
            return null;
        }

        return str.replace(" ", "");
    }
}
