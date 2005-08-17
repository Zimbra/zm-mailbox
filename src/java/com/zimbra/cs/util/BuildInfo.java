package com.zimbra.cs.util;

import com.zimbra.cs.db.Versions;

public class BuildInfo {
    
    public static final String VERSION;
    public static final String RELEASE;
    public static final String DATE;
    public static final String HOST;

    static {
        String version = "unknown";
        String release = "unknown";
        String date = "unknown";
        String host = "unknown";
        try {
            Class clz = Class.forName("com.zimbra.cs.util.BuildInfoGenerated");
            version = (String) clz.getField("VERSION").get(null);
            release = (String) clz.getField("RELEASE").get(null);
            date = (String) clz.getField("DATE").get(null);
            host = (String) clz.getField("HOST").get(null);
        } catch (Exception e) {
            System.err.println("Exception occurred during introspecting; version information incomplete");
            e.printStackTrace();
        }
        VERSION = version;
        RELEASE = release;
        DATE = date;
        HOST = host;
    }

    public static void main(String[] args) {
        System.out.println("Version: " + VERSION);
        System.out.println("Release: " + RELEASE);
        System.out.println("Build Date: " + DATE);
        System.out.println("Build Host: " + HOST);
        System.out.println("DB Version: " + Versions.DB_VERSION);
        System.out.println("Index Version: " + Versions.INDEX_VERSION);
    }
}
