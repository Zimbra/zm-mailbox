/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.cs.client.soap.LmcSoapClientException;

public class LmcMessage {
    private String mID;
    private String mMsgIDHeader;
    private String mFlags;
    private long mSize;
    private String mDate;
    private String mConvID;
    private String mScore;
    private String mSubject;
    private String mFragment;
    private String mOriginalID;
    private String mFolder;
    private LmcEmailAddress mEmailAddrs[];
    private ArrayList<LmcMimePart> mMimeParts;
    private String mContentMatched;  // for SearchResponse
    private String mTag; // for AddMsg
    private String mContent; // for AddMsg
    private String mAttachmentIDs[]; // for SendMsg

    private boolean mIsUnread = false;
    
    public LmcMessage() {
        mMimeParts = new ArrayList<LmcMimePart>();
    }

    public void setFolder(String f) { mFolder = f; }
    public void setID(String i) { mID = i; }
    public void setMsgIDHeader(String h) { mMsgIDHeader = h; }
    public void setFlags(String f) {
        mFlags = f;
        mIsUnread = (mFlags != null && mFlags.indexOf("u") >= 0);
    }
    public void setSize(long s) { mSize = s; }
    public void setDate(String d) { mDate = d; }
    public void setConvID(String cid) { mConvID = cid; }
    public void setScore(String s) { mScore = s; }
    public void setOriginalID(String o) { mOriginalID = o; }
    public void setEmailAddresses(LmcEmailAddress e[]) { mEmailAddrs = e; }
    public void addMimePart(LmcMimePart m) { mMimeParts.add(m); }
    public void clearMimeParts() { mMimeParts.clear(); }
    public void setContentMatched(String c) { mContentMatched = c; }
    public void setFragment(String f) { mFragment = f; }
    public void setSubject(String s) { mSubject = s; }
    public void setTag(String t) { mTag = t; }
    public void setContent(String c) { mContent = c; }
    public void setAttachmentIDs(String ids[]) { mAttachmentIDs = ids; }

    public String getSubject() { return mSubject; }
    public String getFragment() { return mFragment; }
    public String getFolder() { return mFolder; }
    public String getID() { return mID; }
    public String getMsgIDHeader() { return mMsgIDHeader; }
    public String getFlags() { return mFlags; }
    public long getSize() { return mSize; }
    public String getDate() { return mDate; }
    public String getConvID() { return mConvID; }
    public String getScore() { return mScore; }
    public String getOriginalID() { return mOriginalID; }
    public String getContentMatched() { return mContentMatched; }
    public LmcEmailAddress[] getEmailAddresses() { return mEmailAddrs; }
    public LmcEmailAddress getFromAddress() {
    	for (int i = 0; i < mEmailAddrs.length; i++)
            if (mEmailAddrs[i].getType().equals("f"))  // XXX should have constant
                return mEmailAddrs[i];
        return null;
    }
    public int getNumMimeParts() { return mMimeParts.size(); }
    public LmcMimePart getMimePart(int i) { return mMimeParts.get(i); }
    public String getTag() { return mTag; }
    public String getContent() { return mContent; }
    public String[] getAttachmentIDs() { return mAttachmentIDs; }
    
    public String toString() {
    	String s = "Msg ID=\"" + mID + "\" folder=\"" + mFolder + "\" flags=\"" + mFlags +
            "\" size=\"" + mSize + "\" date=\"" + mDate + "\" convID=\"" + mConvID + 
            "\" score=\"" + mScore + "\" origID=\"" + mOriginalID + 
            "\" conMatched=\"" + mContentMatched + "\" fragment=\"" + mFragment + 
            "\" subject=\"" + mSubject + "\" tag=\"" + mTag + "\"";
        s += "\n\t" + ((mEmailAddrs == null) ? 0 : mEmailAddrs.length) + " email addresses";
        s += "\n\t" + mMimeParts.size(); 
        return s;
    }

    public byte[] downloadAttachment(String partNo,
                                     String baseURL,
                                     LmcSession session,
                                     String cookieDomain,
                                     int msTimeout)
    throws LmcSoapClientException, IOException {
        // set the cookie.
        if (session == null)
            System.err.println(System.currentTimeMillis() + " " + Thread.currentThread() + " LmcMessage.downloadAttachment session=null");
        
        HttpClient client = new HttpClient();
        String url = baseURL + "?id=" + getID() + "&part=" + partNo;
        GetMethod get = new GetMethod(url);

        ZAuthToken zat = session.getAuthToken();
        Map<String, String> cookieMap = zat.cookieMap(false);
        if (cookieMap != null) {
            HttpState initialState = new HttpState();
            for (Map.Entry<String, String> ck : cookieMap.entrySet()) {
                Cookie cookie = new Cookie(cookieDomain, ck.getKey(), ck.getValue(), "/", -1, false);
                initialState.addCookie(cookie);
            }
            client.setState(initialState);
            client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        }
        
        client.setConnectionTimeout(msTimeout);
        int statusCode = -1;
        try {
            statusCode = client.executeMethod(get);

            // parse the response
            if (statusCode == 200) {
                return get.getResponseBody();
            } else {
                throw new LmcSoapClientException("Attachment download failed, status=" + statusCode);
            }
        } catch (IOException e) {
            System.err.println("Attachment download failed");
            e.printStackTrace();
            throw e;
        } finally {
            get.releaseConnection();
        }
    }

    public boolean isUnread() { return mIsUnread; }
}
