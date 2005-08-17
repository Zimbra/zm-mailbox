package com.liquidsys.coco.client.soap;

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
