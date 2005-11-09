/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;

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
    private ArrayList mMimeParts;  // of type LmcMimePart
    private String mContentMatched;  // for SearchResponse
    private String mTag; // for AddMsg
    private String mContent; // for AddMsg
    private String mAttachmentIDs[]; // for SendMsg

    private boolean mIsUnread = false;
    
    public LmcMessage() {
        mMimeParts = new ArrayList();
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
    public LmcMimePart getMimePart(int i) { return (LmcMimePart) mMimeParts.get(i); }
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
    throws LmcSoapClientException {
        // set the cookie.
        if (session == null)
            System.err.println(System.currentTimeMillis() + " " + Thread.currentThread() + " LmcMessage.downloadAttachment session=null");
        Cookie cookie = new Cookie(cookieDomain, "ZM_AUTH_TOKEN", session.getAuthToken(), "/", -1, false);
        HttpState initialState = new HttpState();
        initialState.addCookie(cookie);
        initialState.setCookiePolicy(CookiePolicy.COMPATIBILITY);
        HttpClient client = new HttpClient();
        client.setState(initialState);
        
        // make the get
        String url = baseURL + "?id=" + getID() + "&part=" + partNo;
        GetMethod get = new GetMethod(url);
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
            e.printStackTrace();
            throw new LmcSoapClientException("Attachment download failed");
        } finally {
            get.releaseConnection();
        }
    }

    public boolean isUnread() { return mIsUnread; }
}
