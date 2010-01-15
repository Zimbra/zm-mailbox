/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client.soap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class LmcCheckSpellingResponse extends LmcSoapResponse {
    private Map mMisspelled = new HashMap();
    private boolean mIsAvailable;
    
    public LmcCheckSpellingResponse(boolean isAvailable) {
        mIsAvailable = isAvailable;
    }
    
    /**
     * Returns <code>true</code> if the spell check service is available.
     */
    public boolean isAvailable() {
        return mIsAvailable;
    }
    
    /**
     * Adds a word and its suggested spellings to the list.
     * @param word the misspelled word
     * @param suggestions the array of suggested replacements for the given word
     */
    public void addMisspelled(String word, String[] suggestions) {
        if (suggestions == null) {
            suggestions = new String[0];
        }
        mMisspelled.put(word, suggestions);
    }
    
    public Iterator getMisspelledWordsIterator() {
        return mMisspelled.keySet().iterator();
    }
    
    /**
     * Returns the array of suggested replacements
     * for the given misspelled word, or an empty array if the
     * word has not been added to this response or the spell
     * check service is not available.
     */
    public String[] getSuggestions(String word) {
        return (String[]) mMisspelled.get(word);
    }
}
