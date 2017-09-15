/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap.entry;

import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.IAttributes.CheckBinary;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.soap.admin.type.DataSourceType;

/**
 * 
 * @author pshao
 *
 */
public class LdapDataSource extends DataSource implements LdapEntry {

	private String mDn;

	public LdapDataSource(Account acct, String dn, ZAttributes attrs, Provisioning prov) 
	throws LdapException, ServiceException {
		super(acct, getObjectType(attrs),
		        attrs.getAttrString(Provisioning.A_zimbraDataSourceName),
		        attrs.getAttrString(Provisioning.A_zimbraDataSourceId),                
		        attrs.getAttrs(), 
		        prov);
		mDn = dn;
	}
	
	public String getDN() {
		return mDn;
	}

    public static String getObjectClass(DataSourceType type) {
        switch (type) {
            case pop3:
                return AttributeClass.OC_zimbraPop3DataSource;
            case imap:
                return AttributeClass.OC_zimbraImapDataSource;
            case rss:
                return AttributeClass.OC_zimbraRssDataSource;
            case gal:
                return AttributeClass.OC_zimbraGalDataSource;
            case custom:
                return AttributeClass.OC_zimbraDataSource;
            default: 
                return null;
        }
    }

    static DataSourceType getObjectType(ZAttributes attrs) throws ServiceException {
        try {
            String dsType = attrs.getAttrString(Provisioning.A_zimbraDataSourceType);
            if (dsType != null)
                return DataSourceType.fromString(dsType);
        } catch (LdapException e) {
            ZimbraLog.datasource.error("cannot get DataSource type", e);
        }
        
        List<String> attr = attrs.getMultiAttrStringAsList(Provisioning.A_objectClass, CheckBinary.NOCHECK);
        if (attr.contains(AttributeClass.OC_zimbraPop3DataSource)) 
            return DataSourceType.pop3;
        else if (attr.contains(AttributeClass.OC_zimbraImapDataSource))
            return DataSourceType.imap;
        else if (attr.contains(AttributeClass.OC_zimbraRssDataSource))
            return DataSourceType.rss;
        else if (attr.contains(AttributeClass.OC_zimbraGalDataSource))
            return DataSourceType.gal;
        else if (attr.contains(AttributeClass.OC_zimbraDataSource))
            return DataSourceType.custom;
        else
            throw ServiceException.FAILURE("unable to determine data source type from object class", null);
    }
}
