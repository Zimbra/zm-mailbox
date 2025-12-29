/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016, 2025 Synacor, Inc.
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

package com.zimbra.cs.mailbox.calendar;

import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.util.ZimbraLog;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class WindowsTimeZoneConversion {

    public static final Map<String, String> STANDARD = new HashMap<>();

    public static final Map<String, String> DAYLIGHT = new HashMap<>();

    // Internal helper maps
    private static final Map<String, String> WIN_STANDARD = new HashMap<>();

    private static final Map<String, String> WIN_DAYLIGHT = new HashMap<>();

    private static final Map<String, String> IANA_TO_WINDOWS = new HashMap<>();

    private static final String ICS_FILE = "/opt/zimbra/conf/timezones.ics";

    private static final String WIN_ALIAS_FILE = "/opt/zimbra/conf/windows-names";

    private static final String WIN_INFO_FILE = "/opt/zimbra/conf/WindowsTimeZoneInfo.txt";

    static {
        try {
            loadWindowsTZInfo();
            loadWindowsAlias();
            buildFinalMaps();
        } catch (Exception e) {
            ZimbraLog.mailbox.warn("ERROR initializing timezone maps: " + e.getMessage());
        }
    }

    private static void loadWindowsTZInfo() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(WIN_INFO_FILE));
        String currentId = null;

        for (String raw : lines) {
            String line = raw.trim();

            if (line.startsWith("ID:")) {
                currentId = line.substring(3).trim();

            } else if (line.startsWith("Standard Name:") && currentId != null) {
                WIN_STANDARD.put(currentId, line.substring("Standard Name:".length()).trim());

            } else if (line.startsWith("Daylight Name:") && currentId != null) {
                String dlt = line.substring("Daylight Name:".length()).trim();
                if (dlt.contains("***")) {
                    dlt = dlt.substring(0, dlt.indexOf("***")).trim();
                }
                WIN_DAYLIGHT.put(currentId, dlt);
            }
        }
    }

    private static void loadWindowsAlias() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(WIN_ALIAS_FILE));
        Pattern p = Pattern.compile("Link\\s+([^\\s]+)\\s+\"([^\"]+)\"");

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            Matcher m = p.matcher(line);
            if (m.find()) {
                String iana = m.group(1);
                String winName = m.group(2);

                if (winName.startsWith("(UTC") || winName.startsWith("(GMT")) {
                    continue;
                }

                if (WIN_STANDARD.containsKey(winName)) {
                    IANA_TO_WINDOWS.putIfAbsent(iana, winName);
                }
            }
        }
    }

    private static void buildFinalMaps() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(ICS_FILE));

        for (String raw : lines) {
            if (!raw.startsWith("TZID:")) {
                continue;
            }

            String tzid = raw.substring(5).trim();
            String winId = IANA_TO_WINDOWS.get(tzid);
            if (winId == null) {
                continue;
            }

            String std = WIN_STANDARD.get(winId);
            String dlt = WIN_DAYLIGHT.get(winId);

            if (std != null) {
                STANDARD.put(tzid, std);
            }
            if (dlt != null) {
                DAYLIGHT.put(tzid, dlt);
            }
        }

        // Make maps immutable for safety
        freeze();
    }

    private static void freeze() {
        Map<String, String> std = Collections.unmodifiableMap(new HashMap<>(STANDARD));
        Map<String, String> dlt = Collections.unmodifiableMap(new HashMap<>(DAYLIGHT));

        STANDARD.clear();
        STANDARD.putAll(std);

        DAYLIGHT.clear();
        DAYLIGHT.putAll(dlt);
    }

    public static String getWindowsStandardName(ICalTimeZone tz) {
        return STANDARD.get(tz.getID()) != null ? STANDARD.get(tz.getID()) : tz.getStandardTzname();
    }

    public static String getWindowsDaylightName(ICalTimeZone tz) {
        return DAYLIGHT.get(tz.getID()) != null ? DAYLIGHT.get(tz.getID()) : tz.getDaylightTzname();
    }
}
