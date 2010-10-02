/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.qa.unittest;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.zclient.ZFeatures;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZPrefs;

import junit.framework.TestCase;

public class TestZClient
extends TestCase {
    
    private static final String USER_NAME = "user1";

    /**
     * Confirms that the prefs accessor works (bug 51384).
     */
    public void testPrefs()
    throws Exception {
        Account account = TestUtil.getAccount(USER_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZPrefs prefs = mbox.getPrefs();
        assertEquals(account.getPrefLocale(), prefs.getLocale());
    }
    
    /**
     * Confirms that the features accessor doesn't throw NPE (bug 51384).
     */
    public void testFeatures()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFeatures features = mbox.getFeatures();
        features.getPop3Enabled();
    }
}
