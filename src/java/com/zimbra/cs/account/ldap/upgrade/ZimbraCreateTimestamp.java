/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap.upgrade;

import java.io.IOException;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.InvalidSearchFilterException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapDIT;
import com.zimbra.cs.account.ldap.LdapEntry;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;

public class ZimbraCreateTimestamp extends LdapUpgrade {
    
    enum Type {
        account,
        alias,
        calendarresource,
        config,
        cos,
        datasource,
        distributionlist,
        domain,
        identity,
        server,
        signature,
        xmppcomponent,
        zimlet;
        
        public static Type fromString(String s) throws ServiceException {
            try {
                return Type.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown type: " + s, e);
            }
        }
    }
    
    private Type mType;

    ZimbraCreateTimestamp() throws ServiceException {
    }
    
    @Override
    boolean parseCommandLine(CommandLine cl) {
        String[] args = cl.getArgs();
        if (args.length == 1) {
            try {
                mType = Type.fromString(args[0]);
            } catch (ServiceException e) {
                LdapUpgrade.usage(null, this, "invalid type: " + args[0]);
                return false;
            }
        } else if (args.length != 0) {
            LdapUpgrade.usage(null, this, "invalid arg");
            return false;
        }
        return true;
    }
    
    @Override
    void usage(HelpFormatter helpFormatter) {
        System.out.println();
        System.out.println("args for bug " + mBug + ":");
        System.out.println("    [type]");
        System.out.println();
        System.out.println("type can be: (if omitted, it means all objects)");
        for (Type t : Type.values())
            System.out.println("    " + t.name());
        System.out.println();
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        // TODO Auto-generated method stub
        
        LdapDIT dit = mProv.getDIT();
        String base;
        String query;
        String returnAttrs[] = new String[] {Provisioning.A_objectClass,
                                             Provisioning.A_zimbraCreateTimestamp,
                                             "createTimestamp"};
        
        if (mType == null) {
            System.out.println("Checking all objects\n");
            
            base = dit.zimbraBaseDN();
            query = "(|" +
                     "(objectclass=zimbraAccount)" +
                     "(objectclass=zimbraAlias)" +
                     "(objectclass=zimbraCalendarResource)" +
                     "(objectclass=zimbraGlobalConfig)" +
                     "(objectclass=zimbraCOS)" +
                     "(objectclass=zimbraDataSource)" +
                     "(objectclass=zimbraDistributionList)" +
                     "(objectclass=zimbraDomain)" +
                     "(objectclass=zimbraIdentity)" +
                     "(objectclass=zimbraServer)" +
                     "(objectclass=zimbraSignature)" +
                     "(objectclass=zimbraXMPPComponent)" +
                     "(objectclass=zimbraZimletEntry)" +
                     ")";
        } else {
            System.out.println("Checking " + mType.name() + " objects...\n");
            
            switch (mType) {
            case account:
                base = dit.mailBranchBaseDN();
                query = "(&(objectclass=zimbraAccount)(!(objectclass=zimbraCalendarResource)))";
                break;
            case alias:
                base = dit.mailBranchBaseDN();
                query = "(objectclass=zimbraAlias)";
                break;
            case calendarresource:
                base = dit.mailBranchBaseDN();
                query = "(objectclass=zimbraCalendarResource)";
                break;
            case config:
                base = dit.configDN();
                query = "(objectclass=zimbraGlobalConfig)";
                break;
            case cos:
                base = dit.cosBaseDN();
                query = "(objectclass=zimbraCOS)";
                break;
            case datasource:
                base = dit.mailBranchBaseDN();
                query = "(objectclass=zimbraDataSource)";
                break;
            case distributionlist:
                base = dit.mailBranchBaseDN();
                query = "(objectclass=zimbraDistributionList)";
                break;
            case domain:
                base = dit.domainBaseDN();
                query = "(objectclass=zimbraDomain)";
                break;
            case identity:
                base = dit.mailBranchBaseDN();
                query = "(objectclass=zimbraIdentity)";
                break;
            case server:
                base = dit.serverBaseDN();
                query = "(objectclass=zimbraServer)";
                break;
            case signature:
                base = dit.mailBranchBaseDN();
                query = "(objectclass=zimbraSignature)";
                break;
            case xmppcomponent:
                base = dit.xmppcomponentBaseDN();
                query = "(objectclass=zimbraXMPPComponent)";
                break;
            case zimlet:
                base = dit.zimletBaseDN();
                query = "(objectclass=zimbraZimletEntry)";
                break;
            default:
                throw ServiceException.FAILURE("", null);    
            }
        }
        
        query = "(&" + "(!(zimbraCreateTimestamp=*))" + query + ")";
        
        int maxResults = 0; // no limit
        ZimbraLdapContext zlc = null; 
        ZimbraLdapContext modZlc = null;
        int numModified = 0;
        
        try {
            zlc = new ZimbraLdapContext(true, false);  // use master, do not use connection pool
            modZlc = new ZimbraLdapContext(true);
            
            SearchControls searchControls =
                new SearchControls(SearchControls.SUBTREE_SCOPE, maxResults, 0, returnAttrs, false, false);

            //Set the page size and initialize the cookie that we pass back in subsequent pages
            int pageSize = LdapUtil.adjustPageSize(maxResults, 1000);
            byte[] cookie = null;

            NamingEnumeration ne = null;

            
            try {
                do {
                    zlc.setPagedControl(pageSize, cookie, true);

                    ne = zlc.searchDir(base, query, searchControls);
                    while (ne != null && ne.hasMore()) {
                        SearchResult sr = (SearchResult) ne.nextElement();
                        String dn = sr.getNameInNamespace();

                        Attributes attrs = sr.getAttributes();
                        String createTime = LdapUtil.getAttrString(attrs, "createTimestamp");
                       
                        System.out.println(dn + " (" + createTime + ")");
                        
                        Attributes modAttrs = new BasicAttributes(true);
                        modAttrs.put("zimbraCreateTimestamp", createTime);
                        modZlc.replaceAttributes(dn, modAttrs);
                        
                        numModified++;
                    }
                    cookie = zlc.getCookie();
                } while (cookie != null);
            } finally {
                if (ne != null) ne.close();
            }
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to list all objects", e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("unable to list all objects", e);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
            ZimbraLdapContext.closeContext(modZlc);
            
            System.out.println("\nModified " + numModified + " objects");
        }
    }

}
