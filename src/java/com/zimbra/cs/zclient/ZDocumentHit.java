/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import org.json.JSONException;

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

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("id", mId);
        zjo.put("sortField", mSortField);
        zjo.put("score", mScore);
        zjo.put("document", mDoc);
        return zjo;
    }

    public String toString() {
        return ZJSONObject.toString(this);
    }
}
