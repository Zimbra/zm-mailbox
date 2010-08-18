/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.DavContext.RequestProp;
import com.zimbra.cs.dav.DavProtocol.Compliance;
import com.zimbra.cs.dav.property.Acl;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.httpclient.URLUtil;

/**
 * DavResource is an object identified by a URL in the hierarchy of HTTP URL
 * namespace as described in section 5 of RFC2518.
 * 
 * @author jylee
 *
 */
public abstract class DavResource {
	
	public static class InvalidResource extends DavResource {
		public InvalidResource(String uri, String owner) {
			super(uri, owner);
		}
		public void delete(DavContext ctxt) { }
		public boolean isCollection() { return false; }
		public boolean isValid() { return false; }
	}
	
	protected String mUri;
	protected String mOwner;
	protected Map<QName,ResourceProperty> mProps;
	protected Collection<Compliance> mDavCompliance;
	protected boolean mNewlyCreated;
	
	public DavResource(String uri, String owner) {
		mNewlyCreated = false;
		mOwner = owner;
		mProps = new HashMap<QName,ResourceProperty>();
		mUri = uri;
		if (isCollection() && !mUri.endsWith("/"))
            mUri = mUri + "/";
		mDavCompliance = new TreeSet<Compliance>();
		mDavCompliance.add(Compliance.one);
		mDavCompliance.add(Compliance.two);
		mDavCompliance.add(Compliance.three);
		mDavCompliance.add(Compliance.access_control);
		mDavCompliance.add(Compliance.calendar_access);
		mDavCompliance.add(Compliance.version_control);
		mDavCompliance.add(Compliance.calendar_proxy);
		mDavCompliance.add(Compliance.calendarserver_principal_property_search);
        mDavCompliance.add(Compliance.addressbook);
        mDavCompliance.add(Compliance.extended_mkcol);
		
		ResourceProperty rtype = new ResourceProperty(DavElements.E_RESOURCETYPE);
		rtype.setProtected(true);
		rtype.setVisible(true);
		addProperty(rtype);

		ResourceProperty href = new ResourceProperty(DavElements.E_HREF);
		href.setProtected(true);
		try {
			href.setStringValue(UrlNamespace.getResourceUrl(this));
		} catch (Exception e) {
			ZimbraLog.dav.error("can't generate href", e);
		}
		addProperty(href);
		if (hasEtag())
			setProperty(DavElements.E_GETETAG, getEtag(), true);
		if (isCollection())
			addResourceType(DavElements.E_COLLECTION);
		addProperty(Acl.getPrincipalCollectionSet());
		addProperty(Acl.getCurrentUserPrincipal());
		QName[] supportedReports = getSupportedReports();
		if (supportedReports.length > 0) {
		    ResourceProperty sr = new ResourceProperty(DavElements.E_SUPPORTED_REPORT_SET);
		    addProperty(sr);
		    sr.setProtected(true);
            sr.setVisible(true);
            Element e = null;
            for (QName n : supportedReports) {
                e = org.dom4j.DocumentHelper.createElement(DavElements.E_SUPPORTED_REPORT);
                e.addElement(DavElements.E_REPORT).addElement(n);
                sr.getChildren().add(e);
            }
		}
	}
	
	public void setHref(String href) {
		getProperty(DavElements.E_HREF).setStringValue(URLUtil.urlEscape(href));
	}
	
	public boolean isValid() {
		return true;
	}
	
	public boolean isNewlyCreated() {
		return mNewlyCreated;
	}
	
	public boolean equals(Object another) {
		if (another instanceof DavResource) {
			DavResource that = (DavResource) another;
			return this.mUri.equals(that.mUri) && this.mOwner.equals(that.mOwner);
		}
		return false;
	}
	
	public Collection<Compliance> getComplianceList() {
		return mDavCompliance;
	}
	
	public ResourceProperty getProperty(String propName) {
		return getProperty(QName.get(propName, DavElements.WEBDAV_NS));
	}
	
	public ResourceProperty getProperty(QName prop) {
		return mProps.get(prop);
	}
	
	public ResourceProperty getProperty(Element prop) {
	    if (prop == null)
	        return null;
	    return getProperty(prop.getQName());
	}
	
