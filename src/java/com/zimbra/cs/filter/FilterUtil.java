/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.filter;

import java.util.Arrays;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.DateParser;
import com.zimbra.common.util.ZimbraLog;

public class FilterUtil {

    public static final DateParser SIEVE_DATE_PARSER = new DateParser("yyyyMMdd");

    public enum Condition {
        allof, anyof;
        
        public static Condition fromString(String value)
        throws ServiceException {
            if (value == null) {
                return null;
            }
            try {
                return Condition.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR(
                    "Invalid value: " + value + ", valid values: " + Arrays.asList(Condition.values()), e);
            }
        }
        
    }

    public enum Flag {
        read, flagged;

        public static Flag fromString(String value)
        throws ServiceException {
            if (value == null) {
                return null;
            }
            try {
                return Flag.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR(
                    "Invalid value: " + value + ", valid values: " + Arrays.asList(Flag.values()), e);
            }
        }
    }

    public enum StringComparison {
        is, contains, matches;

        public static StringComparison fromString(String value)
        throws ServiceException {
            if (value == null) {
                return null;
            }
            try {
                return StringComparison.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR(
                    "Invalid value: "+ value +", valid values: " + Arrays.asList(StringComparison.values()), e);
            }
        }
    }

    public enum NumberComparison {
        over, under;
        
        public static NumberComparison fromString(String value)
        throws ServiceException {
            if (value == null) {
                return null;
            }
            try {
                return NumberComparison.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR(
                    "Invalid value: "+ value +", valid values: " + Arrays.asList(NumberComparison.values()), e);
            }
        }
    }

    public enum DateComparison {
        before, after;
        
        public static DateComparison fromString(String value)
        throws ServiceException {
            if (value == null) {
                return null;
            }
            try {
                return DateComparison.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw ServiceException.PARSE_ERROR(
                    "Invalid value: "+ value +", valid values: " + Arrays.asList(StringComparison.values()), e);
            }
        }
    }
    
    /**
     * Returns a Sieve-escaped version of the given string.  Replaces <tt>\</tt> with
     * <tt>\\</tt> and <tt>&quot;</tt> with <tt>\&quot;</tt>.
     */
    public static String escape(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        s = s.replace("\\", "\\\\");
        s = s.replace("\"", "\\\"");
        return s;
    }
    
    /**
     * Removes escape characters from a Sieve string literal.
     */
    public static String unescape(String s) {
        s = s.replace("\\\"", "\"");
        s = s.replace("\\\\", "\\");
        return s;
    }

    /**
     * Adds a value to the given <tt>Map</tt>.  If <tt>initialKey</tt> already
     * exists in the map, uses the next available index instead.  This way we
     * guarantee that we don't lose data if the client sends two elements with
     * the same index, or doesn't specify the index at all.
     * 
     * @return the index used to insert the value
     */
    public static <T> int addToMap(Map<Integer, T> map, int initialKey, T value) {
        int i = initialKey;
        while (true) {
            if (!map.containsKey(i)) {
                map.put(i, value);
                return i;
            }
            i++;
        }
    }

    public static int getIndex(Element actionElement) {
        String s = actionElement.getAttribute(MailConstants.A_INDEX, "0");
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            ZimbraLog.soap.warn("Unable to parse index value %s for element %s.  Ignoring order.",
                s, actionElement.getName());
            return 0;
        }
    }
    
    /**
     * Parses a Sieve size string and returns the integer value.
     * The string may end with <tt>K</tt> (kilobytes), <tt>M</tt> (megabytes)
     * or <tt>G</tt> (gigabytes).  If the units are not specified, the
     * value is in bytes.
     * 
     * @throws NumberFormatException if the value cannot be parsed
     */
    public static int parseSize(String sizeString) {
        if (sizeString == null || sizeString.length() == 0) {
            return 0;
        }
        sizeString = sizeString.toUpperCase();
        int multiplier = 1;
        if (sizeString.endsWith("K")) {
            sizeString = sizeString.substring(0, sizeString.length() - 1);
            multiplier = 1024;
        } else if (sizeString.endsWith("M")) {
            sizeString = sizeString.substring(0, sizeString.length() - 1);
            multiplier = 1024 * 1024;
        } else if (sizeString.endsWith("G")) {
            sizeString = sizeString.substring(0, sizeString.length() - 1);
            multiplier = 1024 * 1024 * 1024;
        }
        return Integer.parseInt(sizeString) * multiplier;
    }
}

