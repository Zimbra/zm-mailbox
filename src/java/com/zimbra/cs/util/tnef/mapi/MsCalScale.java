/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.util.tnef.mapi;

public enum MsCalScale {

    // Gregorian (but if PatternType is one of HJ_*, use Hijri)
    DEFAULT_CALSCALE       (0x0000, "", true),
    // Gregorian (localized)
    GREGORIAN              (0x0001, "Gregorian", true),
    GREGORIAN_US           (0x0002, "Gregorian_us", true),
    // Japanese Emperor Era
    JAPAN                  (0x0003, "Japan", true),
    TAIWAN                 (0x0004, "Taiwan", true),
    // Korean Tangun Era
    KOREA                  (0x0005, "Korea", true),
    // Arabic Lunar
    HIJRI                  (0x0006, "Hijri", false),
    THAI                   (0x0007, "Thai", true),
    // Lunar
    HEBREW                 (0x0008, "Hebrew", false),
    // Middle East French
    GREGORIAN_ME_FRENCH    (0x0009, "GregorianMeFrench", true),
    GREGORIAN_ARABIC       (0x000A, "GregorianArabic", true),
    // Gregorian transliterated English
    GREGORIAN_XLIT_ENGLISH (0x000B, "GregorianXlitEnglish", true),
    // Gregorian transliterated French
    GREGORIAN_XLIT_FRENCH  (0x000C, "GregorianXlitFrench", true),
    LUNAR_JAPANESE         (0x000E, "JapanLunar", false),
    LUNAR_CHINESE          (0x000F, "ChineseLunar", false),
    SAKA                   (0x0010, "Saka", false),
    LUNAR_ETO_CHN          (0x0011, "LunarEtoChn", false),
    LUNAR_ETO_KOR          (0x0012, "LunarEtoKor", false),
    LUNAR_ROKUYOU          (0x0013, "LunarRokuyou", false),
    LUNAR_KOREAN           (0x0014, "KoreaLunar", false),
    UMALQURA               (0x0017, "Umalqura", false);
    
    private final int MapiPropValue;
    private final String IcalValue;
    private final boolean IsSolarCalendar;

    MsCalScale(int propValue, String icalValue, boolean isSolarCalendar) {
        this.MapiPropValue = propValue;
        this.IcalValue = icalValue;
        this.IsSolarCalendar = isSolarCalendar;
    }

    public int mapiPropValue() {
        return MapiPropValue;
    }

    /**
     *
     * This method can be used to determine whether, for monthly and yearly
     * recurrences, the standard ICALENDAR RRULE mechanism is sufficient.
     *
     * @return true if this Calendar Scale is Solar based like Gregorian
     */

    public boolean isSolarCalendar() {
        return IsSolarCalendar;
    }
    
    /**
     * 
     * @return Suitable value for X-MICROSOFT-CALSCALE
     */
    public String XMicrosoftCalScale() {
       return IcalValue;
    }
}
