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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import com.zimbra.soap.Element;

public class ZDocumentHit implements ZSearchHit {

	private ZDocument mDoc;
	private String mId;
	private String mSortField;
	private float mScore;
	
    public ZDocumentHit(Element e) throws ServiceException {
    	mId = e.getAttribute(MailConstants.A_ID);
    	mSortField = e.getAttribute(MailConstants.A_SORT_FIELD, null);
    	mScore = (float) e.getAttributeDouble(MailConstants.A_SCORE, 0);
    	mDoc = new ZDocument(e);
    }
    
    public ZDocument getDocument() {
    	return mDoc;
    }
    
	public String getId() {
		return mId;
	}

	public float getScore() {
		return mScore;
	}

	public String getSortField() {
		return mSortField;
	}

	public void modifyNotification(ZModifyEvent event) throws ServiceException {
		// TODO Auto-generated method stub
	}

}
