package com.ch.base.lang.time;


import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by he.chen on 5/4/16.
 */
public class DateHelper {
    private static final int MAX_SDF_SIZE = 20;
    private static final ThreadLocal<LRUMap> dateFormatThreadLocal = new ThreadLocal<LRUMap>() {
        @Override
        protected LRUMap initialValue() {
            return new LRUMap(MAX_SDF_SIZE);
        }
    };

    public static SimpleDateFormat getDateFormat(String pattern) {
        LRUMap sdfMap = dateFormatThreadLocal.get();
        SimpleDateFormat sdf = (SimpleDateFormat) sdfMap.get(pattern);
        if (sdf == null) {
            sdf = new SimpleDateFormat(pattern);
            sdf.setLenient(false);
            sdfMap.put(pattern, sdf);
        }
        return sdf;
    }

    public static void destory() {
        dateFormatThreadLocal.remove();
    }

    public static Date parse4y2M2d(String source) {
        try {
            return getDateFormat("yyyy-MM-dd").parse(source);
        } catch (Exception e) {
            return null;
        }
    }

    public static Date parseyyyyMMdd(String source) {
        try {
            return getDateFormat("yyyyMMdd").parse(source);
        } catch (Exception e) {
            return null;
        }
    }

    public static Date parseyyyyMMddHHmmss(String source) {
        try {
            return getDateFormat("yyyyMMddHHmmss").parse(source);
        } catch (Exception e) {
            return null;
        }
    }

    public static Date parseyyyyMMddHHmmssSSS(String source) {
        try {
            return getDateFormat("yyyyMMddHHmmssSSS").parse(source);
        } catch (Exception e) {
            return null;
        }
    }

    public static Date parseHHmmssSSS(String source) {
        try {
            return getDateFormat("HHmmssSSS").parse(source);
        } catch (Exception e) {
            return null;
        }
    }


    public static void main(String[] args) {
        Date date = parseyyyyMMdd_HHmmssSSS("20160517", "92500234");
        System.out.println(date);
    }

    /**
     * 解析
     * @param date 日期
     * @param time 时分秒
     * @return date
     */
    public static Date parseyyyyMMdd_HHmmssSSS (String date, String time) {
        if (StringUtils.isEmpty(time)) {
            return parseyyyyMMdd(date);
        }
        StringBuilder sb = new StringBuilder(date);
        if (time.length() == 8) {
            sb.append("0");
        }
        sb.append(time);
        return parseyyyyMMddHHmmssSSS(sb.toString());
    }
    public static Date parse4y2M2d2h2m2s(String source) {
        try {
            return getDateFormat("yyyy-MM-dd HH:mm:ss").parse(source);
        } catch (Exception e) {
            return null;
        }
    }

    public static Date parse4y2M2d2h2m(String source) {
        try {
            return getDateFormat("yyyy-MM-dd HH:mm").parse(source);
        } catch (Exception e) {
            return null;
        }
    }

    public static Date parse4y2M2dT2h2m2s(String source) {
        try {
            return getDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(source);
        } catch (Exception e) {
            return null;
        }
    }
    public static Date parse4y2M2dhhmm(String source) {
        try {
            return getDateFormat("yyyy-MM-dd HHmm").parse(source);
        } catch (Exception e) {
            return null;
        }
    }


    public static String format4y2M2d(Date date){
        return getDateFormat("yyyy-MM-dd").format(date);
    }

    public static String format4y2M2dCN(Date date){
        return getDateFormat("yyyy年MM月dd日").format(date);
    }

    public static String format2M2dCN(Date date){
        return getDateFormat("MM月dd日").format(date);
    }

    public static String formatMdCN(Date date) {
        return getDateFormat("M月d日").format(date);
    }

    public static String formatyyyyMMdd(Date date){
        return getDateFormat("yyyyMMdd").format(date);
    }

    public static String parseyyyyMMddHHmmss(Date date){
        return getDateFormat("yyyyMMddHHmmss").format(date);
    }

    public static String format4y2M2d2h2m2s(Date date){
        return getDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    public static String formatyyyyMMddHHmmss(Date date) {
        return getDateFormat("yyyyMMddHHmmss").format(date);
    }

    public static String format4y2M2d2h2m(Date date){
        return getDateFormat("yyyy-MM-dd HH:mm").format(date);
    }
    public static String format2h2m2s(Date date){
        return getDateFormat("HH:mm:ss").format(date);
    }
    public static String format2h2m(Date date){
        return getDateFormat("HH:mm").format(date);
    }
    public static String formathhmm(Date date){
        return getDateFormat("HHmm").format(date);
    }

    private static Calendar getCalendar(String pattern){
        LRUMap sdfMap = dateFormatThreadLocal.get();
        if(sdfMap.containsKey(pattern)){
            return (Calendar)sdfMap.get(pattern);
        }else{
            Calendar cal = Calendar.getInstance();
            sdfMap.put(pattern, cal);
            return cal;
        }
    }

    public static Date parseLong2Date(long longTime){
        Calendar cal = getCalendar("long2Date");
        cal.clear();
        cal.setTimeInMillis(longTime);
        return cal.getTime();
    }

    public static Date parseHHmm (String source) {
        try {
            return getDateFormat("HH:mm").parse(source);
        } catch (Exception e) {
            return null;
        }
    }

}
