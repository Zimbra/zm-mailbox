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
package com.zimbra.cs.dav.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.XMLWriter;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.resource.DavResource;

/**
 * Base class for DAV methods.
 * 
 * @author jylee
 *
 */
public abstract class DavMethod {
	public abstract String getName();
	public abstract void handle(DavContext ctxt) throws DavException, IOException, ServiceException;
	
	public void checkPrecondition(DavContext ctxt) throws DavException {
	}
	
	public void checkPostcondition(DavContext ctxt) throws DavException {
	}
	
	public String toString() {
		return "DAV method " + getName();
	}
	
	public String getMethodName() {
		return getName();
	}
	
	protected static final int STATUS_OK = HttpServletResponse.SC_OK;
	
	protected void addComplianceHeader(DavContext ctxt, DavResource rs) throws IOException {
		HttpServletResponse resp = ctxt.getResponse();
		String comp = DavProtocol.getComplianceString(rs.getComplianceList());
		if (comp != null)
			resp.setHeader(DavProtocol.HEADER_DAV, comp);
	}
	
	protected void sendResponse(DavContext ctxt) throws IOException {
		if (ctxt.isResponseSent())
			return;
		HttpServletResponse resp = ctxt.getResponse();
		resp.setStatus(ctxt.getStatus());
		if (ctxt.hasResponseMessage()) {
			resp.setContentType(DavProtocol.DAV_CONTENT_TYPE);
			DavResponse respMsg = ctxt.getDavResponse();
			respMsg.writeTo(resp.getOutputStream());
		}
		ctxt.responseSent();
	}
	
	public HttpMethod toHttpMethod(DavContext ctxt, String targetUrl) throws IOException, DavException {
		if (ctxt.hasRequestMessage()) {
			PostMethod method = new PostMethod(targetUrl) {
				public String getName() { return getMethodName(); }
			};
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			XMLWriter writer = new XMLWriter(baos);
			writer.write(ctxt.getRequestMessage());
			ByteArrayRequestEntity reqEntry = new ByteArrayRequestEntity(baos.toByteArray());
			method.setRequestEntity(reqEntry);
			return method;
		}
    	return new GetMethod(targetUrl) {
    		public String getName() { return getMethodName(); }
    	};
	}
	
	protected RequestProp getRequestProp(DavContext ctxt) throws DavException {
		if (ctxt.hasRequestMessage()) {
			Document req = ctxt.getRequestMessage();
			return new RequestProp(req.getRootElement());
		}
		return sEmptyProp;
	}
	
	protected RequestProp getRequestProp(Collection<Element> set, Collection<QName> remove) {
		return new RequestProp(set, remove);
	}
	
	protected static RequestProp sEmptyProp;
	
	static {
		sEmptyProp = new RequestProp();
	}
	
	/* List of properties in the PROPFIND or PROPPATCH request. */
	public static class RequestProp {
		boolean nameOnly;
		boolean allProp;
		Collection<QName> props;
		HashMap<QName, DavException> errProps;

		public RequestProp() {
			props = new ArrayList<QName>();
			errProps = new HashMap<QName, DavException>();
			nameOnly = false;
			allProp = true;
		}
		
		public RequestProp(Element top) {
			this();
			
			nameOnly = false;
			allProp = false;
			for (Object obj : top.elements()) {
				if (!(obj instanceof Element))
					continue;
				Element e = (Element) obj;
				String name = e.getName();
				if (name.equals(DavElements.P_ALLPROP))
					allProp = true;
				else if (name.equals(DavElements.P_PROPNAME))
					nameOnly = true;
				else if (name.equals(DavElements.P_PROP)) {
					@SuppressWarnings("unchecked")
					List<Element> propElems = e.elements();
					for (Element prop : propElems)
						props.add(prop.getQName());
				}
			}
		}
		
		public RequestProp(Collection<Element> set, Collection<QName> remove) {
			this();
			allProp = false;
			for (Element e : set)
				props.add(e.getQName());
			props.addAll(remove);
		}
		
		public boolean isNameOnly() {
			return nameOnly;
		}
		public boolean isAllProp() {
			return allProp;
		}
		public void addProp(QName p) {
			props.add(p);
		}
		public Collection<QName> getProps() {
			return props;
		}
		public void addPropError(QName prop, DavException ex) {
			errProps.put(prop, ex);
		}
		public Map<QName, DavException> getErrProps() {
			return errProps;
		}
	}
}
