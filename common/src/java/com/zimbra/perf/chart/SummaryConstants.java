/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
