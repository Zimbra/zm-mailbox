package com.zimbra.common.util;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.zimbra.common.localconfig.LC;

public class JettyConditioner {

	private static void fixRequestLogs() throws Exception {
		
        DecimalFormat fourDigitFormat = new DecimalFormat("0000");
        DecimalFormat twoDigitFormat = new DecimalFormat("00");
    	Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT")); //jetty request log is configured to use GMT
    	cal.setTimeInMillis(System.currentTimeMillis());
 
        StringBuffer buf = new StringBuffer();
        buf.append(fourDigitFormat.format(cal.get(Calendar.YEAR)));
        buf.append("_");
        buf.append(twoDigitFormat.format(cal.get(Calendar.MONTH) + 1));
        buf.append("_");
        buf.append(twoDigitFormat.format(cal.get(Calendar.DATE)));
        String todayLog = buf.toString() + ".request.log";
        
        cal.roll(Calendar.DATE, true);
        buf = new StringBuffer();
        buf.append(fourDigitFormat.format(cal.get(Calendar.YEAR)));
        buf.append("_");
        buf.append(twoDigitFormat.format(cal.get(Calendar.MONTH) + 1));
        buf.append("_");
        buf.append(twoDigitFormat.format(cal.get(Calendar.DATE)));
        String tomorrowLog = buf.toString() + ".request.log";
		
		File reqLogDir = new File(LC.mailboxd_directory.value() + File.separator + "logs");
		File f = new File(reqLogDir, todayLog);
		f.createNewFile();
		f = new File(reqLogDir, tomorrowLog);
		f.createNewFile();
	}
	
	public static void main(String[] args) throws Exception {
		fixRequestLogs();
	}
}
