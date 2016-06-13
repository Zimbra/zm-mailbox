/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

public class LmcDocument {
	protected String mId;
	protected String mName;
	protected String mContentType;
	protected String mRev;
	protected String mFolder;
	protected String mLastEditedBy;
	protected String mLastModifiedDate;
	protected String mAid;
	protected String mRestUrl;
	protected String mFragment;
	protected String mCreator;
	protected String mCreateDate;
	
	public void setID(String id)             { mId = id; }
	public void setName(String filename)     { mName = filename; }
	public void setContentType(String ct)    { mContentType = ct; }
	public void setRev(String rev)           { mRev = rev; }
	public void setFolder(String folder)     { mFolder = folder; }
	public void setLastEditor(String str)    { mLastEditedBy = str; }
	public void setLastModifiedDate(String d)  { mLastModifiedDate = d; }
	public void setAttachmentId(String aid)  { mAid = aid; }
	public void setRestUrl(String url)       { mRestUrl = url; }
	public void setFragment(String f)        { mFragment = f; }
	public void setCreator(String cr)        { mCreator = cr; }
	public void setCreateDate(String cd)     { mCreateDate = cd; }
	
	public String getID()               { return mId; }
	public String getName()             { return mName; }
	public String getContentType()      { return mContentType; }
	public String getRev()              { return mRev; }
	public String getFolder()           { return mFolder; }
	public String getLastEditor()       { return mLastEditedBy; }
	public String getLastModifiedDate() { return mLastModifiedDate; }
	public String getAttachmentId()     { return mAid; }
	public String getRestUrl()          { return mRestUrl; }
	public String getFragment()         { return mFragment; }
	public String getCreator()          { return mCreator; }
	public String getCreateDate()       { return mCreateDate; }
	
	public String toString() {
		return "Document id=" + mId + " rev=" + mRev + " filename=" + mName +
		" ct=" + mContentType + " folder=" + mFolder + " lastEditor=" + mLastEditedBy + 
		" lastModifiedDate=" + mLastModifiedDate + " restUrl=" + mRestUrl;
	}
}
