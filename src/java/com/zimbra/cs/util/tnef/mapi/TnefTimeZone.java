package com.zimbra.cs.util.tnef.mapi;

import java.io.IOException;
import java.util.TimeZone;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.tnef.mapi.SYSTEMTIME;
import com.zimbra.cs.util.tnef.mapi.TimeZoneDefinition;

import net.freeutils.tnef.RawInputStream;

public class TnefTimeZone {
    
    // There are cases where TimeZoneDescription is missing from incoming tnef.
    // (e.g. ActiveSync wm meeting cancellation requests!)
    // In such cases a default time zone ID can be used!!
    public static final String DEFAULT_TNEF_TIMEZONE_ID = "TnefTimeZone";
    
    static Log sLog = ZimbraLog.tnef;
    
    // [MS-OXOCAL] - v20100729
    // Appointment and Meeting Object Protocol Specification
    // indexed Standard offset from UTC+12 (international date line) in minutes
    // The second element holds an index to the standard/DST dates table; an index 
    // of -1 means there is no DST for the time zone.
    static final int[][] MS_OXOCAL_STANDARD_OFFSET = {
        {0, -1},          //0
        {12*60, 0},       //1
        {11*60, 1},       //2
        {11*60, 2},       //3
        {11*60, 2},       //4
        {10*60, 3},       //5
        {11*60, 3},       //6
        {10*60, 4},       //7
        {15*60, 5},       //8
        {16*60, 6},       //9
        {17*60, 6},       //10
        {18*60, 6},       //11
        {19*60, 6},       //12
        {20*60, 6},       //13
        {21*60, 6},       //14
        {22*60, -1},      //15
        {23*60, -1},      //16
        {0*60, 7},        //17
        {2*60, 8},        //18
        {(2*60)+30, 8},   //19
        {3*60, -1},       //20
        {4*60, -1},       //21
        {5*60, -1},       //22
        {(6*60)+30, -1},  //23
        {8*60, -1},       //24
        {(8*60)+30, 9},   //25
        {9*60, -1},       //26
        {10*60, 10},      //27
        {(15*60)+30, 11}, //28
        {13*60, 12},      //29
        {14*60, 12},      //30
        {12*60, -1},      //31
        {15*60, -1},      //32
        {16*60, -1},      //33
        {17*60, -1},      //34
        {17*60, -1},      //35
        {18*60, -1},      //36
        {18*60, 13},      //37
        {19*60, -1},      //38
        {24*60, -1},      //39
        {0*60, -1},       //40
        {1*60, -1},       //41
        {2*60, 14},       //42
        {2*60, -1},       //43
        {(2*60)+30, -1},  //44
        {4*60, 15},       //45
        {6*60, -1},       //46
        {7*60, -1},       //47
        {(7*60)+30, -1},  //48
        {10*60, 16},      //49
        {10*60, -1},      //50
        {9*60, 12},       //51
        {2*60, 17},       //52
        {2*60, 18},       //53
        {(2*60)+30, 18},  //54
        {2*60, 19},       //55
        {16*60, 20},      //56
        {4*60, 8},        //57
        {19*60, 13},      //58
        {20*60, 13}       //59
    };
    
