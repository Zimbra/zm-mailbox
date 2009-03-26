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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZAttrProvisioning.GalMode;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.gal.GalUtil;
import com.zimbra.cs.account.ldap.LdapGalMapRules;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.LdapUtil;

public class GalSearchConfig {
    
	public enum GalType {
	    zimbra,
	    ldap;
        
	    public static GalType fromString(String s) throws ServiceException {
		try {
		    return GalType.valueOf(s);
		} catch (IllegalArgumentException e) {
		    throw ServiceException.INVALID_REQUEST("unknown gal type: " + s, e);
		}
	    }
	}
    
	public static GalSearchConfig create(Account account, GalOp op, boolean external) throws ServiceException {
		if (external)
			return new LdapConfig(account, op);
		else
			return new ZimbraConfig(account, op);
	}
	
	public static GalSearchConfig create(DataSource ds) throws ServiceException {
		return new DataSourceConfig(ds);
	}

	private static class ZimbraConfig extends GalSearchConfig {
		public ZimbraConfig(Account account, GalOp op) throws ServiceException {
			loadZimbraConfig(account, op);
		}
	}
	private static class LdapConfig extends GalSearchConfig {
		public LdapConfig(Account account, GalOp op) throws ServiceException {
			loadConfig(account, op);
		}
	}
	
	private static class DataSourceConfig extends GalSearchConfig {
		public DataSourceConfig(DataSource ds) throws ServiceException {
			mGalType = GalType.fromString(ds.getAttr(Provisioning.A_zimbraGalType));
			if (mGalType == GalType.zimbra)
				loadZimbraConfig(ds.getAccount(), GalOp.sync);
			else {
				loadConfig(ds.getAccount(), GalOp.search);
				String[] url = ds.getMultiAttr(Provisioning.A_zimbraGalSyncLdapURL);
				if (url != null && url.length > 0)
					mUrl = url;
				mFilter = ds.getAttr(Provisioning.A_zimbraGalSyncLdapFilter, mFilter);
				mSearchBase = ds.getAttr(Provisioning.A_zimbraGalSyncLdapSearchBase, mSearchBase);
				mStartTlsEnabled = ds.getBooleanAttr(Provisioning.A_zimbraGalSyncLdapStartTlsEnabled, mStartTlsEnabled);
				mAuthMech = ds.getAttr(Provisioning.A_zimbraGalSyncLdapAuthMech, mAuthMech);
				mBindDn = ds.getAttr(Provisioning.A_zimbraGalSyncLdapBindDn, mBindDn);
				mBindPassword = ds.getAttr(Provisioning.A_zimbraGalSyncLdapBindPassword, mBindPassword);
				mKerberosPrincipal = ds.getAttr(Provisioning.A_zimbraGalSyncLdapKerberos5Principal, mKerberosPrincipal);
				mKerberosKeytab = ds.getAttr(Provisioning.A_zimbraGalSyncLdapKerberos5Keytab, mKerberosKeytab);
			}
			String[] attrs = ds.getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap);
			if (attrs.length > 0)
				mRules = new LdapGalMapRules(attrs);

