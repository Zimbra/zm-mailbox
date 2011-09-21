/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.zimbra.common.service.ServiceException;

public class DateUtilTest {
    
    @Test
    public void getTimeInterval() throws Exception {
        assertEquals(5, DateUtil.getTimeInterval("5ms"));
        assertEquals(10 * Constants.MILLIS_PER_SECOND, DateUtil.getTimeInterval("10s"));
        assertEquals(321 * Constants.MILLIS_PER_SECOND, DateUtil.getTimeInterval("321"));
        assertEquals(5 * Constants.MILLIS_PER_HOUR, DateUtil.getTimeInterval("5h"));
        assertEquals(5 * Constants.MILLIS_PER_DAY, DateUtil.getTimeInterval("5d"));
    }

    @Test
    public void getTimeIntervalWithDefault() throws Exception {
        assertEquals(1, DateUtil.getTimeInterval("abc", 1));
        assertEquals(2, DateUtil.getTimeInterval("1a", 2));

        assertEquals(5, DateUtil.getTimeInterval("5ms", 0));
        assertEquals(10 * Constants.MILLIS_PER_SECOND, DateUtil.getTimeInterval("10s", 0));
        assertEquals(321 * Constants.MILLIS_PER_SECOND, DateUtil.getTimeInterval("321", 0));
        assertEquals(5 * Constants.MILLIS_PER_HOUR, DateUtil.getTimeInterval("5h", 0));
        assertEquals(5 * Constants.MILLIS_PER_DAY, DateUtil.getTimeInterval("5d", 0));
    }
    
    @Test
    public void getTimeIntervalSecs() throws Exception {
        assertEquals(1, DateUtil.getTimeIntervalSecs("abc", 1));
        assertEquals(2, DateUtil.getTimeIntervalSecs("1a", 2));
        
        assertEquals(1, DateUtil.getTimeIntervalSecs("1000ms", 0));
        assertEquals(1, DateUtil.getTimeIntervalSecs("1499ms", 0));
        assertEquals(2, DateUtil.getTimeIntervalSecs("1500ms", 0));
        assertEquals(10, DateUtil.getTimeIntervalSecs("10s", 0));
        assertEquals(321, DateUtil.getTimeIntervalSecs("321", 0));
        assertEquals(5 * Constants.SECONDS_PER_HOUR, DateUtil.getTimeIntervalSecs("5h", 0));
        assertEquals(5 * Constants.SECONDS_PER_DAY, DateUtil.getTimeIntervalSecs("5d", 0));
    }
    
    @Test
    public void negativeTimeInterval() throws Exception {
        assertEquals(1, DateUtil.getTimeInterval("-5", 1));
        assertEquals(1, DateUtil.getTimeInterval("-5m", 1));
        assertEquals(1, DateUtil.getTimeInterval("-30d", 1));
        try {
            DateUtil.getTimeInterval("-5s");
            fail("Parse should have failed");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }
}
