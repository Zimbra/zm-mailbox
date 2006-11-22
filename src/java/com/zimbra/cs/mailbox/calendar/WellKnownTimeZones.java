/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.service.ServiceException;

public class WellKnownTimeZones {

    private static Map<String, ICalTimeZone> sMap =
        new HashMap<String, ICalTimeZone>();
    // list to perserve ordering used in the timezone file
    private static List<ICalTimeZone> sList =
        new ArrayList<ICalTimeZone>();
    private static long sLastModifiedTime = 0;

    /**
     * Iterates all well-known time zones.
     * @return
     */
    public static Iterator<ICalTimeZone> getTimeZoneIterator() {
        return sList.iterator();
    }

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
        Reader reader = null;
        ZVCalendar tzs = null;
        try {
            reader = new InputStreamReader(new FileInputStream(tzFile));
            tzs = ZCalendar.ZCalendarBuilder.build(reader);
        } finally {
            if (reader != null)
                reader.close();
        }
        for (Iterator<ZComponent> compIter = tzs.getComponentIterator();
             compIter.hasNext(); ) {
            ZComponent tzComp = compIter.next();
            if (!ICalTok.VTIMEZONE.equals(tzComp.getTok()))
                continue;
            ICalTimeZone tz = ICalTimeZone.fromVTimeZone(tzComp);
            sList.add(tz);
            sMap.put(tz.getID(), tz);
        }
    }
}
