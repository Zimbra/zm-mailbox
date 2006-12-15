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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;

/**
 * @author schemers
 */
public class DocumentDispatcher {

    private static final String REQUEST_SUFFIX = "Request";
    private static final String RESPONSE_SUFFIX = "Response";

	Map<QName, DocumentHandler> mHandlers;
    Map<QName, QName> mResponses;
	
	public DocumentDispatcher() {
		mHandlers = new HashMap<QName, DocumentHandler>();
        mResponses = new HashMap<QName, QName>();
	}
	
	public void registerHandler(QName qname, DocumentHandler handler) {
		mHandlers.put(qname, handler);
        QName respQName = mResponses.get(qname);
        if (respQName == null) {
            String reqName = qname.getName();
            String respName;
            if (reqName.endsWith(REQUEST_SUFFIX))
                respName =
                    reqName.substring(0, reqName.length() - REQUEST_SUFFIX.length())
                    + RESPONSE_SUFFIX;
            else
                respName = reqName + RESPONSE_SUFFIX;
            respQName = new QName(respName, qname.getNamespace());
            mResponses.put(qname, respQName);
        }
        handler.setResponseQName(respQName);
	}
	
	public DocumentHandler getHandler(Element doc) {
		DocumentHandler handler = mHandlers.get(doc.getQName());
		return handler;
	}

	public Element dispatch(Element doc, Map<String, Object> context) throws ServiceException, SoapFaultException {
		DocumentHandler handler = mHandlers.get(doc.getQName());
		if (handler == null) 
			throw ServiceException.UNKNOWN_DOCUMENT(doc.getQualifiedName(), null);
		return handler.handle(doc, context);
	}
}