    // [MS-OXOCAL] - v20100729
    // Standard and daylight saving dates are defined in the following format:
    // Standard date{wMonth, wDayOfWeek, wDay, wHour} Daylight date{wMonth, wDayOfWeek, wDay, wHour}
    static final int[][][] MS_OXOCAL_STAN_DST_DATES = {
        {{10,0,5,2}, {3,0,5,1}},  //0
        {{9,0,5,2}, {3,0,5,1}},   //1
        {{10,0,5,3}, {3,0,5,2}},  //2
        {{9,0,5,1}, {3,0,5,0}},   //3
        {{10,0,5,4}, {3,0,5,3}},  //4
        {{2,0,2,2}, {10,0,3,2}},  //5
        {{11,0,1,2}, {3,0,2,2}},  //6
        {{4,0,1,3}, {9,0,5,2}},   //7
        {{3,0,5,3}, {10,0,5,2}},  //8
        {{9,2,4,2}, {3,0,1,2}},   //9
        {{9,0,3,2}, {3,5,5,2}},   //10
        {{11,0,1,0}, {3,0,2,0}},  //11
        {{10,0,5,1}, {3,0,5,0}},  //12
        {{10,0,5,2}, {4,0,1,2}},  //13
        {{3,0,5,2}, {10,0,1,2}},  //14
        {{9,0,2,2}, {4,0,2,2}},   //15
        {{9,4,5,2}, {5,5,1,2}},   //16
        {{3,0,5,2}, {8,0,5,2}},   //17
        {{4,0,1,3}, {10,0,5,2}},  //18
        {{4,0,1,3}, {10,0,1,2}},  //19
        {{3,6,2,23}, {10,6,2,23}} //20
    };
    
    
    /**
     * Ref: [MS-OXOCAL] - v20100729
     * Returns a time zone from the given index;
     * The default TimeZone ID is "tnefTimeZone"
     * @param index
     * @return
     * @throws IOException 
     */
    public static TimeZone getTimeZone(int index, boolean observeDaylightSaving, String tzId) throws IOException {
        if (index < 0 || index > 59)
            return null;
        
        int bias = 0; // UTC offset in minutes
        int standardBias = 0; // offset in minutes from bias during standard time.; has a value of 0 in most cases
        int daylightBias = 0; // offset in minutes from bias during daylight saving time.
        SYSTEMTIME StandardDate = null;
        SYSTEMTIME DaylightDate = null;
        
        int startMonth = 0;     // 1 - January, 12 - December
        int startDayOfWeek = 0; // 0 - Sunday, 6 - Saturday
        int startDay = 0;       // day of the week within the month, 5 = last occurrence of that day
        int startHour = 0;      
        
        int endMonth = 0;
        int endDayOfWeek = 0;
        int endDay = 0;
        int endHour = 0;    
        
        // get the UTC+12 standard offset in minutes from MS_OXOCAL_STANDARD_OFFSET table!!
        int utcPlus12offset = MS_OXOCAL_STANDARD_OFFSET[index][0];
        int indexToDaytimeSavingDatesTable = MS_OXOCAL_STANDARD_OFFSET[index][1];
        
        if (utcPlus12offset > 12*60) 
            bias = (utcPlus12offset - 12*60) * -1;
        else if (utcPlus12offset < 12*60)
            bias = (12*60 - utcPlus12offset);
        
        if (indexToDaytimeSavingDatesTable == -1 || !observeDaylightSaving) {
            int utcOffsetInMilliseconds = bias * 60 * 1000;
            return getNoDaylightSavingTimeZoneFromUtcOffset(utcOffsetInMilliseconds);
        }

        // handle the daylight saving case here...
        // HACK!!
        // HACK!!
        // TZRule.getStandardUtcOffset() always multiply the offset by -1;
        // Hence, we are multiplying by -1 in reverse order.
        bias = bias * -1;
            
        // If daylight saving time is observed, during the daylight time period, 
        // an additional -60 offset is added to the standard offset.
        daylightBias = -60;
            
        startMonth = MS_OXOCAL_STAN_DST_DATES[indexToDaytimeSavingDatesTable][1][0];
        startDayOfWeek = MS_OXOCAL_STAN_DST_DATES[indexToDaytimeSavingDatesTable][1][1];
        startDay = MS_OXOCAL_STAN_DST_DATES[indexToDaytimeSavingDatesTable][1][2];
        startHour = MS_OXOCAL_STAN_DST_DATES[indexToDaytimeSavingDatesTable][1][3];
            
        endMonth = MS_OXOCAL_STAN_DST_DATES[indexToDaytimeSavingDatesTable][0][0];
        endDayOfWeek = MS_OXOCAL_STAN_DST_DATES[indexToDaytimeSavingDatesTable][0][1];
        endDay = MS_OXOCAL_STAN_DST_DATES[indexToDaytimeSavingDatesTable][0][2];
        endHour = MS_OXOCAL_STAN_DST_DATES[indexToDaytimeSavingDatesTable][0][3];
            
        StandardDate = new SYSTEMTIME(endMonth, endDayOfWeek, endDay, endHour);
        DaylightDate = new SYSTEMTIME(startMonth, startDayOfWeek, startDay, startHour);
            
        if (sLog.isDebugEnabled()) {
            StringBuilder debugInfo = new StringBuilder();
            debugInfo.append(bias * -1 + " " + 
                    "{" + endMonth + "," + endDayOfWeek + "," + endDay + "," + endHour + "} " + 
                    "{" + startMonth + "," + startDayOfWeek + "," + startDay + "," + startHour + "}");
            sLog.debug(debugInfo);
        }
         
        String timeZoneId;
        if (tzId == null || tzId.length() == 0)
            timeZoneId = DEFAULT_TNEF_TIMEZONE_ID;
        else
            timeZoneId = tzId;
        
        TimeZoneDefinition timeZoneDefinition = new TimeZoneDefinition(timeZoneId, bias, standardBias, daylightBias, StandardDate, DaylightDate);
        return timeZoneDefinition.getTimeZone();
    }
    
    /**
     * Returns a no daylight saving TimeZone from the utc offset 
     * @param utcOffsetInMilliseconds utc offset is milliseconds
     * @return TimeZone or, null if no matching TimeZone found
     */
    private static TimeZone getNoDaylightSavingTimeZoneFromUtcOffset(int utcOffsetInMilliseconds) {
        String[] ids = TimeZone.getAvailableIDs(utcOffsetInMilliseconds);
        for (int i = 0; i < ids.length; i++) {
            if (TimeZone.getTimeZone(ids[i]).getDSTSavings() == 0) {
                return TimeZone.getTimeZone(ids[i]);
            }
        }
        return null;
    }
    
    /**
     * [MS-OXOCAL] - v20100729
     * PidLidTimeZone
     * Type: PtypInteger32
     * The lower WORD specifies an index into a table that contains time zone information. 
     * From the upper WORD, only the highest bit is read. If that bit is set, 
     * the time zone referenced will not observe daylight saving time; 
     * otherwise, the daylight saving time dates listed in the table will be used.
     * @param ris
     * @return time zone, null if no matching time zone found
     * @throws IOException
     */
    public static TimeZone getTimeZone(RawInputStream ris) throws IOException {
        return getTimeZone(ris, null); 
    }
    
    public static TimeZone getTimeZone(RawInputStream ris, String tzId) throws IOException {
        int index = ris.readU16(); // index to tzIndex
        boolean observeDaylightSaving = !((ris.readU16() & 0x8000) == 0x8000);
        return getTimeZone(index, observeDaylightSaving, tzId); 
    }

}
