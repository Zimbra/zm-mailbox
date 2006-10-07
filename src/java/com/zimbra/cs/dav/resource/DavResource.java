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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.util.DateUtil;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.soap.Element;

/**
 * DavResource is an object identified by a URL in the hierarchy of HTTP URL
 * namespace as described in section 5 of RFC2518.
 * 
 * @author jylee
 *
 */
public abstract class DavResource {
	protected String mUri;
	protected String mOwner;
	protected Map<String, String> mProps;
	
	public DavResource(String uri, String owner) {
		mProps = new HashMap<String,String>();
		mUri = uri;
		mOwner = owner;
	}
	
	public String getProperty(String propName) {
		return mProps.get(propName);
	}

	public Set<String> getAllPropertyNames() {
		return mProps.keySet();
	}
	
	public String getUri() {
		return mUri;
	}

	public String getOwner() {
		return mOwner;
	}
	
	public boolean hasContent() {
		String cl = mProps.get(DavElements.P_GETCONTENTLENGTH	);
		try {
			return (cl != null) && (Long.parseLong(cl) > 0);
		} catch (NumberFormatException e) {
		}
		return false;
	}
	
	public String getContentType() {
		return mProps.get(DavElements.P_GETCONTENTTYPE);
	}
	
	public int getContentLength() {
		String cl = mProps.get(DavElements.P_GETCONTENTLENGTH);
		if (cl == null)
			return 0;
		return Integer.parseInt(cl);
	}
	
	protected void setCreationDate(long ts) {
		mProps.put(DavElements.P_CREATIONDATE, DateUtil.toISO8601(new Date(ts)));
	}
	
	protected void setLastModifiedDate(long ts) {
		mProps.put(DavElements.P_GETLASTMODIFIED, DateUtil.toRFC822Date(new Date(ts)));
	}
	
	protected void setProperty(String key, String val) {
		mProps.put(key, val);
	}
	
	public Element getResourceTypeElement() {
		Element rs = new Element.XMLElement(DavElements.E_RESOURCETYPE);
		if (isCollection())
			rs.addElement(DavElements.E_COLLECTION);
		return rs;
	}
	
	public abstract InputStream getContent() throws IOException, DavException;
	
	public abstract boolean isCollection();
}
