/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on May 26, 2004
 */
package com.zimbra.soap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import org.dom4j.QName;

import java.util.HashMap;
import java.util.Map;

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
