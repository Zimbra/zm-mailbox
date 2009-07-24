/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
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

package com.zimbra.cs.mailbox.calendar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.calendar.TZIDMapper;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;

public class WellKnownTimeZones {

    private static Map<String, ICalTimeZone> sMap =
        new HashMap<String, ICalTimeZone>();
    private static long sLastModifiedTime = 0;

    /**
     * Look up a well-known time zone by its TZID.
     * @param tzid
     * @return
     */
    public static ICalTimeZone getTimeZoneById(String tzid) {
        return sMap.get(tzid);
    }

    /**
     * Should be called once at server start
     * @param tzFile
     * @throws IOException
     * @throws ServiceException
     */
    public static void loadFromFile(File tzFile) throws IOException, ServiceException {
        sLastModifiedTime = tzFile.lastModified();
        FileInputStream fis = null;
        ZVCalendar tzs = null;
        try {
            fis = new FileInputStream(tzFile);
            tzs = ZCalendar.ZCalendarBuilder.build(new FileInputStream(tzFile), Mime.P_CHARSET_UTF8);
        } finally {
            if (fis != null)
                fis.close();
        }
        for (Iterator<ZComponent> compIter = tzs.getComponentIterator();
             compIter.hasNext(); ) {
            ZComponent tzComp = compIter.next();
            if (!ICalTok.VTIMEZONE.equals(tzComp.getTok()))
                continue;
            ICalTimeZone tz = ICalTimeZone.fromVTimeZone(tzComp);
            sMap.put(tz.getID(), tz);
        }

        for (Iterator<TZIDMapper.TZ> tzIter = TZIDMapper.iterator();
             tzIter.hasNext(); ) {
            TZIDMapper.TZ tz = tzIter.next();
            String wid = tz.getWindowsID();
            if (wid != null) {
                ICalTimeZone itz = getTimeZoneById(wid);
            if (itz != null) {
                    String jid = tz.getJavaID();
                    addAlias(itz, jid);
                    String oid = tz.getOlsonID();
                    addAlias(itz, oid);
                String[] aliases = tz.getAliases();
                if (aliases != null) {
                    for (String alias : aliases) {
                        addAlias(itz, alias);
                    }
                }
            }
        }
        }
    }

    private static void addAlias(ICalTimeZone tz, String tzid) {
        ICalTimeZone existing = getTimeZoneById(tzid);
        if (existing == null) {
            ICalTimeZone newTz = tz.cloneWithNewTZID(tzid);
            sMap.put(tzid, newTz);
        }
    }

    private static class Test {
        public static void doit(File tzsFile) throws Exception {
            loadFromFile(tzsFile);
            List<ICalTimeZone> list = new ArrayList<ICalTimeZone>(sMap.values());
            Collections.sort(list, new TZComparator());
            for (ICalTimeZone tz : list) {
                ZComponent comp = tz.newToVTimeZone();
                System.out.println(comp.toString());
                System.out.println();
            }
        }

        private static class TZComparator implements Comparator<ICalTimeZone> {

            public int compare(ICalTimeZone o1, ICalTimeZone o2) {
                long off1 = o1.getRawOffset();
                long off2 = o2.getRawOffset();
                if (off1 < off2) return -1;
                else if (off1 > off2) return 1;

                String cid1 = getCanonicalId(o1);
                String cid2 = getCanonicalId(o2);
                if (cid1 == null && cid2 == null)
                    return o1.getID().compareTo(o2.getID());
                else if (cid2 == null)
                    return -1;
                else if (cid1 == null)
                    return 1;

                int s1 = getScore(o1);
                int s2 = getScore(o2);
                if (s1 < s2)
                    return -1;
                else if (s1 > s2)
                    return 1;
                else
                    return o1.getID().compareTo(o2.getID());
            }

            private static int getScore(ICalTimeZone tz) {
                String id = tz.getID();
                if (id.equals(TZIDMapper.toWindows(id)))
                    return 0;
                if (id.equals(TZIDMapper.toJava(id)))
                    return 1;
                if (id.equals(TZIDMapper.toOlson(id)))
                    return 2;
                return 3;
            }

            private static String getCanonicalId(ICalTimeZone tz) {
                return TZIDMapper.toWindows(tz.getID());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: WellKnownTimeZones <timezones.ics>");
            System.exit(1);
        }
        File ics = new File(args[0]);
        Test.doit(ics);
    }
}
