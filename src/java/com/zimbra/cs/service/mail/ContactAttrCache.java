/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

/*
 * Created on Jun 14, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.HashMap;

import com.zimbra.cs.service.Element;

/**
 * @author schemers
 */
public class ContactAttrCache {

	public int mId;
	public HashMap mCache;

	public ContactAttrCache() {
		mId = 0;
		mCache = new HashMap();
	}

	public Element makeAttr(Element parent, String name, String value) {
		
		if (value == null || value.equals(""))
		    return null;
		    
		Element e = parent.addElement(MailService.E_ATTRIBUTE);
		
		Long id = (Long) mCache.get(name);
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
