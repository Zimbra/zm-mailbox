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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.util.DateUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol.Compliance;
import com.zimbra.cs.dav.property.ResourceProperty;
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
	protected Map<QName,ResourceProperty> mProps;
	protected List<Compliance> mDavCompliance;
	
	public DavResource(String uri, Account acct) throws ServiceException {
		this(uri, getOwner(acct));
	}
	
	public DavResource(String uri, String owner) {
		mOwner = owner;
		mProps = new HashMap<QName,ResourceProperty>();
		mUri = uri;
		mDavCompliance = new ArrayList<Compliance>();
		mDavCompliance.add(Compliance.one);
		mDavCompliance.add(Compliance.two);
		//mDavCompliance.add(Compliance.three);
		//mDavCompliance.add(Compliance.access_control);
		//mDavCompliance.add(Compliance.update);
		//mDavCompliance.add(Compliance.binding);
		
		ResourceProperty rs = new ResourceProperty(DavElements.E_RESOURCETYPE);
		if (isCollection())
			rs.addChild(DavElements.E_COLLECTION);
		addProperty(rs);
	}
	
	protected static String getOwner(Account acct) throws ServiceException {
		String owner = acct.getName();
		Provisioning prov = Provisioning.getInstance();
        Config config = prov.getConfig();
        String defaultDomain = config.getAttr(Provisioning.A_zimbraDefaultDomainName, null);
        if (defaultDomain != null && defaultDomain.equalsIgnoreCase(acct.getDomainName()))
        	owner = owner.substring(0, owner.indexOf('@'));
        return owner;
	}
	
	public boolean equals(Object another) {
		if (another instanceof DavResource) {
			DavResource that = (DavResource) another;
			return this.mUri.equals(that.mUri) && this.mOwner.equals(that.mOwner);
		}
		return false;
	}
	
	public List<Compliance> getComplianceList() {
		return mDavCompliance;
	}
	
	public ResourceProperty getProperty(String propName) {
		return getProperty(QName.get(propName, DavElements.WEBDAV_NS));
	}
	
	public ResourceProperty getProperty(QName prop) {
		return mProps.get(prop);
	}

	public Set<QName> getAllPropertyNames() {
		HashSet<QName> ret = new HashSet<QName>();
		for (QName key : mProps.keySet())
			if (!mProps.get(key).isProtected())
				ret.add(key);
		
		return ret;
	}

	public Element addPropertyElement(DavContext ctxt, Element parent, QName propName, boolean nameOnly) {
		Element e = null;
		
		ResourceProperty prop = mProps.get(propName);
		if (prop != null) {
			e = prop.toElement(ctxt, parent, nameOnly);
			return e;
		}
		
		// check ACL and add LockDiscovery
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
		ResourceProperty prop = getProperty(DavElements.E_GETCONTENTTYPE);
		if (prop != null)
			return prop.getStringValue();
		return null;
	}
	
	public int getContentLength() {
		ResourceProperty prop = getProperty(DavElements.E_GETCONTENTLENGTH);
		if (prop != null)
			return Integer.parseInt(prop.getStringValue());
		return 0;
	}
	
	protected void setCreationDate(long ts) {
		setProperty(DavElements.P_CREATIONDATE, DateUtil.toISO8601(new Date(ts)));
	}
	
	protected void setLastModifiedDate(long ts) {
		setProperty(DavElements.P_GETLASTMODIFIED, DateUtil.toRFC822Date(new Date(ts)));
	}
	
	protected void addProperty(ResourceProperty prop) {
		mProps.put(prop.getName(), prop);
	}
	
	protected void addProperties(Set<ResourceProperty> props) {
		for (ResourceProperty p : props)
			mProps.put(p.getName(), p);
	}
	
	protected void setProperty(String key, String val) {
		setProperty(QName.get(key, DavElements.WEBDAV_NS), val);
	}
	
	protected void setProperty(QName key, String val) {
		setProperty(key, val, false);
	}
	
	protected void setProperty(QName key, String val, boolean isProtected) {
		ResourceProperty prop = mProps.get(key);
		if (prop == null) {
			prop = new ResourceProperty(key);
			mProps.put(key, prop);
		}
		prop.setProtected(isProtected);
		prop.setStringValue(val);
	}
	
	public Element addResourceTypeElement(Element parent, boolean nameOnly) {
		Element rs = parent.addElement(DavElements.E_RESOURCETYPE);
		if (nameOnly)
			return rs;
		if (isCollection())
			rs.addElement(DavElements.E_COLLECTION);
		return rs;
	}

	public Element addHref(Element parent, boolean nameOnly) throws DavException {
		Element href = parent.addElement(DavElements.E_HREF);
		if (nameOnly)
			return href;
		href.setText(UrlNamespace.getResourceUrl(this));
		return href;
	}
	/*
	 * whether the resource is access controlled as in RFC3744.
	 */
	public boolean isAccessControlled() {
		return true;
	}
	
	public abstract InputStream getContent() throws IOException, DavException;
	
	public abstract boolean isCollection();
	
	public Collection<DavResource> getChildren(DavContext ctxt) throws DavException {
		return Collections.emptyList();
	}
}
