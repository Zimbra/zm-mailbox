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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateParser;

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
        s = s.replace("\\", "\\\\");
        s = s.replace("\"", "\\\"");
        return s;
    }
}

