/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client.soap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class LmcCheckSpellingResponse extends LmcSoapResponse {
    private Map<String, String[]> mMisspelled = new HashMap<String, String[]>();
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
    
    public Iterator<String> getMisspelledWordsIterator() {
        return mMisspelled.keySet().iterator();
    }
    
    /**
     * Returns the array of suggested replacements
     * for the given misspelled word, or an empty array if the
     * word has not been added to this response or the spell
     * check service is not available.
     */
    public String[] getSuggestions(String word) {
        return mMisspelled.get(word);
    }
}
