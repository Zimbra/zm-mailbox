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
package com.zimbra.cs.zclient;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.soap.Element;

public class ZDocument implements ZItem {

	private boolean mIsWiki;
	private String mName;
	private String mId;
	private String mFolderId;
	private String mVersion;
	private String mEditor;
	private String mCreator;
	private String mRestUrl;
	private long mCreatedDate;
	private long mModifiedDate;
	private long mMetaDataChangedDate;
	
	
    public ZDocument(Element e) throws ServiceException {
    	mIsWiki = "w".equals(e.getName());
    	mName = e.getAttribute(MailService.A_NAME);
    	mId = e.getAttribute(MailService.A_ID);
    	mFolderId = e.getAttribute(MailService.A_FOLDER);
    	mVersion = e.getAttribute(MailService.A_VERSION);
    	mEditor = e.getAttribute(MailService.A_LAST_EDITED_BY);
    	mCreator = e.getAttribute(MailService.A_CREATOR);
    	mRestUrl = e.getAttribute(MailService.A_REST_URL);
    	mCreatedDate = e.getAttributeLong(MailService.A_CREATED_DATE, 0) * 1000;
    	mModifiedDate = e.getAttributeLong(MailService.A_MODIFIED_DATE, 0) * 1000;
    	mMetaDataChangedDate = e.getAttributeLong(MailService.A_MODIFIED_DATE, 0) * 1000;
    }
    
	public String getId() {
		return mId;
	}
	public String getName() {
		return mName;
	}
	public String getFolderId() {
		return mFolderId;
	}
	public String getVersion() {
		return mVersion;
	}
	public String getEditor() {
		return mEditor;
	}
	public String getCreator() {
		return mCreator;
	}
	public String getRestUrl() {
		return mRestUrl;
	}
	public long getCreatedDate() {
		return mCreatedDate;
	}
	public long getModifiedDate() {
		return mModifiedDate;
	}
	public long getMetaDataChangedDate() {
		return mMetaDataChangedDate;
	}
	public boolean isWiki() {
		return mIsWiki;
	}
}
