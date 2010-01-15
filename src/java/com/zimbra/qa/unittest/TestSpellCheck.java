/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2010 Zimbra, Inc.
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.client.soap.LmcCheckSpellingRequest;
import com.zimbra.cs.client.soap.LmcCheckSpellingResponse;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author bburtin
 */
public class TestSpellCheck extends TestCase {
    private static String USER_NAME = "user1";

    private static String sText =
        "On a cycle the fram is gone. You're completely in cotnact with it all.\n" +
        "You're in the scene, not just watching it anymore, and the sense of presence\n" +
        "is overwhelming. That concrete whizing by five inches below your foot is the\n" +
        "real thing, the same \"stuff\" you walk on, it's right there, so blurred you can't\n" +
        "focus on it, yet you can put your foot down and touch it anytime, and the\n" +
        "whole thing, the whole experience, is nevr removed from immediate\n" +
        "consciousness.";
        
    public void testCheckSpelling() throws Exception {
        ZimbraLog.test.debug("testCheckSpelling");

        // Send the request
        LmcSession session = TestUtil.getSoapSession(USER_NAME);
        LmcCheckSpellingRequest req = new LmcCheckSpellingRequest(sText);
        req.setSession(session);
        LmcCheckSpellingResponse response =
            (LmcCheckSpellingResponse)req.invoke(TestUtil.getSoapUrl());
        
        if (!response.isAvailable()) {
            ZimbraLog.test.debug(
                "Unable to test spell checking because the service is not available.");
            return;
        }
        
        // Verify the response
        Map map = new HashMap();
        Iterator i = response.getMisspelledWordsIterator();
        while (i.hasNext()) {
            String word = (String)i.next();
            map.put(word, response.getSuggestions(word));
        }
        
        assertEquals("Number of misspelled words", 4, map.size());
        assertTrue("fram", response.getSuggestions("fram").length > 0);
        assertTrue("cotnact", response.getSuggestions("cotnact").length > 0);
        assertTrue("whizing", response.getSuggestions("whizing").length > 0);
        assertTrue("nevr", response.getSuggestions("nevr").length > 0);
        ZimbraLog.test.debug("Successfully tested spell checking");
    }
}