			mFilter = GalUtil.expandFilter(null, mFilter, "", null, false);
		}
	}
	
	protected void loadZimbraConfig(Account account, GalOp op) throws ServiceException {
		Provisioning prov = Provisioning.getInstance();
		Domain domain = prov.getDomain(account);
		
        mRules = new LdapGalMapRules(domain.getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap));
        mOp = op;
        
		switch (op) {
		case sync:
			mFilter = LdapProvisioning.getFilterDef("zimbraAccountSync");
			break;
		case search:
			mFilter = LdapProvisioning.getFilterDef("zimbraAccounts");
			mTokenizeKey = domain.getAttr(Provisioning.A_zimbraGalTokenizeSearchKey, null);
			break;
		case autocomplete:
			mFilter = LdapProvisioning.getFilterDef("zimbraAccountAutocomplete");
			mTokenizeKey = domain.getAttr(Provisioning.A_zimbraGalTokenizeAutoCompleteKey, null);
			break;
		}
		mAuthMech = Provisioning.LDAP_AM_SIMPLE;
		mFilter = "(&("+mFilter+")(!(zimbraHideInGal=TRUE))(!(zimbraIsSystemResource=TRUE)))";
		mSearchBase = LdapUtil.getZimbraSearchBase(domain, op);
	}
	
	protected void loadConfig(Account account, GalOp op) throws ServiceException {
		Provisioning prov = Provisioning.getInstance();
		Domain domain = prov.getDomain(account);
		
        mRules = new LdapGalMapRules(domain.getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap));
        mOp = op;
        
        GalMode galMode = domain.getGalMode();
        if (galMode == GalMode.zimbra)
        	loadZimbraConfig(account, op);
		
        switch (op) {
        case sync:
        	mUrl = domain.getMultiAttr(Provisioning.A_zimbraGalSyncLdapURL);
            mFilter = domain.getAttr(Provisioning.A_zimbraGalLdapFilter);
            mSearchBase = domain.getAttr(Provisioning.A_zimbraGalSyncLdapSearchBase, "");
            mStartTlsEnabled = domain.getBooleanAttr(Provisioning.A_zimbraGalSyncLdapStartTlsEnabled, false);
            mAuthMech = domain.getAttr(Provisioning.A_zimbraGalSyncLdapAuthMech);
            mBindDn = domain.getAttr(Provisioning.A_zimbraGalSyncLdapBindDn);
            mBindPassword = domain.getAttr(Provisioning.A_zimbraGalSyncLdapBindPassword);
            mKerberosPrincipal = domain.getAttr(Provisioning.A_zimbraGalSyncLdapKerberos5Principal);
            mKerberosKeytab = domain.getAttr(Provisioning.A_zimbraGalSyncLdapKerberos5Keytab);
        	break;
        case search:
            mUrl = domain.getMultiAttr(Provisioning.A_zimbraGalLdapURL);
            mFilter = domain.getAttr(Provisioning.A_zimbraGalLdapFilter);
            mSearchBase = domain.getAttr(Provisioning.A_zimbraGalLdapSearchBase);
            mStartTlsEnabled = domain.getBooleanAttr(Provisioning.A_zimbraGalLdapStartTlsEnabled, false);
            mAuthMech = domain.getAttr(Provisioning.A_zimbraGalLdapAuthMech);
            mBindDn = domain.getAttr(Provisioning.A_zimbraGalLdapBindDn);
            mBindPassword = domain.getAttr(Provisioning.A_zimbraGalLdapBindPassword);
            mKerberosPrincipal = domain.getAttr(Provisioning.A_zimbraGalLdapKerberos5Principal);
            mKerberosKeytab = domain.getAttr(Provisioning.A_zimbraGalLdapKerberos5Keytab);
            mTokenizeKey = domain.getAttr(Provisioning.A_zimbraGalTokenizeSearchKey);
        	break;
        case autocomplete:
            mUrl = domain.getMultiAttr(Provisioning.A_zimbraGalLdapURL);
            mFilter = domain.getAttr(Provisioning.A_zimbraGalAutoCompleteLdapFilter);
            mSearchBase = domain.getAttr(Provisioning.A_zimbraGalLdapSearchBase);
            mStartTlsEnabled = domain.getBooleanAttr(Provisioning.A_zimbraGalLdapStartTlsEnabled, false);
            mAuthMech = domain.getAttr(Provisioning.A_zimbraGalLdapAuthMech);
            mBindDn = domain.getAttr(Provisioning.A_zimbraGalLdapBindDn);
            mBindPassword = domain.getAttr(Provisioning.A_zimbraGalLdapBindPassword);
            mKerberosPrincipal = domain.getAttr(Provisioning.A_zimbraGalLdapKerberos5Principal);
            mKerberosKeytab = domain.getAttr(Provisioning.A_zimbraGalLdapKerberos5Keytab);
            mTokenizeKey = domain.getAttr(Provisioning.A_zimbraGalTokenizeAutoCompleteKey);
        	break;
        }
        if (mFilter != null && mFilter.indexOf("(") == -1)
        	mFilter = LdapProvisioning.getFilterDef(mFilter);
	}
	
	protected GalOp mOp;
	protected GalType mGalType;
	protected String[] mUrl;
	protected boolean mStartTlsEnabled;
	protected String mFilter;
	protected String mSearchBase;
	protected String mAuthMech;
	protected String mBindDn;
	protected String mBindPassword;
	protected String mKerberosPrincipal;
	protected String mKerberosKeytab;
	protected String mTokenizeKey;
	protected LdapGalMapRules mRules;
	
	public GalOp getOp() {
		return mOp;
	}
	public GalType getGalType() {
		return mGalType;
	}
	public String[] getUrl() {
		return mUrl;
	}
	public boolean getStartTlsEnabled() {
		return mStartTlsEnabled;
	}
	public String getFilter() {
		return mFilter;
	}
	public String getSearchBase() {
		return mSearchBase;
	}
	public String getAuthMech() {
		return mAuthMech;
	}
	public String getBindDn() {
		return mBindDn;
	}
	public String getBindPassword() {
		return mBindPassword;
	}
	public String getKerberosPrincipal() {
		return mKerberosPrincipal;
	}
	public String getKerberosKeytab() {
		return mKerberosKeytab;
	}
	public LdapGalMapRules getRules() {
		return mRules;
	}
	public String getTokenizeKey() {
		return mTokenizeKey;
	}
	public void setGalType(GalType galType) {
		mGalType = galType;
	}
	public void setUrl(String[]  url) {
		mUrl = url;
	}
	public void setStartTlsEnabled(boolean startTlsEnabled) {
		mStartTlsEnabled = startTlsEnabled;
	}
	public void setFilter(String filter) {
		mFilter = filter;
	}
	public void setSearchBase(String searchBase) {
		mSearchBase = searchBase;
	}
	public void setAuthMech(String authMech) {
		mAuthMech = authMech;
	}
	public void setBindDn(String bindDn) {
		mBindDn = bindDn;
	}
	public void setBindPassword(String bindPassword) {
		mBindPassword = bindPassword;
	}
	public void setKerberosPrincipal(String kerberosPrincipal) {
		mKerberosPrincipal = kerberosPrincipal;
	}
	public void setKerberosKeytab(String kerberosKeytab) {
		mKerberosKeytab = kerberosKeytab;
	}
	public void setRules(LdapGalMapRules rules) {
		mRules = rules;
	}
	public void setTokenizeKey(String tokenizeKey) {
		mTokenizeKey = tokenizeKey;
	}
}
