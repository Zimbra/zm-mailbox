/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.common.calendar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.zimbra.common.calendar.ICalTimeZone.SimpleOnset;
import com.zimbra.common.calendar.TZIDMapper.TZ;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.mime.MimeConstants;

public class WellKnownTimeZones {

    private static Map<String /* TZID (or alias) */, ICalTimeZone> sTZIDMap =
        new HashMap<String, ICalTimeZone>();
    private static Map<ICalTimeZone, ICalTimeZone> sOffsetRuleMatches = new TreeMap<ICalTimeZone, ICalTimeZone>(new SimpleYearlyTZComparator());

    /**
     * Look up a well-known time zone by its TZID.
     * @param tzid
     * @return
     */
    public static ICalTimeZone getTimeZoneById(String tzid) {
        return sTZIDMap.get(tzid);
    }

    /**
     * Find a well-known time zone that matches the given time zone's standard/daylight offsets
     * and transition rules.  Null is returned if no match is found.
     * @param tz
     * @return
     */
    public static ICalTimeZone getBestMatch(ICalTimeZone tz) {
        return sOffsetRuleMatches.get(tz);
    }

    /**
     * Should be called once at server start
     * @param tzFile
     * @throws IOException
     * @throws ServiceException
     */
    public static void loadFromFile(File tzFile) throws IOException, ServiceException {
        FileInputStream fis = null;
        ZVCalendar tzs = null;
        try {
            fis = new FileInputStream(tzFile);
            tzs = ZCalendar.ZCalendarBuilder.build(new FileInputStream(tzFile), MimeConstants.P_CHARSET_UTF8);
        } finally {
            if (fis != null)
                fis.close();
        }
        for (Iterator<ZComponent> compIter = tzs.getComponentIterator();
             compIter.hasNext(); ) {
            ZComponent tzComp = compIter.next();
            if (!ICalTok.VTIMEZONE.equals(tzComp.getTok()))
                continue;
            ICalTimeZone tz = ICalTimeZone.fromVTimeZone(tzComp, true);
            sTZIDMap.put(tz.getID(), tz);
        }
        // Add aliases from TZIDMapper.  Build map of TZID to match score.
        Map<String, Integer> matchScoreMap = new HashMap<String, Integer>();
        for (Iterator<TZIDMapper.TZ> tzIter = TZIDMapper.iterator(false);
             tzIter.hasNext(); ) {
            TZIDMapper.TZ tz = tzIter.next();
            String id = tz.getID();
            matchScoreMap.put(id, tz.getMatchScore());
            ICalTimeZone itz = getTimeZoneById(id);
            if (itz != null) {
                String[] aliases = tz.getAliases();
                if (aliases != null) {
                    for (String alias : aliases) {
                        addAlias(itz, alias);
                    }
                }
            }
        }
        // Build map for rule-based matching.  For each distinct GMT offset/DST rule, the time zone with
        // the highest match score is added to the map.
        for (Iterator<TZIDMapper.TZ> tzIter = TZIDMapper.iterator(false); tzIter.hasNext(); ) {
            TZIDMapper.TZ tz = tzIter.next();
            String id = tz.getID();
            ICalTimeZone itz = getTimeZoneById(id);
            if (itz != null) {
                ICalTimeZone current = sOffsetRuleMatches.get(itz);
                if (current == null) {
                    sOffsetRuleMatches.put(itz, itz);
                } else {
                    String currentId = current.getID();
                    int currentMatchScore = matchScoreMap.containsKey(currentId) ? matchScoreMap.get(currentId) : 0;                    
                    // Higher score wins.  In a tie, the TZID that comes earlier lexicographically wins.
                    if (currentMatchScore < tz.getMatchScore() ||
                        (currentMatchScore == tz.getMatchScore() && currentId.compareTo(id) > 0)) {
                        sOffsetRuleMatches.remove(itz);
                        sOffsetRuleMatches.put(itz, itz);
                    }
                }
            }
        }
    }

    private static void addAlias(ICalTimeZone tz, String tzid) {
        ICalTimeZone existing = getTimeZoneById(tzid);
        if (existing == null) {
            ICalTimeZone newTz = tz.cloneWithNewTZID(tzid);
            sTZIDMap.put(tzid, newTz);
        }
    }

    // Comparator that sorts ICalTimeZone objects based on GMT offsets and DST transition
    // rules.  It allows checking if two time zones with different TZIDs are equivalent.
    private static class SimpleYearlyTZComparator implements Comparator<ICalTimeZone> {

        // Compare two objects for nullness.  Null sorts before non-null.
        private static int nullCompare(Object o1, Object o2) {
            if (o1 != null) {
                if (o2 != null) return 0;
                else return 1;
            } else {
                if (o2 != null) return -1;
                else return 0;  // both null
            }
        }

