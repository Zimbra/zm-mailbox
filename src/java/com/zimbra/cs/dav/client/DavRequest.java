/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
package com.zimbra.cs.dav.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.zimbra.cs.dav.DavContext.Depth;
import com.zimbra.cs.dav.DavElements;

public class DavRequest {
	
	private DavRequest(String uri, String method) {
		mUri = uri;
		mMethod = method;
		mDepth = Depth.zero;
	}

	public Document getRequestMessage() {
		return mDoc;
	}
	
	public void setRequestMessage(Element root) {
		if (mDoc == null)
			mDoc = DocumentHelper.createDocument();
		mDoc.setRootElement(root);
	}
	
	public void addRequestProp(QName p) {
		if (mDoc == null)
			return;
		Element prop = mDoc.getRootElement().element(DavElements.E_PROP);
		if (prop == null)
			return;
		prop.addElement(p);
	}
	
	public void addHref(String href) {
		if (mDoc == null)
			return;
		Element el = mDoc.getRootElement().addElement(DavElements.E_HREF);
		el.setText(href);
	}
	
	public void addRequestElement(Element e) {
		if (mDoc == null)
			return;
		mDoc.getRootElement().add(e);
	}
	public void setDepth(Depth d) {
		mDepth = d;
	}
	
	public Depth getDepth() {
		return mDepth;
	}
	
	public String getUri() {
		return mUri;
	}
	public String getMethod() {
		return mMethod;
	}
	public String getRequestMessageString() throws IOException {
		if (mDoc != null) {
			OutputFormat format = OutputFormat.createPrettyPrint();
			format.setTrimText(false);
			format.setOmitEncoding(false);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			XMLWriter writer = new XMLWriter(out, format);
			writer.write(mDoc);
			return new String(out.toByteArray(), "UTF-8");
		}
		return "";
	}
	
	public static class DocumentRequestEntity implements RequestEntity {
		private Document doc;
		private byte[] buffer;
		public DocumentRequestEntity(Document d) { doc = d; buffer = null; }
		public boolean isRepeatable() { return true; }
	    public long getContentLength() {
	    	if (buffer == null)
	    		try {
		    		getContents();
	    		} catch (Exception e) {
	    		}
	    	if (buffer == null) return -1;
	    	return buffer.length;
	    }
	    public String getContentType() { return "text/xml"; }
	    public void writeRequest(OutputStream out) throws IOException {
	    	if (buffer != null) {
	    		out.write(buffer);
	    		return;
	    	}
			OutputFormat format = OutputFormat.createCompactFormat();
			format.setTrimText(false);
			format.setOmitEncoding(false);
			XMLWriter writer = new XMLWriter(out, format);
			writer.write(doc);
	    }
	    private void getContents() throws IOException {
	    	ByteArrayOutputStream out = new ByteArrayOutputStream();
	    	writeRequest(out);
			buffer = out.toByteArray();
	    }
	}
	
	public HttpMethod getHttpMethod(String baseUrl) {
		if (mDoc == null)
			return new GetMethod(baseUrl + mUri) {
	    		public String getName() {
	    			return mMethod;
	    		}
			};
		
    	PutMethod m = new PutMethod(baseUrl + mUri) {
    		RequestEntity re;
    		public String getName() {
    			return mMethod;
    		}
    	    protected RequestEntity generateRequestEntity() {
    	    	return re;
    	    }
    	    public void setRequestEntity(RequestEntity requestEntity) {
    	    	re = requestEntity;
    	    	super.setRequestEntity(requestEntity);
    	    }
    	};
		DocumentRequestEntity re = new DocumentRequestEntity(mDoc);
		m.setRequestEntity(re);
    	return m;
	}
	
	private Document mDoc;
	private String mUri;
	private String mMethod;
	private Depth mDepth;
	
	private static final String PROPFIND = "PROPFIND";
	private static final String REPORT = "REPORT";
	private static final String DELETE = "DELETE";
	private static final String MKCOL = "MKCOL";
	private static final String MKCALENDAR = "MKCALENDAR";
	private static final String PROPPATCH = "PROPPATCH";
	private static final String OPTION = "OPTION";
	
	public static DavRequest PROPFIND(String uri) {
		DavRequest req = new DavRequest(uri, PROPFIND);
    	Element root = DocumentHelper.createElement(DavElements.E_PROPFIND);
    	root.addElement(DavElements.E_PROP);
    	req.setRequestMessage(root);
		return req;
	}
	
	public static DavRequest CALENDARMULTIGET(String uri) {
		DavRequest req = new DavRequest(uri, REPORT);
    	Element root = DocumentHelper.createElement(DavElements.E_CALENDAR_MULTIGET);
    	root.addElement(DavElements.E_PROP);
    	req.setRequestMessage(root);
		return req;
	}
	
	public static DavRequest CALENDARQUERY(String uri) {
		DavRequest req = new DavRequest(uri, REPORT);
    	Element root = DocumentHelper.createElement(DavElements.E_CALENDAR_QUERY);
    	root.addElement(DavElements.E_PROP);
    	root.addElement(DavElements.E_FILTER).addElement(DavElements.E_COMP_FILTER).addAttribute(DavElements.P_NAME, "VCALENDAR");
    	req.setRequestMessage(root);
		return req;
	}
	
	public static DavRequest DELETE(String uri) {
		DavRequest req = new DavRequest(uri, DELETE);
		return req;
	}

	public static DavRequest MKCOL(String uri) {
		DavRequest req = new DavRequest(uri, MKCOL);
		return req;
	}

	public static DavRequest MKCALENDAR(String uri) {
		DavRequest req = new DavRequest(uri, MKCALENDAR);
		return req;
	}

	public static DavRequest PROPPATCH(String uri) {
		DavRequest req = new DavRequest(uri, PROPPATCH);
    	Element root = DocumentHelper.createElement(DavElements.E_PROPERTYUPDATE);
    	root.addElement(DavElements.E_SET).addElement(DavElements.E_PROP);
    	req.setRequestMessage(root);
		return req;
	}
	
	public static DavRequest OPTION(String uri) {
		DavRequest req = new DavRequest(uri, OPTION);
		return req;
	}
}
