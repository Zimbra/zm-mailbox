/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009 Zimbra, Inc.
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
package com.zimbra.cs.index;

import java.util.List;

import com.zimbra.common.soap.Element;

public class SpellSuggestQueryInfo implements QueryInfo {
    
    static class Suggestion {
        public String mStr;
        public int mDocs;
        public int mEditDist;
    }
    
    private String mMisSpelled;
    private List<Suggestion> mSuggestions;
    
    SpellSuggestQueryInfo(String misSpelled, List<Suggestion> suggestions) {
        mMisSpelled = misSpelled;
        mSuggestions = suggestions;
    }

    public Element toXml(Element parent) {
        Element ms = parent.addElement("spell");
        ms.addAttribute("word", mMisSpelled);
        for (Suggestion s : mSuggestions) {
            Element elt = ms.addElement("sug");
            elt.addAttribute("dist", s.mEditDist);
            elt.addAttribute("numDocs", s.mDocs);
            elt.addAttribute("value", s.mStr);
        }
        return ms;
    }
    
    public String toString() {
        String toRet = "SUGGEST("+mMisSpelled+" [";
        for (Suggestion s : mSuggestions) {
            toRet = toRet +"("+ s.mStr+","+s.mEditDist+", "+s.mDocs+")   ";
        }
        return toRet + "]";
    }

}
