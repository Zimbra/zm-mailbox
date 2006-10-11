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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;

import com.zimbra.common.util.DateUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol.Compliance;
import com.zimbra.cs.service.ServiceException;

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
	protected Map<String,String> mProps;
	protected List<Compliance> mDavCompliance;
	
	public DavResource(String uri, Account acct) throws ServiceException {
		setOwner(acct);
		mProps = new HashMap<String,String>();
		mUri = uri;
		mDavCompliance = new ArrayList<Compliance>();
		mDavCompliance.add(Compliance.one);
		mDavCompliance.add(Compliance.two);
		//mDavCompliance.add(Compliance.three);
		//mDavCompliance.add(Compliance.access_control);
		//mDavCompliance.add(Compliance.update);
		//mDavCompliance.add(Compliance.binding);
	}
	
	protected void setOwner(Account acct) throws ServiceException {
		mOwner = acct.getName();
		Provisioning prov = Provisioning.getInstance();
        Config config = prov.getConfig();
        String defaultDomain = config.getAttr(Provisioning.A_zimbraDefaultDomainName, null);
        if (defaultDomain != null && defaultDomain.equalsIgnoreCase(acct.getDomainName()))
        	mOwner = mOwner.substring(0, mOwner.indexOf('@'));
	}
	
	public boolean equals(Object another) {
		if (another instanceof DavResource) {
			DavResource that = (DavResource) another;
			return this.mUri.equals(that.mUri) && this.mOwner.equals(that.mOwner);
		}
		return false;
	}
	
	public List<Compliance> getCompliance() {
		return mDavCompliance;
	}
	
	public String getProperty(String propName) {
		return mProps.get(propName);
	}

	public Set<String> getAllPropertyNames() {
		return mProps.keySet();
	}

	public Element addPropertyElement(Element parent, String propName, boolean nameOnly) {
		Element e = null;
		
		if (mProps.containsKey(propName)) {
			e = parent.addElement(propName);
			if (!nameOnly)
				e.setText(mProps.get(propName));
		}
		
		return e;
	}
	
	public String getUri() {
		return mUri;
	}

	public String getOwner() {
		return mOwner;
	}
	
	public boolean hasContent() {
		try {
			return (getContentLength() > 0);
		} catch (NumberFormatException e) {
		}
		return false;
	}
	
	public String getContentType() {
		return getProperty(DavElements.P_GETCONTENTTYPE);
	}
	
	public int getContentLength() {
		String cl = getProperty(DavElements.P_GETCONTENTLENGTH);
		if (cl == null)
			return 0;
		return Integer.parseInt(cl);
	}
	
	protected void setCreationDate(long ts) {
		setProperty(DavElements.P_CREATIONDATE, DateUtil.toISO8601(new Date(ts)));
	}
	
	protected void setLastModifiedDate(long ts) {
		setProperty(DavElements.P_GETLASTMODIFIED, DateUtil.toRFC822Date(new Date(ts)));
	}
	
	protected void setProperty(String key, String val) {
		mProps.put(key, val);
	}
	
	public Element addResourceTypeElement(Element parent, boolean nameOnly) {
		Element rs = parent.addElement(DavElements.E_RESOURCETYPE);
		if (nameOnly)
			return rs;
		if (isCollection())
			rs.addElement(DavElements.E_COLLECTION);
		return rs;
	}
	
	public abstract InputStream getContent() throws IOException, DavException;
	
	public abstract boolean isCollection();
}
