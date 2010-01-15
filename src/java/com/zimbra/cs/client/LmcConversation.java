/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.client;

public class LmcConversation {

    // attributes
    private String mID;
    private String mDate;
    private String mTags;
    private String mFlags;
    private String mFolder;
    private long mNumMsgs;

    // contained as elements inside the <c> element
    private String mFragment;
    private String mSubject;
    private LmcEmailAddress[] mParticipants;
    private LmcMessage[] mMsgs;

    public void setID(String id) { mID = id; }
    public void setDate(String d) { mDate = d; }
    public void setTags(String t) { mTags = t; }
    public void setFlags(String f) { mFlags = f; }
    public void setFolder(String f) { mFolder = f; }
    public void setNumMessages(long n) { mNumMsgs = n; }
    public void setSubject(String s) { mSubject = s; }
    public void setFragment(String f) { mFragment = f; }
    public void setParticipants(LmcEmailAddress e[]) { mParticipants = e; }
    public void setMessages(LmcMessage m[]) { mMsgs = m; }

    public String getDate() { return mDate; }
    public String getID() { return mID; }
    public String getTags() { return mTags; }
    public String getFlags() { return mFlags; }
    public String getSubject() { return mSubject; }
    public String getFragment() { return mFragment; }
    public String getFolder() { return mFolder; }
    public LmcEmailAddress[] getParticipants() { return mParticipants; }
    public LmcMessage[] getMessages() { return mMsgs; }

    private static String getCount(Object o[]) {
    	return (o == null) ? "0" : Integer.toString(o.length);
    }
    
    public String toString() {
    	return "Conversation: id=\"" + mID + "\" date=\"" + mDate + "\" tags=\"" + mTags +
            "\" flags=\"" + mFlags + "\" folder=\"" + mFolder + "\" numMsgs=\"" + mNumMsgs + 
			"\" subject=\"" + mSubject + "\" fragment=\"" + mFragment + "\" and " + 
            getCount(mParticipants) + " participants";
    }
}
