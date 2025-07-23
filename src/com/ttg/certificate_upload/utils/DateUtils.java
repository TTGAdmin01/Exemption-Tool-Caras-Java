/*
 * StringUtils.java
 *
 * Description:  Date utilities, probably cannot be copyrighted.
 *
 * Author: Cara Brauner
 *
 * $Log: DateUtils.java,v $
 * Revision 1.1.1.1  2021/10/20 22:24:42  carab
 * Imported sources
 *
 *
 */
package com.ttg.certificate_upload.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author carab
 */
public class DateUtils {
    private static final String dashFormat = "yyyy-MM-dd";
    
    /**
     * Convenience function to format a date in yyyy-mm-dd format
     * @param date
     * @return
     * @throws Exception 
     */
    public static String getYyyyMmDd(Date date) throws Exception {
      DateFormat dateFormat = new SimpleDateFormat(dashFormat);
      
      if (date == null) {
        return null;
      }
      return dateFormat.format(date);

    }
}
