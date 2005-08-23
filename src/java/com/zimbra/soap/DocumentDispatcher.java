/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.soap;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.QName;

import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 */
public class DocumentDispatcher {

	Map mHandlers;
	
	public DocumentDispatcher() {
		mHandlers = new HashMap();
	}
	
	public void registerHandler(QName qname, DocumentHandler handler) {
		mHandlers.put(qname, handler);
	}
	
	public DocumentHandler getHandler(Element doc) {
		DocumentHandler handler = (DocumentHandler) mHandlers.get(doc.getQName());
		return handler;
	}

	public Element dispatch(Element doc, Map context) throws ServiceException {
		DocumentHandler handler = (DocumentHandler) mHandlers.get(doc.getQName());
		if (handler == null) 
			throw ServiceException.UNKNOWN_DOCUMENT(doc.getQualifiedName(), null);
		return handler.handle(doc, context);
	}
}
