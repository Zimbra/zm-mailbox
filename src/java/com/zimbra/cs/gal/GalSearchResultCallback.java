/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.gal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;

public class GalSearchResultCallback implements GalContact.Visitor {
    private Element mResponse;
	protected ItemIdFormatter mFormatter;
	private boolean mIdOnly;
	private GalOp mOp;
    
    public GalSearchResultCallback(GalSearchParams params) {
    	reset(params);
    	mOp = params.getOp();
    }
    
    public void reset(GalSearchParams params) {
    	if (params.getSoapContext() != null) {
    		mResponse = params.getSoapContext().createElement(params.getResponseName());
        	mFormatter = new ItemIdFormatter(params.getSoapContext());
    	} else {
    	    mResponse = Element.XMLElement.mFactory.createElement(params.getResponseName());
    	    mFormatter = new ItemIdFormatter();
    	}
    	params.setGalResult(SearchGalResult.newSearchGalResult(this));
    	mIdOnly = params.isIdOnly();
    }
    
    public void visit(GalContact c) throws ServiceException {
    	handleContact(c);
    }
    
    public Element getResponse() {
    	return mResponse;
    }
    
    public Element handleContact(Contact c) throws ServiceException {
    	if (mIdOnly)
            return mResponse.addElement(MailConstants.E_CONTACT).addAttribute(MailConstants.A_ID, mFormatter.formatItemId(c));
    	else if (mOp == GalOp.autocomplete)
    		return ToXML.encodeContact(mResponse, mFormatter, c, true, c.getAllFields().keySet());
    	else
            return ToXML.encodeContact(mResponse, mFormatter, c, true, null);
    }
    
    public void handleContact(GalContact c) throws ServiceException {
		ToXML.encodeGalContact(mResponse, c);
    }
    
    public void handleElement(Element e) throws ServiceException {
    	mResponse.addElement(e.detach());
    }
    
    public void handleDeleted(ItemId id) {
    	mResponse.addElement(MailConstants.E_DELETED).addAttribute(MailConstants.A_ID, id.toString());
    }
    
    protected HashMap<String,Object> parseContactElement(Element e) {
    	HashMap<String,Object> map = new HashMap<String,Object>();
    	Iterator<Element> iter = e.elementIterator(MailConstants.E_ATTRIBUTE);
    	while (iter.hasNext()) {
    		Element elem = iter.next();
    		String key = elem.getAttribute(MailConstants.A_ATTRIBUTE_NAME, null);
    		String value = elem.getText();
    		if (key == null)
    			continue;
    		Object obj = map.get(key);
    		if (obj != null) {
    			if (obj instanceof String) {
    				String[] str = new String[2];
    				str[0] = (String)obj;
    				str[1] = value;
    				map.put(key, str);
    			} else if (obj instanceof String[]) {
    				ArrayList<String> arr = new ArrayList<String>();
    				arr.addAll(Arrays.asList((String[])obj));
    				arr.add(value);
    				map.put(key, arr.toArray(new String[0]));
    			}
    		} else {
    			map.put(key, value);
    		}
    	}
    	return map;
    }
    public void setNewToken(String newToken) {
    	setNewToken(new GalSyncToken(newToken));
    }
    public void setNewToken(GalSyncToken newToken) {
    	String oldToken = mResponse.getAttribute(MailConstants.A_TOKEN, null);
    	if (oldToken != null)
    		newToken.merge(new GalSyncToken(oldToken));
    	mResponse.addAttribute(MailConstants.A_TOKEN, newToken.toString());
    }
    public void setSortBy(String sortBy) {
        mResponse.addAttribute(MailConstants.A_SORTBY, sortBy);
    }
    public void setQueryOffset(int offset) {
        mResponse.addAttribute(MailConstants.A_QUERY_OFFSET, offset);
    }
    public void setHasMoreResult(boolean more) {
        mResponse.addAttribute(MailConstants.A_QUERY_MORE, more);
    }
}
