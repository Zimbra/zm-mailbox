/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
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
    throws Exception {
        // set the cookie.
        if (session == null)
            System.err.println(System.currentTimeMillis() + " " + Thread.currentThread() + " LmcMessage.downloadAttachment session=null");
        
        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        String url = baseURL + "?id=" + getID() + "&part=" + partNo;
        HttpGet get = new HttpGet(url);

        ZAuthToken zat = session.getAuthToken();
        Map<String, String> cookieMap = zat.cookieMap(false);
        if (cookieMap != null) {
            BasicCookieStore initialState = new BasicCookieStore();
            for (Map.Entry<String, String> ck : cookieMap.entrySet()) {
                BasicClientCookie cookie = new BasicClientCookie(ck.getKey(), ck.getValue());
                cookie.setDomain(cookieDomain);
                cookie.setPath("/");
                cookie.setSecure(false);
                cookie.setExpiryDate(null);
                initialState.addCookie(cookie);
            }
            clientBuilder.setDefaultCookieStore(initialState);
            
            RequestConfig reqConfig = RequestConfig.copy(
                ZimbraHttpConnectionManager.getInternalHttpConnMgr().getZimbraConnMgrParams().getReqConfig())
                .setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build();

            clientBuilder.setDefaultRequestConfig(reqConfig);
        }

        SocketConfig config = SocketConfig.custom().setSoTimeout(msTimeout).build();
        clientBuilder.setDefaultSocketConfig(config);
        HttpClient client = clientBuilder.build();
        int statusCode = -1;
        try {
            HttpResponse response = HttpClientUtil.executeMethod(client, get);

            // parse the response
            if (response.getStatusLine().getStatusCode() == 200) {
                return EntityUtils.toByteArray(response.getEntity());
            } else {
                throw new LmcSoapClientException("Attachment download failed, status=" + statusCode);
            }
        } catch (IOException | HttpException e) {
            System.err.println("Attachment download failed");
            e.printStackTrace();
            throw e;
        } finally {
            get.releaseConnection();
        }
    }

    public boolean isUnread() { return mIsUnread; }
}
