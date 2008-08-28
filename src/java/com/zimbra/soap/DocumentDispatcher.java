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

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.ZimbraLog;

import org.dom4j.QName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * @author schemers
 */
public class DocumentDispatcher {

    private static final String REQUEST_SUFFIX = "Request";
    private static final String RESPONSE_SUFFIX = "Response";

	Map<QName, DocumentHandler> mHandlers;
    Map<QName, QName> mResponses;
    ArrayList<String> mExcludeList;
    
	public DocumentDispatcher() {
		mHandlers = new HashMap<QName, DocumentHandler>();
        mResponses = new HashMap<QName, QName>();
        String excludeString = LC.get("admin_soap_exclude_list");
        ZimbraLog.soap.info("Loading the exclude list");
        mExcludeList = new ArrayList<String>(Arrays.asList(excludeString.split(",")));
        Iterator <String> it = mExcludeList.iterator();
	}
	
	public void clearExcludeList() {
		ZimbraLog.soap.info("clearing the exclude list");
		mExcludeList.clear();
	}
	
	public void registerHandler(QName qname, DocumentHandler handler) {
		ZimbraLog.soap.info(String.format("Registering %s::%s", qname.getNamespaceURI(),qname.getQualifiedName()));
        if(!mExcludeList.isEmpty() && mExcludeList.contains(String.format("%s::%s", qname.getNamespaceURI(),qname.getQualifiedName()))) {
        	ZimbraLog.soap.info(String.format("skipping %s::%s", qname.getNamespaceURI(),qname.getQualifiedName()));
        	return;
        }
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
	
	
	public void unRegisterHandler(QName qname, DocumentHandler handler) {
		
		DocumentHandler qHandler = mHandlers.get(qname);
		if(qHandler!=null) {
			mHandlers.remove(qname);
		}
		
        QName respQName = mResponses.get(qname);
        if (respQName != null) {
            mResponses.remove(qname);
        }

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
