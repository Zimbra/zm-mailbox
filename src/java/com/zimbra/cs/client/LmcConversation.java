/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
