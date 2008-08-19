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

package com.zimbra.cs.account.ldap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

/**
 * @author schemers
 */
class LdapDataSource extends DataSource implements LdapEntry {

	static String getObjectClass(Type type) {
		switch (type) {
		case pop3: return "zimbraPop3DataSource";
		case imap: return "zimbraImapDataSource";
		default: return null;
		}
	}

	static Type getObjectType(Attributes attrs) throws ServiceException {
		try {
			String dsType = LdapUtil.getAttrString(attrs, Provisioning.A_zimbraDataSourceType);
			if (dsType != null)
				return Type.fromString(dsType);
		} catch (NamingException e) {
			ZimbraLog.datasource.error("cannot get DataSource type", e);
		}
		Attribute attr = attrs.get("objectclass");
		if (attr.contains("zimbraPop3DataSource")) 
			return Type.pop3;
		else if (attr.contains("zimbraImapDataSource"))
			return Type.imap;
		else
			throw ServiceException.FAILURE("unable to determine data source type from object class", null);
	}

	private String mDn;

	LdapDataSource(Account acct, String dn, Attributes attrs) throws NamingException, ServiceException {
		super(acct, 
				getObjectType(attrs),
				LdapUtil.getAttrString(attrs, Provisioning.A_zimbraDataSourceName),
				LdapUtil.getAttrString(attrs, Provisioning.A_zimbraDataSourceId),                
				LdapUtil.getAttrs(attrs));
		mDn = dn;
	}

	public String getDN() {
		return mDn;
	}
}
