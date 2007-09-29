/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 14, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.HashMap;

import com.zimbra.soap.Element;

/**
 * @author schemers
 */
public class ContactAttrCache {

	public int mId;
	public HashMap<String, Long> mCache;

	public ContactAttrCache() {
		mId = 0;
		mCache = new HashMap<String, Long>();
	}

	public Element makeAttr(Element parent, String name, String value) {
		
		if (value == null || value.equals(""))
		    return null;
		    
		Element e = parent.addElement(MailService.E_ATTRIBUTE);
		
		Long id = mCache.get(name);
		if (id == null) {
		    id = new Long(mId++);
			mCache.put(name, id);
			e.addAttribute(MailService.A_ATTRIBUTE_NAME, name);
			e.addAttribute(MailService.A_ID, id.longValue());
		} else {
		    e.addAttribute(MailService.A_REF, id.longValue());
		}
		e.setText(value);
		return e;
	}
}
