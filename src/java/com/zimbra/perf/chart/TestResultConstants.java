package com.zimbra.perf.chart;



public interface TestResultConstants {
	
	public static final String RESULT_REPORT = "result.txt";
	public static final String RESULT_CSV = "result.csv";
	
	public static final String ADD_COUNT = "mbox_add_msg_count";

	public static final String ADD_LATENCY = "mbox_add_msg_ms_avg";

	public static final String GET_LATENCY = "mbox_get_ms_avg";

	public static final String SOAP_RESPONSE = "soap_ms_avg";

	public static final String POP_RESPONSE = "pop_ms_avg";

	public static final String IMAP_RESPONSE = "imap_ms_avg";
	
	
   //	server  MEM gc time
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
