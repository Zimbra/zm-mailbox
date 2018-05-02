/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.db.Versions;

public final class BuildInfo {

    public static final String TYPE_NETWORK = "NETWORK";
    public static final String TYPE_FOSS = "FOSS";

    public static final String TYPE;     /* whether this is a FOSS or NETWORK installation */
    public static final String VERSION;
    public static final String RELEASE;
    public static final String DATE;
    public static final String HOST;
    public static final String PLATFORM;
    public static final String MAJORVERSION;
    public static final String MINORVERSION;
    public static final String MICROVERSION;
    public static final String BUILDNUM;

    public static final String FULL_VERSION;

    static {
        String version = "unknown";
        String type = getType();
        String release = "unknown";
        String date = "unknown";
        String host = "unknown";
        String majorversion = "unknown";
        String minorversion = "unknown";
        String microversion = "unknown";
        String platform = getPlatform();
        String buildnum = "buildnum";
        try {
            Class<?> clz = Class.forName("com.zimbra.cs.util.BuildInfoGenerated");
            version = (String) clz.getField("VERSION").get(null);
            release = (String) clz.getField("RELEASE").get(null);
            date = (String) clz.getField("DATE").get(null);
            host = (String) clz.getField("HOST").get(null);
            majorversion = (String) clz.getField("MAJORVERSION").get(null);
            minorversion = (String) clz.getField("MINORVERSION").get(null);
            microversion = (String) clz.getField("MICROVERSION").get(null);
            buildnum = (String) clz.getField("BUILDNUM").get(null);
        } catch (Throwable e) {
            System.out.println("build information not available: " + e);
        }

        VERSION = version;
        TYPE = type;
        RELEASE = release;
        DATE = date;
        HOST = host;
        PLATFORM = platform;
        MAJORVERSION = majorversion;
        MINORVERSION = minorversion;
        MICROVERSION = microversion;
        BUILDNUM = buildnum;
        if (TYPE != null && TYPE.length() > 0) {
            // e.g. 6.0.0_BETA2_1542.RHEL4_64 20090529191053 20090529-1912 NETWORK
            FULL_VERSION = VERSION + " " + RELEASE + " " + DATE + " " + TYPE;
        } else {
            FULL_VERSION = VERSION + " " + RELEASE + " " + DATE;
        }
    }

    private static String getType() {
        File licenseBin = new File("/opt/zimbra/bin/zmlicense");
        return licenseBin.exists() ? TYPE_NETWORK : TYPE_FOSS;
    }

    /**
     * Returns the first line in {@code /opt/zimbra/.platform}, or {@code unknown}
     * if the platform cannot be determined.
     */
    private static String getPlatform() {
        String platform = "unknown";
        File platformFile = new File(LC.zimbra_home.value(), ".platform");
        if (platformFile.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(platformFile));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    if (line.length() > 0) {
                        platform = line;
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Unable to determine platform.");
                e.printStackTrace(System.err);
            } finally {
                ByteUtil.closeReader(reader);
            }
        } else {
            System.err.format("Unable to determine platform because %s does not exist.\n", platformFile);
        }
        return platform;
    }

    public static void main(String[] args) {
        System.out.println("Version: " + VERSION);
        System.out.println("Release: " + RELEASE);
        System.out.println("Build Date: " + DATE);
        System.out.println("Build Host: " + HOST);
        System.out.println("Full Version: " + FULL_VERSION);
        System.out.println("DB Version: " + Versions.DB_VERSION);
        System.out.println("Index Version: " + Versions.INDEX_VERSION);
    }
}
