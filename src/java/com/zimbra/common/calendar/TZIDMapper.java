/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.calendar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.localconfig.LC;

/**
 * Mapping of time zone IDs among Olson database, Java and Windows.  Also
 * maps deprecated names to new names.
 */
public class TZIDMapper {

    public static class TZ {
    	private String mCanonicalID;
        private String mJavaID;
        private String mOlsonID;
        private String mWindowsID;
        private String[] mAliases;

        // If olsonId is null, javaId value is used as olsonId.
        public TZ(String javaId, String olsonId, String windowsId, String... aliases) {
            mJavaID = javaId;
            mOlsonID = olsonId != null ? olsonId : javaId;
            mWindowsID = windowsId;
            if (aliases.length > 0)
                mAliases = aliases;
            
            if (LC.calendar_canonical_tzid.value().equals("windows"))
            	mCanonicalID = mWindowsID;
            else if (LC.calendar_canonical_tzid.value().equals("olson"))
            	mCanonicalID = mOlsonID;
            else if (LC.calendar_canonical_tzid.value().equals("java"))
            	mCanonicalID = mJavaID;
            else
            	mCanonicalID = mWindowsID;
        }

        public String getCanonicalID() { return mCanonicalID; }
        public String getJavaID()      { return mJavaID; }
        public String getOlsonID()     { return mOlsonID; }
        public String getWindowsID()   { return mWindowsID; }
        public String[] getAliases()   { return mAliases; }
    }

    private static Map<String, TZ> sMap = new HashMap<String, TZ>();
    private static List<TZ> sList = new ArrayList<TZ>();

    private static void map(String javaID, String olsonID, String windowsID,
                            String... aliases) {
        if (javaID != null) {
            TZ tz = new TZ(javaID, olsonID, windowsID, aliases);
            sList.add(tz);
            sMap.put(javaID, tz);

            if (olsonID != null)
                sMap.put(olsonID, tz);
            if (windowsID != null)
                sMap.put(windowsID, tz);
            for (String alias : aliases)
                sMap.put(alias, tz);
        }
    }

    public static Iterator<TZ> iterator() {
        return sList.iterator();
    }

    public static String[] getJavaIDs() {
        String[] ids = new String[sList.size()];
        int i = 0;
        for (TZ tz : sList) {
            ids[i++] = tz.getJavaID();
        }
        return ids;
    }

    public static String[] getOlsonIDs() {
        String[] ids = new String[sList.size()];
        int i = 0;
        for (TZ tz : sList) {
            ids[i++] = tz.getOlsonID();
        }
        return ids;
    }

    public static String[] getWindowsIDs() {
        String[] ids = new String[sList.size()];
        int i = 0;
        for (TZ tz : sList) {
            ids[i++] = tz.getWindowsID();
        }
        return ids;
    }

    public static String toJava(String tzid) {
        TZ tz = sMap.get(tzid);
        if (tz == null)
            return tzid;
        return tz.getJavaID();
    }

    public static String toOlson(String tzid) {
        TZ tz = sMap.get(tzid);
        if (tz == null)
            return tzid;
        return tz.getOlsonID();
    }

    public static String toWindows(String tzid) {
        TZ tz = sMap.get(tzid);
        if (tz == null)
            return tzid;
        return tz.getWindowsID();
    }

    public static String canonicalize(String tzid) {
        TZ tz = sMap.get(tzid);
        if (tz != null)
            return tz.getCanonicalID();
        else
            return tzid;
    }

