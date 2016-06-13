/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
import com.zimbra.common.calendar.ICalTimeZone.TZID_NAME_ASSIGNMENT_BEHAVIOR;
import com.zimbra.common.calendar.TZIDMapper.TZ;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;

public class WellKnownTimeZones {

    private static Map<String /* TZID (or alias) */, ICalTimeZone> sTZIDMap = new HashMap<String, ICalTimeZone>();
    private static Map<ICalTimeZone, ICalTimeZone> sOffsetRuleMatches =
            new TreeMap<ICalTimeZone, ICalTimeZone>(new SimpleYearlyTZComparator(false /* fuzzy */));
    /* Bug 95440 Calendar.app on Yosemite behaves very badly with timezones it doesn't recognize.
     * Combine that with slightly buggy timezone rules coming out of ActiveSync and these get seen more than
     * ideally.
     * So, enable a more relaxed match - this should match timezones which only differ in the time on the onset
     * days that rules come into effect.  So, only instances which occur during a narrow window on the onset
     * days should be affected by the rules changes.
     */
    private static Map<ICalTimeZone, ICalTimeZone> fuzzyOffsetRuleMatches =
            new TreeMap<ICalTimeZone, ICalTimeZone>(new SimpleYearlyTZComparator(true /* fuzzy */));

    /**
     * Look up a well-known time zone by its TZID.
     * @param tzid
     * @return Best matching zone or null if no match is found.
     */
    public static ICalTimeZone getTimeZoneById(String tzid) {
        return sTZIDMap.get(tzid);
    }

    /**
     * Find a well-known time zone that matches the given time zone's standard/daylight offsets
     * and transition rules.  Null is returned if no match is found.
     * Made private to encourage use of {@link getBestFuzzyMatch} as call sites which used <b>getBestMatch</b> let
     * through problematic timezone definitions.
     * @return Best matching zone or null if no match is found.
     */
    private static ICalTimeZone getBestMatch(ICalTimeZone tz) {
        return sOffsetRuleMatches.get(tz);
    }

    /**
     * Find a well-known time zone that matches the given time zone's standard/daylight offsets and transition rules.
     * If a known zone only differs by times of onsets on the transition days, we accept that as a match.  This is
     * considered a good idea because this is a frequent source of buggy timezone definitions and some software
     * really doesn't handle unknown timezones at all well, so we should try as hard as possible to coerce to a known
     * zone.
     *
     * @param tz
     * @return Best matching zone or null if no match is found.
     */
    public static ICalTimeZone getBestFuzzyMatch(ICalTimeZone tz) {
        ICalTimeZone match = getBestMatch(tz);
        if (match != null) {
            return match;
        } else {
            return fuzzyOffsetRuleMatches.get(tz);
        }
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
            ICalTimeZone tz = ICalTimeZone.fromVTimeZone(tzComp, true, TZID_NAME_ASSIGNMENT_BEHAVIOR.ALWAYS_KEEP);
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
                current = fuzzyOffsetRuleMatches.get(itz);
                if (current == null) {
                    fuzzyOffsetRuleMatches.put(itz, itz);
                } else {
                    String currentId = current.getID();
                    int currentMatchScore = matchScoreMap.containsKey(currentId) ? matchScoreMap.get(currentId) : 0;
                    // Higher score wins.  In a tie, the TZID that comes earlier lexicographically wins.
                    if (currentMatchScore < tz.getMatchScore() ||
                        (currentMatchScore == tz.getMatchScore() && currentId.compareTo(id) > 0)) {
                        fuzzyOffsetRuleMatches.remove(itz);
                        fuzzyOffsetRuleMatches.put(itz, itz);
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
        final boolean fuzzy;

        SimpleYearlyTZComparator(boolean fuzzyComparisons) {
            fuzzy = fuzzyComparisons;
        }

        /**
         * Compare two objects for nullness.  Null sorts before non-null. <b>Only</b> interested in nullness.
         * i.e. if both non-null - returns 0 regardless of their value!
         */
        protected static int nullCompare(Object o1, Object o2) {
            if (o1 != null) {
                if (o2 != null) return 0;
                else return 1;
            } else {
                if (o2 != null) return -1;
                else return 0;  // both null
            }
        }

        protected static <T> int doCompare(Comparable<T> o1, Comparable<T> o2) {
            int comp = nullCompare(o1, o2);
            return (comp != 0) ? comp : o1.compareTo((T) o2);
        }

        protected static int doFuzzyCompare(SimpleOnset o1, SimpleOnset o2) {
            int comp = nullCompare(o1, o2);
            return (comp != 0) ? comp : o1.fuzzyCompareTo(o2);
        }

        protected int compareObservance(SimpleOnset onset1, SimpleOnset onset2, String rule1, String rule2,
                String dtstart1, String dtstart2) {
            int comp;
            if (onset1 != null || onset2 != null) {
                if (fuzzy) {
                    comp = doFuzzyCompare(onset1, onset2);
                } else {
                    comp = doCompare(onset1, onset2);
                }
            } else {
                // RRULE
                comp = doCompare(rule1, rule2);
                if (comp != 0) {
                    return comp;
                }
                // Not sure whether this makes sense.
                // if (fuzzy) { return comp; }

                // DTSTART
                comp = doCompare(dtstart1, dtstart2);
            }
            return comp;
        }

        @Override
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
            comp = compareObservance(tz1.getStandardOnset(), tz2.getStandardOnset(),
                                        tz1.getStandardRule(), tz2.getStandardRule(),
                                        tz1.getStandardDtStart(), tz2.getStandardDtStart());
            if (comp != 0) {
                return comp;
            }
            // Compare by daylight time onset.
            return compareObservance(tz1.getDaylightOnset(), tz2.getDaylightOnset(),
                                        tz1.getDaylightRule(), tz2.getDaylightRule(),
                                        tz1.getDaylightDtStart(), tz2.getDaylightDtStart());
        }
    }

    public static void main(String[] args) throws Exception {
        String tzFilePath = LC.timezone_file.value();
        File tzFile = new File(tzFilePath);
        WellKnownTimeZones.loadFromFile(tzFile);

        System.out.println("OFFSET/RULE MATCH TIME ZONES");
        System.out.println("----------------------------");
        for (ICalTimeZone key : sOffsetRuleMatches.keySet()) {
            String id = key.getID();
            boolean inFuzzy = false;
            for (ICalTimeZone fuzzykey : fuzzyOffsetRuleMatches.keySet()) {
                if (id.equals(fuzzykey.getID())) {
                    inFuzzy = true;
                    break;
                }
            }
            if (inFuzzy) {
                System.out.println(key.getID());
            } else {
                System.out.println(key.getID() + " [missing from fuzzyOffsetRuleMatches]");
            }
        }
        System.out.println(
                "(Total = " + sOffsetRuleMatches.size() + ") (fuzzyTotal = " + fuzzyOffsetRuleMatches.size() + ")");
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
