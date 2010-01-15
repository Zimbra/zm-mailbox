/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */package com.zimbra.cs.dav.property;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;

public class VersioningProperty extends ResourceProperty {
	public static ResourceProperty getSupportedReportSet() {
		return new SupportedReportSet();
	}
	
	protected VersioningProperty(QName name) {
		super(name);
		setProtected(true);
	}

	private static class SupportedReportSet extends VersioningProperty {
		private SupportedReportSet() {
			super(DavElements.E_SUPPORTED_REPORT_SET);
			Element e = null;
			for (QName n : SUPPORTED_REPORTS) {
				e = org.dom4j.DocumentHelper.createElement(DavElements.E_SUPPORTED_REPORT);
				e.addElement(DavElements.E_REPORT).addElement(n);
				mChildren.add(e);
			}
		}
		private QName[] SUPPORTED_REPORTS = {
				DavElements.E_ACL_PRINCIPAL_PROP_SET,
				DavElements.E_PRINCIPAL_MATCH,
				DavElements.E_PRINCIPAL_PROPERTY_SEARCH,
				DavElements.E_PRINCIPAL_SEARCH_PROPERTY_SET,
				DavElements.E_EXPAND_PROPERTY
		};
		public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
			return super.toElement(ctxt, parent, nameOnly);
		}
	}
}
