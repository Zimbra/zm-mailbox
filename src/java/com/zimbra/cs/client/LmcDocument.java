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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
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
	
	public void setID(String id)             { mId = id; }
	public void setName(String filename)     { mName = filename; }
	public void setContentType(String ct)    { mContentType = ct; }
	public void setRev(String rev)           { mRev = rev; }
	public void setFolder(String folder)     { mFolder = folder; }
	public void setLastEditor(String str)    { mLastEditedBy = str; }
	public void setLastModifiedDate(String d)  { mLastModifiedDate = d; }
	public void setAttachmentId(String aid)  { mAid = aid; }
	public void setRestUrl(String url)       { mRestUrl = url; }
	
	public String getID()               { return mId; }
	public String getName()             { return mName; }
	public String getContentType()      { return mContentType; }
	public String getRev()              { return mRev; }
	public String getFolder()           { return mFolder; }
	public String getLastEditor()       { return mLastEditedBy; }
	public String getLastModifiedDate() { return mLastModifiedDate; }
	public String getAttachmentId()     { return mAid; }
	public String getRestUrl()          { return mRestUrl; }
	
	public String toString() {
		return "Document id=" + mId + " rev=" + mRev + " filename=" + mName +
		" ct=" + mContentType + " folder=" + mFolder + " lastEditor=" + mLastEditedBy + 
		" lastModifiedDate=" + mLastModifiedDate + " restUrl=" + mRestUrl;
	}
}