    // the time zone list
    static {
        map("Etc/GMT+12", null, "(GMT-12.00) International Date Line West");
        map("Pacific/Midway", null, "(GMT-11.00) Midway Island / Samoa");
        map("US/Hawaii", "Pacific/Honolulu", "(GMT-10.00) Hawaii");
        map("US/Alaska", "America/Anchorage", "(GMT-09.00) Alaska");
        map("US/Pacific", "America/Los_Angeles", "(GMT-08.00) Pacific Time (US & Canada)",
            "(GMT-08.00) Pacific Time (US & Canada) / Tijuana");
        map("America/Tijuana", "America/Tijuana", "(GMT-08.00) Tijuana / Baja California");
        map("US/Arizona", "America/Phoenix", "(GMT-07.00) Arizona");
        map("America/Chihuahua", "America/Chihuahua", "(GMT-07.00) Chihuahua / La Paz / Mazatlan - New",
            "(GMT-07.00) Chihuahua / La Paz / Mazatlan - Old",
            "(GMT-07.00) Chihuahua / La Paz / Mazatlan");
        map("US/Mountain", "America/Denver", "(GMT-07.00) Mountain Time (US & Canada)");
        map("America/Guatemala", null, "(GMT-06.00) Central America");
        map("US/Central", "America/Chicago", "(GMT-06.00) Central Time (US & Canada)");
        map("America/Mexico_City", "America/Mexico_City", "(GMT-06.00) Guadalajara / Mexico City / Monterrey - New",
            "(GMT-06.00) Guadalajara / Mexico City / Monterrey - Old",
            "(GMT-06.00) Guadalajara / Mexico City / Monterrey");
        map("Canada/Saskatchewan", "America/Guatemala", "(GMT-06.00) Saskatchewan");
        map("America/Bogota", null, "(GMT-05.00) Bogota / Lima / Quito / Rio Branco",
            "(GMT-05.00) Bogota / Lima / Quito");
        map("US/Eastern", "America/New_York", "(GMT-05.00) Eastern Time (US & Canada)");
        map("US/East-Indiana", "America/Jamaica", "(GMT-05.00) Indiana (East)");
        map("Canada/Eastern", "America/Glace_Bay", "(GMT-04.00) Atlantic Time (Canada)");
        map("America/Caracas", null, "(GMT-04.00) Caracas / La Paz");
        map("America/Manaus", "America/Manaus", "(GMT-04.00) Manaus");
        map("America/Santiago", null, "(GMT-04.00) Santiago");
        map("Canada/Newfoundland", "America/St_Johns", "(GMT-03.30) Newfoundland");
        map("Brazil/East", "America/Sao_Paulo", "(GMT-03.00) Brasilia");
        map("America/Buenos_Aires", "America/Argentina/Buenos_Aires", "(GMT-03.00) Buenos Aires / Georgetown");
        map("America/Godthab", null, "(GMT-03.00) Greenland");
        map("America/Montevideo", null, "(GMT-03.00) Montevideo");
        map("Atlantic/South_Georgia", null, "(GMT-02.00) Mid-Atlantic");
        map("Atlantic/Azores", null, "(GMT-01.00) Azores");
        map("Atlantic/Cape_Verde", null, "(GMT-01.00) Cape Verde Is.");
        map("Africa/Casablanca", null, "(GMT) Casablanca / Monrovia / Reykjavik",
            "(GMT) Casablanca / Monrovia");
        map("Europe/London", null, "(GMT) Greenwich Mean Time - Dublin / Edinburgh / Lisbon / London");
        map("Europe/Berlin", null, "(GMT+01.00) Amsterdam / Berlin / Bern / Rome / Stockholm / Vienna");
        map("Europe/Belgrade", null, "(GMT+01.00) Belgrade / Bratislava / Budapest / Ljubljana / Prague");
        map("Europe/Brussels", null, "(GMT+01.00) Brussels / Copenhagen / Madrid / Paris");
        map("Europe/Warsaw", null, "(GMT+01.00) Sarajevo / Skopje / Warsaw / Zagreb");
        map("Africa/Algiers", null, "(GMT+01.00) West Central Africa");
        map("Asia/Amman", null, "(GMT+02.00) Amman");
        map("Europe/Athens", null, "(GMT+02.00) Athens / Bucharest / Istanbul",
            "(GMT+02.00) Athens / Beirut / Istanbul / Minsk",
            "(GMT+02.00) Bucharest");
        map("Asia/Beirut", null, "(GMT+02.00) Beirut");
        map("Africa/Cairo", null, "(GMT+02.00) Cairo");
        map("Africa/Harare", null, "(GMT+02.00) Harare / Pretoria");
        map("Europe/Helsinki", null, "(GMT+02.00) Helsinki / Kyiv / Riga / Sofia / Tallinn / Vilnius");
        map("Asia/Jerusalem", null, "(GMT+02.00) Jerusalem");
        map("Europe/Minsk", null, "(GMT+02.00) Minsk");
        map("Africa/Windhoek", null, "(GMT+02.00) Windhoek");
        map("Asia/Baghdad", null, "(GMT+03.00) Baghdad");
        map("Asia/Kuwait", null, "(GMT+03.00) Kuwait / Riyadh");
        map("Europe/Moscow", null, "(GMT+03.00) Moscow / St. Petersburg / Volgograd");
        map("Africa/Nairobi", null, "(GMT+03.00) Nairobi");
        map("Asia/Tbilisi", null, "(GMT+03.00) Tbilisi");
        map("Asia/Tehran", null, "(GMT+03.30) Tehran");
        map("Asia/Muscat", null, "(GMT+04.00) Abu Dhabi / Muscat");
        map("Asia/Baku", null, "(GMT+04.00) Baku",
            "(GMT+04.00) Baku / Tbilisi / Yerevan");
        map("Asia/Yerevan", null, "(GMT+04.00) Yerevan");
        map("Asia/Kabul", null, "(GMT+04.30) Kabul");
        map("Asia/Yekaterinburg", null, "(GMT+05.00) Ekaterinburg");
        map("Asia/Karachi", null, "(GMT+05.00) Islamabad / Karachi / Tashkent");
        map("Asia/Calcutta", null, "(GMT+05.30) Chennai / Kolkata / Mumbai / New Delhi");
        map("Asia/Colombo", null, "(GMT+05.30) Sri Jayawardenepura",
            "(GMT+06.00) Sri Jayawardenepura");
        map("Asia/Katmandu", null, "(GMT+05.45) Kathmandu");
        map("Asia/Novosibirsk", null, "(GMT+06.00) Almaty / Novosibirsk");
        map("Asia/Dhaka", null, "(GMT+06.00) Astana / Dhaka");
        map("Asia/Rangoon", null, "(GMT+06.30) Yangon (Rangoon)",
            "(GMT+06.30) Rangoon");
        map("Asia/Bangkok", null, "(GMT+07.00) Bangkok / Hanoi / Jakarta");
        map("Asia/Krasnoyarsk", null, "(GMT+07.00) Krasnoyarsk");
        map("Asia/Hong_Kong", null, "(GMT+08.00) Beijing / Chongqing / Hong Kong / Urumqi");
        map("Asia/Irkutsk", null, "(GMT+08.00) Irkutsk / Ulaan Bataar");
        map("Asia/Kuala_Lumpur", null, "(GMT+08.00) Kuala Lumpur / Singapore");
        map("Australia/Perth", null, "(GMT+08.00) Perth");
        map("Asia/Taipei", null, "(GMT+08.00) Taipei");
        map("Asia/Tokyo", null, "(GMT+09.00) Osaka / Sapporo / Tokyo");
        map("Asia/Seoul", null, "(GMT+09.00) Seoul");
        map("Asia/Yakutsk", null, "(GMT+09.00) Yakutsk");
        map("Australia/Adelaide", null, "(GMT+09.30) Adelaide");
        map("Australia/Darwin", null, "(GMT+09.30) Darwin");
        map("Australia/Brisbane", null, "(GMT+10.00) Brisbane");
        map("Australia/Sydney", null, "(GMT+10.00) Canberra / Melbourne / Sydney");
        map("Pacific/Guam", null, "(GMT+10.00) Guam / Port Moresby");
        map("Australia/Hobart", null, "(GMT+10.00) Hobart");
        map("Asia/Vladivostok", null, "(GMT+10.00) Vladivostok");
        map("Asia/Magadan", null, "(GMT+11.00) Magadan / Solomon Is. / New Caledonia");
        map("Pacific/Auckland", null, "(GMT+12.00) Auckland / Wellington");
        map("Pacific/Fiji", null, "(GMT+12.00) Fiji / Kamchatka / Marshall Is.");
        map("Pacific/Tongatapu", null, "(GMT+13.00) Nuku'alofa");
    }

    public static void main(String[] args) {
        for (Iterator<TZ> tzIter = iterator(); tzIter.hasNext(); ) {
            TZ tz = tzIter.next();
            String jid = tz.getJavaID();
            String oid = tz.getOlsonID();
            String wid = tz.getWindowsID();
            String[] aliases = tz.getAliases();

            System.out.println(wid);
            System.out.println("    Java : " + jid);
            System.out.println("    Olson: " + oid);
            if (aliases != null && aliases.length > 0) {
                for (String a : aliases) {
                    System.out.println("    Alias: " + a);
                }
            }
            System.out.println();
        }
    }
}