	public ResourceProperty getProperty(QName prop, RequestProp request) {
	    if (request.getProp(prop) == null)
	        return getProperty(prop);
	    return getProperty(request.getProp(prop));
	}

	public Set<QName> getAllPropertyNames() {
		Set<QName> ret = new HashSet<QName>();
		for (QName key : mProps.keySet())
			if (mProps.get(key).isVisible())
				ret.add(key);
		
		return ret;
	}

	public String getUri() {
		return mUri;
	}

	public String getOwner() {
		return mOwner;
	}
	
	public boolean hasContent(DavContext ctxt) {
		try {
			return (getContentLength() > 0);
		} catch (NumberFormatException e) {
		}
		return false;
	}
	
	public String getContentType(DavContext ctxt) {
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
	
	/*
	 * whether the resource is access controlled as in RFC3744.
	 */
	public boolean isAccessControlled() {
		return true;
	}
	
    public InputStream getContent(DavContext ctxt) throws IOException, DavException {
        return new ByteArrayInputStream(getTextContent(ctxt).getBytes("UTF-8"));
    }
	
	public abstract boolean isCollection();
	
	public abstract void delete(DavContext ctxt) throws DavException;
	
	public String getName() {
	    return getUri();
	}
	
	public Collection<DavResource> getChildren(DavContext ctxt) throws DavException {
		return Collections.emptyList();
	}
	
	public boolean hasEtag() {
		return false;
	}
	public String getEtag() {
		return null;
	}
	
	public String getLastModifiedDate() {
        ResourceProperty rp = getProperty(DavElements.P_GETLASTMODIFIED);
        if (rp != null)
            return rp.getStringValue();
	    return new Date(0).toString();
	}
	
	public void patchProperties(DavContext ctxt, Collection<Element> set, Collection<QName> remove) throws DavException, IOException {
		throw new DavException("PROPPATCH not supported on "+getUri(), DavProtocol.STATUS_FAILED_DEPENDENCY, null);
	}
	
	protected void addResourceType(QName type) {
		ResourceProperty rtype = getProperty(DavElements.E_RESOURCETYPE);
		rtype.addChild(type);
	}
	
	public void handlePost(DavContext ctxt) throws DavException, IOException, ServiceException {
		throw new DavException("the resource does not handle POST", HttpServletResponse.SC_FORBIDDEN);
	}
	
	public boolean isLocal() {
		return true;
	}
	
	public String toString() {
		return mUri;
	}
	public DavResource copy(DavContext ctxt, DavResource dest) throws DavException {
		throw new DavException("not supported", HttpServletResponse.SC_NOT_ACCEPTABLE);
	}
	public void rename(DavContext ctxt, String newName, DavResource destCollection) throws DavException {
		throw new DavException("not supported", HttpServletResponse.SC_NOT_ACCEPTABLE);
	}

    protected QName[] getSupportedReports() {
        return new QName[0];
    }
    
    protected String getTextContent(DavContext ctxt) throws IOException {
        StringBuilder buf = new StringBuilder();
        buf.append("Request\n\n");
        buf.append("\tAuthenticated user:\t").append(ctxt.getAuthAccount().getName()).append("\n");
        buf.append("\tCurrent date:\t\t").append(new Date(System.currentTimeMillis())).append("\n");
        buf.append("\nResource\n\n");
        buf.append("\tName:\t\t\t").append(getName()).append("\n");
        buf.append("\tPath:\t\t\t").append(getUri()).append("\n");
        buf.append("\tDate:\t\t\t").append(getLastModifiedDate()).append("\n");
        try {
            Provisioning prov = Provisioning.getInstance();
            Account account = prov.get(Provisioning.AccountBy.name, getOwner());
            buf.append("\tOwner account name:\t").append(account.getName()).append("\n");
        } catch (ServiceException se) {
        }
        buf.append("\nProperties\n\n");
        buf.append(getPropertiesAsText(ctxt));
        return buf.toString();
    }
    
    protected String getPropertiesAsText(DavContext ctxt) throws IOException {
        Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_PROP);
        for (ResourceProperty rp : mProps.values())
            rp.toElement(ctxt, e, false);
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setTrimText(false);
        format.setOmitEncoding(false);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLWriter writer = new XMLWriter(baos, format);
        writer.write(e);
        return new String(baos.toByteArray());
    }
}
