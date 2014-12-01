package com.hp.sddg.utils;

import com.hp.sddg.rest.common.entities.EntityHandler;

/**
 * Created by panuska on 13.10.14.
 */
public class TimeUtils {

    public static String getTimeDifference(long earlier, long later) {
        long diff = later - earlier;
        diff = diff / 1000;                                         // in secs
        if (diff == 0) return "fresh list";  //todo this is a general method; should not return this string
        long sec = diff % 60;
        String Sec = "0"+sec;
        if (Sec.length() > 2) Sec = Sec.substring(1);
        diff = diff / 60;                                           // in mins
        if (diff == 0) {
            return Sec+" sec";
        } else {
            long min = diff % 60;
            String Min = "0"+min;
            if (Min.length() > 2) Min = Min.substring(1);
            diff = diff / 60;                                       // in hours
            if (diff == 0) {
                return Min+":"+Sec;
            } else {
                long hrs = diff % 24;
                String Hrs = "0"+hrs;
                if (Hrs.length() > 2) Hrs = Hrs.substring(1);
                diff = diff / 24;                                   // in days
                if (diff == 0) {
                    return Hrs+":"+Min+":"+Sec;
                } else {
                    return diff+" days "+Hrs+":"+Min+":"+Sec;
                }
            }
        }
    }

    public static String getTimeDifference(long earlier) {
        long later = System.currentTimeMillis();
        return getTimeDifference(earlier, later);
    }
}