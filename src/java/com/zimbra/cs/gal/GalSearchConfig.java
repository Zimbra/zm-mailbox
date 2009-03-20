/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.gal;

import com.zimbra.cs.account.ldap.LdapGalMapRules;

public class GalSearchConfig {
	private String mUrl;
	private String mFilter;
	private String mSearchBase;
	private String mAuthMech;
	private String mBindDn;
	private String mBindPassword;
	private String mKerberosPrincipal;
	private String mKerberosKeytab;
	private LdapGalMapRules mRules;

	public String getUrl() {
		return mUrl;
	}
	public String getFilter() {
		return mFilter;
	}
	public String getSearchBase() {
		return mSearchBase;
	}
	public String getAuthType() {
		return mAuthMech;
	}
	public String getBindDN() {
		return mBindDn;
	}
	public String getPassword() {
		return mBindPassword;
	}
	public String getKerberos5Principal() {
		return mKerberosPrincipal;
	}
	public String getKerberos5Keytab() {
		return mKerberosKeytab;
	}
	public LdapGalMapRules getRules() {
		return mRules;
	}
}
