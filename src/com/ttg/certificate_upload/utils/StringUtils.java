/*
 * StringUtils.java
 *
 * Description:  String utilities, probably cannot be copyrighted.
 *
 * Author: Cara Brauner
 *
 * $Log: StringUtils.java,v $
 * Revision 1.1.1.1  2021/10/20 22:24:42  carab
 * Imported sources
 *
 *
 */
package com.ttg.certificate_upload.utils;

/**
 *
 * @author carab
 */
public class StringUtils {
     

    /**
     * Determines if the string is empty or not.
     * 
     * @param str
     * @return true if string is null or has length 0.  False otherwise. 
     */
    public static boolean isEmpty(String str) {
         
        if (str == null) {
            return true;
        } else if (str.length() == 0) {
            return true;
        }
        return false;
    }
    
    public static String arrayToCommaDelimitedString(String[] str) {
        StringBuffer results = new StringBuffer();
        
        if (str == null) {
            return null;
        }
        
        for (int i = 0; i < str.length; i++) {
            if (i > 0) results.append(", ");
            results.append(str[i]);
        }
        
        return results.toString();
    }
    
}
