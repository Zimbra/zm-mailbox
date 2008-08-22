/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007 Zimbra, Inc.
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

package com.zimbra.cs.util;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.Versions;

public class BuildInfo {
    
    public static final String VERSION;
    public static final String TYPE;
    public static final String RELEASE;
    public static final String DATE;
    public static final String HOST;
    
    public static final String FULL_VERSION;

    static {
        String version = "unknown";
        String type = "unknown";
        String release = "unknown";
        String date = "unknown";
        String host = "unknown";
        try {
            Class clz = Class.forName("com.zimbra.cs.util.BuildInfoGenerated");
            version = (String) clz.getField("VERSION").get(null);
            type = (String) clz.getField("TYPE").get(null);
            release = (String) clz.getField("RELEASE").get(null);
            date = (String) clz.getField("DATE").get(null);
            host = (String) clz.getField("HOST").get(null);
        } catch (Exception e) {
            System.err.println("Exception occurred during introspecting; version information incomplete");
            e.printStackTrace();
        }
        VERSION = version;
        TYPE = type;
        RELEASE = release;
        DATE = date;
        HOST = host;
        
        if (TYPE != null && TYPE.length() > 0) {
        	FULL_VERSION = VERSION + " " + RELEASE + " " + DATE + " " + TYPE;
        } else {
        	FULL_VERSION = VERSION + " " + RELEASE + " " + DATE;
        }
    }
    
    public static class Version {
        int mMajor;
        int mMinor;
        int mPatch;
        
        /**
         * 
         * @param version String in the format of <major number>.<minor number>.<patch number>
         */
        public Version(String version) throws ServiceException {
            String[] parts = version.split("\\.");
            
            if (parts.length == 1)
                mMajor = Integer.parseInt(parts[0]);
            else if (parts.length == 2) {
                mMajor = Integer.parseInt(parts[0]);
                mMinor = Integer.parseInt(parts[1]);
            } else if (parts.length == 3) {
                mMajor = Integer.parseInt(parts[0]);
                mMinor = Integer.parseInt(parts[1]);
                mPatch = Integer.parseInt(parts[2]);
            } else
                throw ServiceException.FAILURE("invalid version format:" + version, null); 
        }
        
        /**
         * Compares the two versions
         * 
         * e.g. 
         *     Version.compare("5.0.10", "5.0.9")  returns > 0
         *     Version.compare("5.0.10", "5.0.10") returns == 0
         *     Version.compare("5.0", "5.0.9")     returns < 0
         * 
         * @param versionX
         * @param versionY
         * 
         * @return a negative integer, zero, or a positive integer as versionX is older than, equal to, or newer than the versionY.
         */
        public static int compare(String versionX, String versionY) throws ServiceException {
            Version x = new Version(versionX);
            Version y = new Version(versionY);
            return x.compare(y);
        }
        
        /**
         * Compares this object with the specified version.
         * 
         * @param version
         * @return a negative integer, zero, or a positive integer as this object is older than, equal to, or newer than the specified version.
         */
        public int compare(String version) throws ServiceException  {
            Version other = new Version(version);
            return compare(other);
        }
        
        /**
         * Compares this object with the specified version.
         * 
         * @param version
         * @return a negative integer, zero, or a positive integer as this object is older than, equal to, or newer than the specified version.
         */
        public int compare(Version version) throws ServiceException  {
            int r = mMajor - version.mMajor;
            if (r != 0)
                return r;
            
            r = mMinor - version.mMinor;
            if (r != 0)
                return r;
            
            return mPatch - version.mPatch;
        }
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
