/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.internet.MailDateFormat;

public class DateUtil {

    /**
     * to LDAP generalized time string
     */
    public static String toGeneralizedTime(Date date) {
        SimpleDateFormat fmt =  new SimpleDateFormat("yyyyMMddHHmmss'Z'");
        Date gmtDate;
        if (fmt.getCalendar().getTimeZone().inDaylightTime(date))
            gmtDate = new Date(date.getTime() -
                               fmt.getCalendar().getTimeZone().getRawOffset() -
                               fmt.getCalendar().getTimeZone().getDSTSavings());
        else
            gmtDate =
                new Date(date.getTime() -
                         fmt.getCalendar().getTimeZone().getRawOffset());
        return (fmt.format(gmtDate));        
    }

    /**
     * from LDAP generalized time string
     */
    public static Date parseGeneralizedTime(String time) {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
            Date localDate = fmt.parse(time);
            if (time.endsWith("Z")) {
                Date date = new Date();
                if (fmt.getCalendar().getTimeZone().inDaylightTime(date))
                    localDate =
                        new Date(localDate.getTime() +
                                fmt.getCalendar().getTimeZone().getRawOffset() +
                                fmt.getCalendar().getTimeZone().getDSTSavings());
                else
                    localDate =
                        new Date(localDate.getTime() +
                                fmt.getCalendar().getTimeZone().getRawOffset());
            }
            return localDate;
        } catch(ParseException pe) {
            return null;
        }            
    }

    /**
     * full date/time yyyy-MM-dd'T'HH:mm:ssZ
     * @param date
     * @return
     */
    public static String toISO8601(Date date)
    {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
      String result = format.format(date);
      //convert YYYYMMDDTHH:mm:ss+HH00 into YYYYMMDDTHH:mm:ss+HH:00 
      //- note the added colon for the Timezone
      result = result.substring(0, result.length()-2) 
        + ":" + result.substring(result.length()-2);
      return result;
    }

    public static Date parseRFC2822Date(String encoded, Date fallback) {
        if (encoded == null)
            return fallback;
        try {
            return new MailDateFormat().parse(encoded);
        } catch (ParseException e) {
            return fallback;
        }
    }

    public static Date parseISO8601Date(String encoded, Date fallback) {
        if (encoded == null)
            return fallback;
    
        // normalize format to "2005-10-19T16:25:38-0800"
        int length = encoded.length();
        if (length == 4)
            encoded += "-01-01T00:00:00-0000";
        else if (length == 7)
            encoded += "-01T00:00:00-0000";
        else if (length == 10)
            encoded += "T00:00:00-0000";
        else if (length < 17)
            return fallback;
        else if (encoded.charAt(16) != ':')
            encoded = encoded.substring(0, 16) + ":00" + encoded.substring(16);
        else if (length >= 22 && encoded.charAt(19) == '.')
            encoded = encoded.substring(0, 19) + encoded.substring(21);
    
        // timezone cleanup: this format understands '-0800', not '-08:00'
        int colon = encoded.lastIndexOf(':');
        if (colon > 19)
            encoded = encoded.substring(0, colon) + encoded.substring(colon + 1);
        // timezone cleanup: this format doesn't understand 'Z' or default timezones
        if (encoded.length() == 19)
            encoded += "-0000";
        else if (encoded.endsWith("Z"))
            encoded = encoded.substring(0, encoded.length() - 1) + "-0000";
    
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(encoded);
        } catch (ParseException e) {
            return fallback;
        }
    }
    
}
