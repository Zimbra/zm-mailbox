/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.client;

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
	private String mFirstName;
	private String mMiddleName;
	private String mLastName;
	private String mFullName;
	private String mNickname;
	private String mCompany;
	private String mFileAs;
	private boolean isGroup;
    private boolean exp;

	private ZMailbox mMailbox;

    public ZAutoCompleteMatch(Element e, ZMailbox mailbox) throws ServiceException {
        mMailbox = mailbox;
        mRanking = e.getAttribute(MailConstants.A_RANKING);
        mType = e.getAttribute(MailConstants.A_MATCH_TYPE);
        mEmail = e.getAttribute(MailConstants.A_EMAIL, null);
        mFolderId = e.getAttribute(MailConstants.A_FOLDER, null);
        mId = e.getAttribute(MailConstants.A_ID, null);
        mDisplay = e.getAttribute(MailConstants.A_DISPLAYNAME, null);
        isGroup = e.getAttributeBool(MailConstants.A_IS_GROUP, false);
        exp = e.getAttributeBool(MailConstants.A_EXP, false);
        mFirstName = e.getAttribute(MailConstants.A_FIRSTNAME, null);
        mMiddleName = e.getAttribute(MailConstants.A_MIDDLENAME, null);
        mLastName = e.getAttribute(MailConstants.A_LASTNAME, null);
        mFullName = e.getAttribute(MailConstants.A_FULLNAME, null);
        mNickname = e.getAttribute(MailConstants.A_NICKNAME, null);
        mCompany = e.getAttribute(MailConstants.A_COMPANY, null);
        mFileAs = e.getAttribute(MailConstants.A_FILEAS, null);
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

    public String getFirstName() {
        return mFirstName;
    }

    public String getMiddleName() {
        return mMiddleName;
    }

    public String getLastName() {
        return mLastName;
    }

    public String getFullName() {
        return mFullName;
    }

    public String getNickname() {
        return mNickname;
    }

    public String getCompany() {
        return mCompany;
    }

    public String getFileAs() {
        return mFileAs;
    }

	public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = new ZJSONObject();
        jo.put("ranking", mRanking);
        jo.put("type", mType);
        if (mEmail != null) jo.put("email", mEmail);
        if (mFolderId != null) jo.put("l", mFolderId);
        if (mId != null)       jo.put("id", mId);
        if (mDisplay != null)  jo.put("display", mDisplay);
        if (isGroup)           jo.put("isGroup", isGroup);
        if (exp)               jo.put("exp", exp);
        if (mFirstName  != null) jo.put("first", mFirstName);
        if (mMiddleName != null) jo.put("middle", mMiddleName);
        if (mLastName   != null) jo.put("last", mLastName);
        if (mFullName   != null) jo.put("full", mFullName);
        if (mNickname   != null) jo.put("nick", mNickname);
        if (mCompany    != null) jo.put("company", mCompany);
        if (mFileAs     != null) jo.put("fileas", mFileAs);
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
