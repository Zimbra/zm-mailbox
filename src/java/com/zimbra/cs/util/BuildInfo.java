/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.db.Versions;

public final class BuildInfo {

    public static final String VERSION;
    public static final String TYPE;
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
        String type = "unknown";
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
            type = (String) clz.getField("TYPE").get(null);
            release = (String) clz.getField("RELEASE").get(null);
            date = (String) clz.getField("DATE").get(null);
            host = (String) clz.getField("HOST").get(null);
            majorversion = (String) clz.getField("MAJORVERSION").get(null);
            minorversion = (String) clz.getField("MINORVERSION").get(null);
            microversion = (String) clz.getField("MICROVERSION").get(null);
            buildnum = (String) clz.getField("BUILDNUM").get(null);
        } catch (Exception e) {
            System.err.println("Exception occurred during introspecting; version information incomplete");
            e.printStackTrace(System.err);
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
