/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.client.ZMailbox;
import com.zimbra.soap.mail.message.CheckSpellingResponse;
import com.zimbra.soap.mail.type.Misspelling;

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
        
    private String[] originalDictionaries;
    private boolean available = false;
    private String[] originalAccountIgnoreWords;
    private String[] originalDomainIgnoreWords;
    private String[] originalCosIgnoreWords;
    private String originalIgnoreAllCaps;
    
    public void setUp()
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        originalDictionaries = prov.getLocalServer().getSpellAvailableDictionary();
        
        Account account = TestUtil.getAccount(USER_NAME);
        originalAccountIgnoreWords = account.getPrefSpellIgnoreWord();
        originalDomainIgnoreWords = prov.getDomain(account).getPrefSpellIgnoreWord();
        originalCosIgnoreWords = prov.getCOS(account).getPrefSpellIgnoreWord();
        originalIgnoreAllCaps = account.getAttr(Provisioning.A_zimbraPrefSpellIgnoreAllCaps);
        
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        CheckSpellingResponse result = mbox.checkSpelling("test");
        available = result.isAvailable();
        if (!available) {
            ZimbraLog.test.info("Spell checking service is not available.  Skipping tests.");
        }
    }
    

    public void testCheckSpelling() throws Exception {
        if (!available) {
            return;
        }
        
        // Add ignored words to pref on account, cos, and domain.
        Account account = TestUtil.getAccount(USER_NAME);
        account.setPrefSpellIgnoreWord(new String[] { "completly" });
        Provisioning prov = Provisioning.getInstance();
        prov.getDomain(account).setPrefSpellIgnoreWord(new String[] { "anmore" });
        prov.getCOS(account).setPrefSpellIgnoreWord(new String[] { "concret" });
        
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        CheckSpellingResponse result = mbox.checkSpelling(TEXT);
        assertTrue(result.isAvailable());
        
        // Verify the response
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        for (Misspelling mis : result.getMisspelledWords()) {
            map.put(mis.getWord(), mis.getSuggestionsList());
        }
        
        assertEquals("Number of misspelled words", 4, getNumMisspellings(result));
        assertTrue("fram", hasSuggestion(result, "fram", "from"));
        assertTrue("cotnact", hasSuggestion(result, "cotnact", "contact"));
        assertTrue("whizing", hasSuggestion(result, "whizing", "whizzing"));
        assertTrue("nevr", hasSuggestion(result, "nevr", "never"));
        ZimbraLog.test.debug("Successfully tested spell checking");
    }
    
    /**
     * Tests the <tt>ignore</tt> attribute for <tt>CheckSpellingRequest</tt>.
     */
    public void testIgnore()
    throws Exception {
        if (!available) {
            return;
        }
        
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        CheckSpellingResponse result = mbox.checkSpelling("one twi thre forr", null, Arrays.asList("twi", "thre"));
        assertEquals("Number of misspelled words", 1, getNumMisspellings(result));
        assertTrue(hasSuggestion(result, "forr", "four"));
    }
    
    private int getNumMisspellings(CheckSpellingResponse result) {
        return result.getMisspelledWords().size();
    }
    
    private boolean hasSuggestion(CheckSpellingResponse result, String misspelled, String expectedSuggestion) {
        for (Misspelling mis : result.getMisspelledWords()) {
            if (mis.getWord().equals(misspelled)) {
                for (String suggestion : mis.getSuggestionsList()) {
                    if (suggestion.equals(expectedSuggestion)) {
                        return true;
                    }
                }
            }
        }
        return false;
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
        if (!available) {
            return;
        }
        
        // bug 41760 - non-breaking space
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        CheckSpellingResponse result = mbox.checkSpelling("one \u00a0tuo two");
        assertEquals(1, result.getMisspelledWords().size());
    }
    
    /**
     * Confirms that accented characters are returned correctly (bug 41394).
     */
    public void testReceiveSpanish()
    throws Exception {
        if (!available) {
            return;
        }
        
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        CheckSpellingResponse result = mbox.checkSpelling("reunion", "es");
        assertEquals(1, result.getMisspelledWords().size());
        Misspelling misspelling = result.getMisspelledWords().get(0);
        assertEquals("reunion", misspelling.getWord());
        assertTrue(misspelling.getSuggestionsList().contains("reuni\u00f3n"));
    }
    
    /**
     * Confirms that accented characters are sent correctly (bug 43626).
     */
    public void testSendSpanish()
    throws Exception {
        if (!available) {
            return;
        }
        
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        CheckSpellingResponse result = mbox.checkSpelling("\u00faltimos esst\u00e1", "es");
        assertEquals(1, result.getMisspelledWords().size());
        Misspelling misspelling = result.getMisspelledWords().get(0);
        assertEquals("esst\u00e1", misspelling.getWord());
        assertTrue(misspelling.getSuggestionsList().contains("est\u00e1"));
    }
    
    public void testRussian()
    throws Exception {
        if (!available) {
            return;
        }
        
        String krokodil = "\u041a\u0440\u043e\u043a\u043e\u0434\u0438\u043b";
        String krokodilMisspelled = "\u041a\u0440\u043e\u043a\u043e\u0434\u0438\u043b\u043b";
        String cherepaha = "\u0427\u0435\u0440\u0435\u043f\u0430\u0445\u0430";
        
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        CheckSpellingResponse result = mbox.checkSpelling(krokodilMisspelled + " " + cherepaha, "ru");
        assertEquals(1, result.getMisspelledWords().size());
        Misspelling misspelling = result.getMisspelledWords().get(0);
        assertEquals(krokodilMisspelled, misspelling.getWord());
        assertTrue(misspelling.getSuggestionsList().contains(krokodil));
    }
    
    public void testAllCaps()
    throws Exception {
        if (!available) {
            return;
        }
        
        Account account = TestUtil.getAccount(USER_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        
        account.setPrefSpellIgnoreAllCaps(false);
        CheckSpellingResponse result = mbox.checkSpelling("XYZ");
        assertEquals(1, result.getMisspelledWords().size());
        
        account.setPrefSpellIgnoreAllCaps(true);
        result = mbox.checkSpelling("XYZ");
        assertEquals(0, result.getMisspelledWords().size());
    }
    
    public void tearDown()
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        prov.getLocalServer().setSpellAvailableDictionary(originalDictionaries);
        
        Account account = TestUtil.getAccount(USER_NAME);
        account.setPrefSpellIgnoreWord(originalAccountIgnoreWords);
        prov.getDomain(account).setPrefSpellIgnoreWord(originalDomainIgnoreWords);
        prov.getCOS(account).setPrefSpellIgnoreWord(originalCosIgnoreWords);
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraPrefSpellIgnoreAllCaps, originalIgnoreAllCaps);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestSpellCheck.class);
    }
}
