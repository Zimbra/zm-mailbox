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

package com.zimbra.cs.account.ldap.legacy.entry;

import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.entry.LdapEntry;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapUtil;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

/**
 * @author schemers
 */
public class LdapDataSource extends DataSource implements LdapEntry {

	public static String getObjectClass(DataSourceType type) {
		switch (type) {
		case pop3: return "zimbraPop3DataSource";
		case imap: return "zimbraImapDataSource";
		case rss:  return "zimbraRssDataSource";
		case gal:  return "zimbraGalDataSource";
		default: return null;
		}
	}

	static DataSourceType getObjectType(Attributes attrs) throws ServiceException {
		try {
			String dsType = LegacyLdapUtil.getAttrString(attrs, Provisioning.A_zimbraDataSourceType);
			if (dsType != null)
				return DataSourceType.fromString(dsType);
		} catch (NamingException e) {
			ZimbraLog.datasource.error("cannot get DataSource type", e);
		}
		Attribute attr = attrs.get("objectclass");
		if (attr.contains("zimbraPop3DataSource")) 
			return DataSourceType.pop3;
		else if (attr.contains("zimbraImapDataSource"))
			return DataSourceType.imap;
		else if (attr.contains("zimbraRssDataSource"))
		    return DataSourceType.rss;
		else if (attr.contains("zimbraGalDataSource"))
            return DataSourceType.gal;
		else
			throw ServiceException.FAILURE("unable to determine data source type from object class", null);
	}

	private String mDn;

	public LdapDataSource(Account acct, String dn, Attributes attrs, Provisioning prov) throws NamingException, ServiceException {
		super(acct, 
				getObjectType(attrs),
				LegacyLdapUtil.getAttrString(attrs, Provisioning.A_zimbraDataSourceName),
				LegacyLdapUtil.getAttrString(attrs, Provisioning.A_zimbraDataSourceId),                
				LegacyLdapUtil.getAttrs(attrs), prov);
		mDn = dn;
	}
	public String getDN() {
		return mDn;
	}
}
