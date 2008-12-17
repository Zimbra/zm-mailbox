/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008 Zimbra, Inc.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

/**
 * Mapping of time zone IDs among Olson database, Java and Windows.  Also
 * maps deprecated names to new names.
 */
public class TZIDMapper {

    public static class TZ implements Comparable<TZ> {
        private String mID;
        private String[] mAliases;
        private boolean mIsPrimary;
        private int mMatchScore;

        public TZ(String tzid, String[] aliases, boolean isPrimary, int matchScore) {
            mID = tzid;
            if (aliases.length > 0)
                mAliases = aliases;
            mIsPrimary = isPrimary;
            mMatchScore = matchScore;
        }

        public String getID()        { return mID; }
        public String[] getAliases() { return mAliases; }
        public boolean isPrimary()   { return mIsPrimary; }
        public int getMatchScore()       { return mMatchScore; }

        public int compareTo(TZ other) {
            if (other == null)
                throw new NullPointerException();
            int comp = mID.compareTo(other.getID());
            if (comp != 0)
                return comp;

            comp = (mIsPrimary ? 1 : 0) - (other.isPrimary() ? 1 : 0);
            if (comp != 0)
                return comp;

            String[] otherAliases = other.getAliases();
            if (mAliases != null) {
                if (otherAliases == null)
                    return 1;
                // both non-null
                int min = Math.min(mAliases.length, otherAliases.length);
                for (int i = 0; i < min; ++i) {
                    String a1 = mAliases[i];
                    String a2 = otherAliases[i];
                    if (a1 != null) {
                        if (a2 == null)
                            return 1;
                        // both non-null
                        comp = a1.compareTo(a2);
                        if (comp != 0)
                            return comp;
                    } else if (a2 != null) {
                        return -1;
                    }  // else both null; check next element
                }
                // All elements matched.  Whichever array that has more elements comes later.
                comp = mAliases.length - otherAliases.length;
                if (comp != 0)
                    return comp;
            } else if (otherAliases != null) {
                return -1;
            } // else both null
            return 0;
        }
    }

    private static Map<String /* TZID, real or alias */, TZ> sMap;
    private static Set<TZ> sAllTZs;
    private static Set<TZ> sPrimaryTZs;

    public static synchronized Iterator<TZ> iterator(boolean primary) {
        if (primary)
            return sPrimaryTZs.iterator();
        else
            return sAllTZs.iterator();
    }

    public static synchronized String canonicalize(String tzid) {
        TZ tz = sMap.get(tzid);
        if (tz != null)
            return tz.getID();
        else
            return tzid;
    }

    static {
        String tzFilePath = LC.timezone_file.value();
        try {
            File tzFile = new File(tzFilePath);
            TZIDMapper.loadFromFile(tzFile);
        } catch (Throwable t) {
            ZimbraLog.calendar.fatal("Unable to load timezones from " + tzFilePath, t);
            Runtime.getRuntime().halt(1);
        }
    }

    public static final String X_ZIMBRA_TZ_ALIAS = "X-ZIMBRA-TZ-ALIAS";
    public static final String X_ZIMBRA_TZ_PRIMARY = "X-ZIMBRA-TZ-PRIMARY";
    public static final String X_ZIMBRA_TZ_MATCH_SCORE = "X-ZIMBRA-TZ-MATCH-SCORE";

    public static final int DEFAULT_MATCH_SCORE_PRIMARY = 100;
    public static final int DEFAULT_MATCH_SCORE_NON_PRIMARY = 0;

    private static final String TZID = "TZID:";                                    // note value ends with ":"
    private static final String TRUE = "TRUE";

    private static synchronized void loadFromFile(File tzFile) throws IOException {
        Map<String, TZ> map = new HashMap<String, TZ>();
        // TZ sets user LinkedHashSet to preserve insertion order.
        Set<TZ> allTZs = new LinkedHashSet<TZ>();
        Set<TZ> primaryTZs = new LinkedHashSet<TZ>();

        FileInputStream fi = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            fi = new FileInputStream(tzFile);
            isr = new InputStreamReader(fi, "UTF-8");
            br = new BufferedReader(isr);
            String line;
            boolean inVTIMEZONE = false;
            boolean isPrimary = false;
            boolean matchScoreSpecified = false;
            int matchScore = 0;
            String tzid = null;
            Set<String> aliases = new TreeSet<String>();
            while ((line = br.readLine()) != null) {
                // Remove leading/trailing whitespaces.
                line = line.replaceAll("^\\s+", "");
                line = line.replaceAll("\\s+$", "");
                String lineUpper = line.toUpperCase();
                if (!inVTIMEZONE) {
                    if (lineUpper.equals("BEGIN:VTIMEZONE")) {
                        inVTIMEZONE = true;
                        tzid = null;
                        isPrimary = false;
                        matchScoreSpecified = false;
                        matchScore = 0;
                        aliases = new TreeSet<String>();
                    }
                } else {  // inVTIMEZONE == true
                    if (lineUpper.equals("END:VTIMEZONE")) {
                        inVTIMEZONE = false;
                        if (tzid != null && tzid.length() > 0 && aliases != null) {
                            if (!matchScoreSpecified) {
                                if (isPrimary)
                                    matchScore = DEFAULT_MATCH_SCORE_PRIMARY;
                                else
                                    matchScore = DEFAULT_MATCH_SCORE_NON_PRIMARY;
                            }
                            TZ tz = new TZ(tzid, aliases.toArray(new String[0]), isPrimary, matchScore);
                            allTZs.add(tz);
                            if (isPrimary)
                                primaryTZs.add(tz);
                            map.put(tzid, tz);
                            if (aliases != null) {
                                for (String alias : aliases) {
                                    map.put(alias, tz);
                                }
                            }
                        }
                    } else if (lineUpper.startsWith(TZID)) {
                        tzid = line.substring(TZID.length());
                    } else if (lineUpper.startsWith(X_ZIMBRA_TZ_ALIAS + ":") && aliases != null) {
                        String alias = line.substring(X_ZIMBRA_TZ_ALIAS.length() + 1);
                        if (alias != null && alias.length() > 0)
                            aliases.add(alias);
                    } else if (lineUpper.startsWith(X_ZIMBRA_TZ_PRIMARY + ":")) {
                        String b = lineUpper.substring(X_ZIMBRA_TZ_PRIMARY.length() + 1);
                        isPrimary = TRUE.equals(b);
                    } else if (lineUpper.startsWith(X_ZIMBRA_TZ_MATCH_SCORE + ":") && aliases != null) {
                        String matchScoreStr = line.substring(X_ZIMBRA_TZ_MATCH_SCORE.length() + 1);
                        try {
                            matchScore = Integer.parseInt(matchScoreStr);
                            matchScoreSpecified = true;
                        } catch (NumberFormatException e) {}
                    }
                }
            }

            sMap = map;
            sAllTZs = allTZs;
            sPrimaryTZs = primaryTZs;
        } finally {
            if (br != null)
                br.close();
            else if (isr != null)
                isr.close();
            else if (fi != null)
                fi.close();
        }
    }

    public static void main(String[] args) {
        for (Iterator<TZ> tzIter = iterator(false); tzIter.hasNext(); ) {
            TZ tz = tzIter.next();
            String id = tz.getID();
            String[] aliases = tz.getAliases();

            System.out.println(id);
            if (aliases != null && aliases.length > 0) {
                for (String a : aliases) {
                    System.out.println("    Alias: " + a);
                }
            }
            System.out.println();
        }
    }
}
