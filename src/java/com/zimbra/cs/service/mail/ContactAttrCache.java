/*
 * Created on Jun 14, 2004
 */
package com.liquidsys.coco.service.mail;

import java.util.HashMap;

import com.liquidsys.coco.service.Element;

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
