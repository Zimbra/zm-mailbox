/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
package com.zimbra.cs.zclient;

import org.json.JSONException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;

import java.util.Comparator;

public class ZAutoCompleteMatch implements ToZJSONObject {
	
	private String mRanking;
	private String mType;
	private String mEmail;
	private String mFolderId;
	private String mId;
	private String mDisplay;

	private ZMailbox mMailbox;
	
    public ZAutoCompleteMatch(Element e, ZMailbox mailbox) throws ServiceException {
        mMailbox = mailbox;
        mRanking = e.getAttribute(MailConstants.A_RANKING);
        mType = e.getAttribute(MailConstants.A_MATCH_TYPE);
        mEmail = e.getAttribute(MailConstants.A_EMAIL);
        mFolderId = e.getAttribute(MailConstants.A_FOLDER, null);
        mId = e.getAttribute(MailConstants.A_ID, null);
        mDisplay = e.getAttribute(MailConstants.A_DISPLAYNAME, null);
    }
    
    public ZMailbox getMailbox() {
        return mMailbox;
    }
    
    public String getRanking() {
    	return mRanking;
    }
    
    public String getType() {
    	return mType;
    }
    
    public String getFolderId() {
        return mFolderId;
    }

    public ZFolder getFolder() throws ServiceException {
        return mMailbox.getFolderById(mFolderId);
    }

    public String getId() {
        return mId;
    }

    public boolean isGalContact() {
        return mType.equals("gal");
    }
    
    public String getEmail() {
    	return mEmail;
    }
    
    public String getDisplayName() {
    	return mDisplay;
    }

    public String getValue() {
    	if (mDisplay != null)
    		return mDisplay;
    	return mEmail;
    }
    
	public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = new ZJSONObject();
        jo.put("ranking", mRanking);
        jo.put("type", mType);
        jo.put("email", mEmail);
        if (mFolderId != null) jo.put("l", mFolderId);
        if (mId != null)       jo.put("id", mId);
        if (mDisplay != null)  jo.put("display", mDisplay);
        return jo;
	}
	
    public String dump() {
        return ZJSONObject.toString(this);
    }
    
	public static class MatchComparator implements Comparator<ZAutoCompleteMatch> {
		public int compare(ZAutoCompleteMatch a, ZAutoCompleteMatch b) {
			int r1 = Integer.parseInt(a.mRanking);
			int r2 = Integer.parseInt(b.mRanking);
			if (r1 != r2)
				return r1 - r2;
			if (a.isGalContact() ^ !b.isGalContact())
				return a.isGalContact() ? 1 : -1;
			return a.getValue().compareTo(b.getValue());
		}
	}
}
