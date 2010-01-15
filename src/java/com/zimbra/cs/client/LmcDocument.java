/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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