        public int compare(ICalTimeZone tz1, ICalTimeZone tz2) {
            // Sort null before non-null.
            if (tz1 == null || tz2 == null)
                return nullCompare(tz1, tz2);

            int comp;

            // Compare by standard GMT offset.
            int stdOff1 = tz1.getStandardOffset();
            int stdOff2 = tz2.getStandardOffset();
            comp = stdOff1 - stdOff2;
            if (comp != 0) return comp;

            // Compare by DST savings.
            int dayOff1 = tz1.getDaylightOffset();
            int dayOff2 = tz2.getDaylightOffset();
            comp = dayOff1 - dayOff2;
            if (comp != 0) return comp;

            // If both time zones are non-DST, they are equivalent.
            assert(stdOff1 == stdOff2 && dayOff1 == dayOff2);
            if (stdOff1 == dayOff1) return 0;

            // At this point we know both time zones use daylight savings.

            // Compare by standard time onset.
            SimpleOnset stdOnset1 = tz1.getStandardOnset();
            SimpleOnset stdOnset2 = tz2.getStandardOnset();
            if (stdOnset1 != null || stdOnset2 != null) {
                comp = nullCompare(stdOnset1, stdOnset2);
                if (comp != 0) return comp;
                comp = stdOnset1.compareTo(stdOnset2);
                if (comp != 0) return comp;
            } else {
                // RRULE
                String stdRule1 = tz1.getStandardRule();
                String stdRule2 = tz2.getStandardRule();
                if (stdRule1 != null && stdRule2 != null)
                    comp = stdRule1.compareTo(stdRule2);
                else
                    comp = nullCompare(stdRule1, stdRule2);
                if (comp != 0) return comp;

                // DTSTART
                String stdDt1 = tz1.getStandardDtStart();
                String stdDt2 = tz2.getStandardDtStart();
                if (stdDt1 != null && stdDt2 != null)
                    comp = stdDt1.compareTo(stdDt2);
                else
                    comp = nullCompare(stdDt1, stdDt2);
                if (comp != 0) return comp;
            }

            // Compare by daylight time onset.
            SimpleOnset dayOnset1 = tz1.getDaylightOnset();
            SimpleOnset dayOnset2 = tz2.getDaylightOnset();
            if (dayOnset1 != null || dayOnset2 != null) {
                comp = nullCompare(dayOnset1, dayOnset2);
                if (comp != 0) return comp;
                comp = dayOnset1.compareTo(dayOnset2);
                if (comp != 0) return comp;
            } else {
                // RRULE
                String dayRule1 = tz1.getDaylightRule();
                String dayRule2 = tz2.getDaylightRule();
                if (dayRule1 != null && dayRule2 != null)
                    comp = dayRule1.compareTo(dayRule2);
                else
                    comp = nullCompare(dayRule1, dayRule2);
                if (comp != 0) return comp;

                // DTSTART
                String dayDt1 = tz1.getDaylightDtStart();
                String dayDt2 = tz2.getDaylightDtStart();
                if (dayDt1 != null && dayDt2 != null)
                    comp = dayDt1.compareTo(dayDt2);
                else
                    comp = nullCompare(dayDt1, dayDt2);
                if (comp != 0) return comp;
            }

            return 0;
        }
    }

    public static void main(String[] args) throws Exception {
        String tzFilePath = LC.timezone_file.value();
        File tzFile = new File(tzFilePath);
        WellKnownTimeZones.loadFromFile(tzFile);

        System.out.println("OFFSET/RULE MATCH TIME ZONES");
        System.out.println("----------------------------");
        for (Iterator<ICalTimeZone> piter = sOffsetRuleMatches.keySet().iterator(); piter.hasNext(); ) {
            System.out.println(piter.next().getID());
        }
        System.out.println("(Total = " + sOffsetRuleMatches.size() + ")");
        System.out.println();

        int nTotal = 0, nPrim = 0, nNonPrim = 0;
        for (Iterator<TZ> tziter = TZIDMapper.iterator(false); tziter.hasNext(); ) {
            nTotal++;
            TZ t = tziter.next();
            if (t.isPrimary()) {
                nPrim++;
                ICalTimeZone tz = getTimeZoneById(t.getID());
                ICalTimeZone match = sOffsetRuleMatches.get(tz);
                if (match == null)
                    System.out.println("sOffsetRuleMatches map is missing primary TZ: " + tz.getID());
                else if (!match.getID().equals(tz.getID()))
                    System.out.println("Mismatch for primary TZ: " + tz.getID() + " (map has " + match.getID() + ")");
            } else
                nNonPrim++;
        }
        System.out.println("num primary in TZIDMapper     = " + nPrim);
        System.out.println("num non-primary in TZIDMapper = " + nNonPrim);
        System.out.println("num total in TZIDMapper       = " + nTotal);
        System.out.println();

        ICalTimeZone iPhonePacific = ICalTimeZone.lookup(
                "GMT-08.00/-07.00",
                -28800000, "16010101T020000", "FREQ=YEARLY;INTERVAL=1;BYMONTH=11;BYDAY=1SU;WKST=MO", "PST",
                -25200000, "16010101T020000", "FREQ=YEARLY;INTERVAL=1;BYMONTH=3;BYDAY=2SU;WKST=MO", "PDT");
        System.out.println("iPhone Pacific: " + iPhonePacific.getID());
        ICalTimeZone primaryPacific = sOffsetRuleMatches.get(iPhonePacific);
        System.out.println("Primary Pacific: " + primaryPacific.getID());
    }
}
