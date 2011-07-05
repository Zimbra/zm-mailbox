/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.ldap;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.gal.GalOp;

public enum LdapUsage {
    ADD_ALIAS,
    ADD_ALIAS_ACCOUNT,
    ADD_ALIAS_DL,
    AUTO_PROVISION,
    AUTO_PROVISION_ADMIN_SEARCH,
    CREATE_ACCOUNT,
    CREATE_COS,
    CREATE_DATASOURCE,
    CREATE_DISTRIBUTIONLIST,
    CREATE_DOMAIN,
    CREATE_IDENTITY,
    CREATE_SERVER,
    CREATE_SIGNATURE,
    CREATE_XMPPCOMPONENT,
    CREATE_ZIMLET,
    DELETE_ACCOUNT,
    DELETE_COS,
    DELETE_DATASOURCE,
    DELETE_DISTRIBUTIONLIST,
    DELETE_DOMAIN,
    DELETE_IDENTITY,
    DELETE_SERVER,
    DELETE_SIGNATURE,
    DELETE_XMPPCOMPONENT,
    DELETE_ZIMLET,
    LDAP_AUTH_EXTERNAL,
    LDAP_AUTH_ZIMBRA,
    GAL,
    GAL_AUTOCOMPLETE,
    GAL_SEARCH,
    GAL_SYNC,
    GAL_LEGACY,  // legacy GAL - calls not stemmed from the GAL SYNC account
    GAL_LEGACY_AUTOCOMPLETE,
    GAL_LEGACY_SEARCH,
    GAL_LEGACY_SYNC,
    GET_ENTRY,
    GET_SCHEMA,
    MODIFY_ENTRY,
    MODIFY_ACCOUNT,
    MODIFY_ALIAS,
    MODIFY_CALRESOURCE,
    MODIFY_COS,
    MODIFY_DATASOURCE,
    MODIFY_DISTRIBUTIONLIST,
    MODIFY_DOMAIN,
    MODIFY_GLOBALCONFIG,
    MODIFY_GLOBALGRANT,
    MODIFY_IDENTITY,
    MODIFY_MIMETYPE,
    MODIFY_SERVER,
    MODIFY_SIGNATURE,
    MODIFY_XMPPCOMPONENT,
    MODIFY_ZIMLET,
    PING,
    REMOVE_ALIAS,
    REMOVE_ALIAS_ACCOUNT,
    REMOVE_ALIAS_DL,
    RENAME_ACCOUNT,
    RENAME_COS,
    RENAME_DATASOURCE,
    RENAME_DISTRIBUTIONLIST,
    RENAME_IDENTITY,
    RENAME_DOMAIN,
    RENAME_SIGNATURE,
    RENAME_XMPPCOMPONENT,
    SEARCH, 
    SMIME_LOOKUP,
    NGINX_LOOKUP,
    UNITTEST,
    UPGRADE;
    
    public static LdapUsage modifyEntryfromEntryType(Entry.EntryType entryType) {
        switch (entryType) {
            case ACCOUNT: return MODIFY_ACCOUNT;
            case ALIAS: return MODIFY_ALIAS;
            case CALRESOURCE: return MODIFY_CALRESOURCE;
            case COS: return MODIFY_COS;
            case DATASOURCE: return MODIFY_DATASOURCE;
            case DISTRIBUTIONLIST: return MODIFY_DISTRIBUTIONLIST;
            case DOMAIN: return MODIFY_DOMAIN;
            case GLOBALCONFIG: return MODIFY_GLOBALCONFIG;
            case GLOBALGRANT: return MODIFY_GLOBALGRANT;
            case IDENTITY: return MODIFY_IDENTITY;
            case MIMETYPE: return MODIFY_MIMETYPE;
            case SERVER: return MODIFY_SERVER;
            case SIGNATURE: return MODIFY_SIGNATURE;
            case XMPPCOMPONENT: return MODIFY_XMPPCOMPONENT;
            case ZIMLET: return MODIFY_ZIMLET;
            default: return MODIFY_ENTRY;
        }
    }
    
    public static LdapUsage fromGalOp(GalOp galOp) {
        if (galOp == null) {
            ZimbraLog.ldap.warn("unknown GAL op");
            return LdapUsage.GAL;  // really an error
        }
        switch (galOp) {
            case autocomplete:
                return GAL_AUTOCOMPLETE;
            case search:
                return GAL_SEARCH;
            case sync: 
                return GAL_SYNC;
            default:
                ZimbraLog.ldap.warn("unknown GAL op");
                return GAL;
        }
    }
    
    public static LdapUsage fromGalOpLegacy(GalOp galOp) {
        if (galOp == null) {
            ZimbraLog.ldap.warn("unknown GAL op");
            return LdapUsage.GAL_LEGACY;  // really an error
        }
        switch (galOp) {
            case autocomplete:
                return GAL_LEGACY_AUTOCOMPLETE;
            case search:
                return GAL_LEGACY_SEARCH;
            case sync: 
                return GAL_LEGACY_SYNC;
            default:
                ZimbraLog.ldap.warn("unknown GAL op");
                return GAL_LEGACY;
        }
    }

}
