/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.client.soap.LmcCheckSpellingRequest;
import com.zimbra.cs.client.soap.LmcCheckSpellingResponse;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMailbox.CheckSpellingResult;
import com.zimbra.cs.zclient.ZMailbox.Misspelling;

/**
 * @author bburtin
 */
public class TestSpellCheck extends TestCase {
    private static final String USER_NAME = "user1";
    
    private static final String TEXT =
        "On a cycle the fram is gone. You're completly in cotnact with it all.\n" +
        "You're in the scene, not just watching it anmore, and the sense of presence\n" +
        "is overwhelming. That concret whizing by five inches below your foot is the\n" +
        "real thing, the same \"stuff\" you walk on, it's right there, so blurred you can't\n" +
        "focus on it, yet you can put your foot down and touch it anytime, and the\n" +
        "whole thing, the whole experience, is nevr removed from immediate\n" +
        "consciousness.";
        
    private String[] mOriginalDictionaries;
    private boolean mAvailable = false;
    private String[] mOriginalAccountIgnoreWords;
    private String[] mOriginalDomainIgnoreWords;
    private String[] mOriginalCosIgnoreWords;
    
    public void setUp()
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        mOriginalDictionaries = prov.getLocalServer().getSpellAvailableDictionary();
        
        Account account = TestUtil.getAccount(USER_NAME);
        mOriginalAccountIgnoreWords = account.getPrefSpellIgnoreWord();
        mOriginalDomainIgnoreWords = prov.getDomain(account).getPrefSpellIgnoreWord();
        mOriginalCosIgnoreWords = prov.getCOS(account).getPrefSpellIgnoreWord();
        
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        CheckSpellingResult result = mbox.checkSpelling("test");
        mAvailable = result.getIsAvailable();
        if (!mAvailable) {
            ZimbraLog.test.info("Spell checking service is not available.  Skipping tests.");
        }
    }
    

    public void testCheckSpelling() throws Exception {
        if (!mAvailable) {
            return;
        }
        
        // Add ignored words to pref on account, cos, and domain.
        Account account = TestUtil.getAccount(USER_NAME);
        account.setPrefSpellIgnoreWord(new String[] { "completly" });
        Provisioning prov = Provisioning.getInstance();
        prov.getDomain(account).setPrefSpellIgnoreWord(new String[] { "anmore" });
        prov.getCOS(account).setPrefSpellIgnoreWord(new String[] { "concret" });
        
        // Send the request
        LmcSession session = TestUtil.getSoapSession(USER_NAME);
        LmcCheckSpellingRequest req = new LmcCheckSpellingRequest(TEXT);
        req.setSession(session);
        LmcCheckSpellingResponse response =
            (LmcCheckSpellingResponse)req.invoke(TestUtil.getSoapUrl());
        assertTrue(response.isAvailable());
        
        // Verify the response
        Map<String, String[]> map = new HashMap<String, String[]>();
        Iterator<String> i = response.getMisspelledWordsIterator();
        while (i.hasNext()) {
            String word = i.next();
            map.put(word, response.getSuggestions(word));
        }
        
        assertEquals("Number of misspelled words", 4, map.size());
        assertTrue("fram", response.getSuggestions("fram").length > 0);
        assertTrue("cotnact", response.getSuggestions("cotnact").length > 0);
        assertTrue("whizing", response.getSuggestions("whizing").length > 0);
        assertTrue("nevr", response.getSuggestions("nevr").length > 0);
        ZimbraLog.test.debug("Successfully tested spell checking");
    }
    
    /**
     * Confirms that <tt>GetSpellDictionaries</tt> returns the current list of
     * dictionaries from <tt>zimbraSpellAvailableDictionary</tt>.
     */
    public void testGetDictionaries()
    throws Exception {
        Server server = Provisioning.getInstance().getLocalServer();
        server.setSpellAvailableDictionary(new String[] { "dict1", "dict2" });
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        Element request = new Element.XMLElement(MailConstants.GET_SPELL_DICTIONARIES_REQUEST);
        Element response = mbox.invoke(request);
        
        // Compare results.
        Set<String> expected = new HashSet<String>();
        expected.add("dict1");
        expected.add("dict2");
        
        Set<String> actual = new HashSet<String>();
        for (Element eDict : response.listElements(MailConstants.E_DICTIONARY)) {
            actual.add(eDict.getText());
        }
        
        assertEquals(2, actual.size());
        actual.removeAll(expected);
        assertEquals(0, actual.size());
    }
    
    /**
     * Confirms that spell checking doesn't bomb on unexpected characters.
     */
    public void testUnexpectedCharacters()
    throws Exception {
        if (!mAvailable) {
            return;
        }
        
        // bug 41760 - non-breaking space
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        CheckSpellingResult result = mbox.checkSpelling("one \u00a0tuo two");
        assertEquals(1, result.getMisspellings().size());
    }
    
    /**
     * Confirms that accented characters are handled correctly (bug 41394).
     */
    public void testSpanish()
    throws Exception {
        if (!mAvailable) {
            return;
        }
        
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        CheckSpellingResult result = mbox.checkSpelling("reunion", "es");
        assertEquals(1, result.getMisspellings().size());
        Misspelling misspelling = result.getMisspellings().get(0);
        assertEquals("reunion", misspelling.getWord());
        String expected = "reuni" + "\u00f3" + "n";
        for (String suggestion : misspelling.getSuggestions()) {
            if (suggestion.equals(expected)) {
                return;
            }
        }
        fail("Could not find expected suggestion '" + expected + "'");
    }
    
    public void tearDown()
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        prov.getLocalServer().setSpellAvailableDictionary(mOriginalDictionaries);
        
        Account account = TestUtil.getAccount(USER_NAME);
        account.setPrefSpellIgnoreWord(mOriginalAccountIgnoreWords);
        prov.getDomain(account).setPrefSpellIgnoreWord(mOriginalDomainIgnoreWords);
        prov.getCOS(account).setPrefSpellIgnoreWord(mOriginalCosIgnoreWords);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestSpellCheck.class);
    }
}
