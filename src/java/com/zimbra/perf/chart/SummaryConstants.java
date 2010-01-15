/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.perf.chart;

public interface SummaryConstants {
    
    public static final String SUMMARY_TXT = "summary.txt";
    public static final String SUMMARY_CSV = "summary.csv";
    
    public static final String ADD_COUNT = "mbox_add_msg_count";

    public static final String ADD_LATENCY = "mbox_add_msg_ms_avg";

    public static final String GET_LATENCY = "mbox_get_ms_avg";

    public static final String SOAP_RESPONSE = "soap_ms_avg";

    public static final String POP_RESPONSE = "pop_ms_avg";

    public static final String IMAP_RESPONSE = "imap_ms_avg";
    
   //    server  MEM gc time
    public final static String GC_IN = "gc.csv";
    
    public static final String SERVER_IN = "zimbrastats.csv";
    
    public final static String CPU_IN = "proc.csv";
    
    public static final String IO_IN = "io-x.csv";    

    public final static String F_GC = "FULLGC%";

    public final static String Y_GC = "YGC%";    

   // server CPU
    
    public final static String USER = "user";

    public final static String SYS = "sys";

    public final static String IDLE = "idle";

    public final static String IOWAIT = "iowait";

   // Server IO     

    public static final String KEY_TO_DISK_UTIL = "disk_util.png";

    public static final String W_THROUGHPUT = "wkB/s";

    public static final String R_THROUGHPUT = "rkB/s";

    public static final String R_IOPS = "r/s";

    public static final String W_IOPS = "w/s";
    
    public static final String IO_UTIL = "%util";
}
