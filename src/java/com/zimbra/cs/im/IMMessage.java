/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.zimbra.common.soap.Element;

/**
 *
 */
public class IMMessage {
    public static enum Lang {
        EN, DEFAULT;
    }
    
    public static class TextPart {
        private Lang mLang;
        private String mPlaintext = null;
        private org.dom4j.Element mElement; // always <body>...something...</body>
        
        public TextPart(Lang lang, String plaintext) {
            mLang = lang;
            mElement = org.dom4j.DocumentHelper.createElement("body");
            mElement.addText(plaintext);
        }
        
        public TextPart(Element xhtml) {
            mElement = xhtml.toXML();
        }
        
        public TextPart(org.dom4j.Element xhtml) {
            mElement = xhtml;
        }
        
        public TextPart(String plaintext) {
            mLang = Lang.DEFAULT;
            mElement = org.dom4j.DocumentHelper.createElement("body");
            mElement.addText(plaintext);
        }
        
        public Lang getLang() { return mLang; }
        
        public boolean hasXHTML() {
            if ("http://www.w3.org/1999/xhtml".equals(mElement.getNamespaceURI())) {
                return true;
            } else {
                return false;
            }
        }
        public org.dom4j.Element getHtml() { return mElement; }
        
        public String getPlainText() {
            if (mPlaintext == null) {
                if (!hasXHTML()) {
                    mPlaintext = mElement.getText();
                } else {
                    mPlaintext = depthFirstTextExtract(mElement);
                }
            }
            return mPlaintext;
        }
        
        private String depthFirstTextExtract(org.dom4j.Element cur) {
            StringBuilder toRet = new StringBuilder();
            for (Iterator iter = cur.elementIterator(); iter.hasNext();) {  
                org.dom4j.Element e = (org.dom4j.Element)iter.next();
                toRet.append(depthFirstTextExtract(e));
            }
            toRet.append(cur.getText());
            return toRet.toString();
        }
        
        @Override
        public String toString() {
            return mElement.asXML();
        }
    }
    
    @Override
    public String toString() {
        return "MESSAGE("+mDate+"): "+mSubject+" "+mBody;
    }
    
    public void setFrom(IMAddr from) { mFrom = from; }

    public void setTo(IMAddr to) {
	mTo = to;
    }
    
    
    public IMMessage(TextPart subject, TextPart body, boolean isTyping) {
        mSubject = subject;
        mBody = body;
        mDate = new Date();
        mIsTyping = isTyping;
    }
    
    void addSubject(TextPart subj) {
        if (subj.mLang == Lang.DEFAULT) {
            mSubject = subj;
            return;
        }
        
        if (mSubject == null) {
            mSubject = subj;
        }

        if (mLangSubjects == null) {
            mLangSubjects = new HashMap();
        }
        
        mLangSubjects.put(subj.mLang, subj);
    }
    
    void addBody(TextPart body) {
        if (body.mLang == Lang.DEFAULT) {
            mBody = body;
            return;
        }
        
        if (mBody == null) {
            mBody = body;
        }
        
        if (mLangBodies == null) {
            mLangBodies = new HashMap();
        }
        
        mLangBodies.put(body.mLang, body);
    }
    
    public boolean isTyping() { return mIsTyping; }
    
    private TextPart mSubject;
    private TextPart mBody;
    
    private Map<Lang, TextPart> mLangSubjects;
    private Map<Lang, TextPart> mLangBodies;
    private Date mDate;
    private IMAddr mFrom;
    private IMAddr mTo;
    private boolean mIsTyping;
    
    public Element toXml(Element parent) {
        {
            if (mSubject != null) {
                Element e = parent.addElement("subject");
                e.addElement(mSubject.getPlainText());
            }
        }
        {
            if (mBody != null) {
//              parent.addElement(parent.convertDOM(mBody.getHtml(), parent.getFactory()));
                Element e = parent.addElement("body");
                e.setText(mBody.getHtml().asXML());
            }
        }
        return parent;
    }

    public IMAddr getFrom() { return mFrom; }
    public IMAddr getTo() { return mTo; }
    public long getTimestamp() { return mDate.getTime(); }
    public Date getDate() { return mDate; }
    
    public TextPart getSubject(Lang lang) {
        if (lang == Lang.DEFAULT
                || (mSubject != null && mSubject.mLang == lang)
                || mLangSubjects==null 
                || !mLangSubjects.containsKey(lang)
        ) {
            return mSubject;
        }
        
        return mLangSubjects.get(lang);
    }
    
    public TextPart getBody() {
        return getBody(Lang.DEFAULT);
    }
    
    public TextPart getBody(Lang lang) {
        if (lang == Lang.DEFAULT
                || (mBody != null && mBody.mLang == lang)
                || mLangBodies ==null 
                || !mLangBodies.containsKey(lang)
        ) {
            return mBody;
        }
        
        return mLangBodies.get(lang);
    }
}
