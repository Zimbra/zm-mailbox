/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;

public class BUG_76427 extends UpgradeOp {
    
    public static Set<String> standardZimlets = new HashSet<String>();
    
    static {
        standardZimlets.add("com_zimbra_adminversioncheck");
        standardZimlets.add("com_zimbra_archive");
        standardZimlets.add("com_zimbra_attachcontacts");
        standardZimlets.add("com_zimbra_attachmail");
        standardZimlets.add("com_zimbra_backuprestore");
        standardZimlets.add("com_zimbra_bulkprovision");
        standardZimlets.add("com_zimbra_cert_manager");
        standardZimlets.add("com_zimbra_click2call_cisco");
        standardZimlets.add("com_zimbra_click2call_mitel");
        standardZimlets.add("com_zimbra_clientuploader");
        standardZimlets.add("com_zimbra_convertd");
        standardZimlets.add("com_zimbra_date");
        standardZimlets.add("com_zimbra_delegatedadmin");
        standardZimlets.add("com_zimbra_email");
        standardZimlets.add("com_zimbra_hsm");
        standardZimlets.add("com_zimbra_license");
        standardZimlets.add("com_zimbra_mobilesync");
        standardZimlets.add("com_zimbra_phone");
        standardZimlets.add("com_zimbra_proxy_config");
        standardZimlets.add("com_zimbra_smime");
        standardZimlets.add("com_zimbra_smime_cert_admin");
        standardZimlets.add("com_zimbra_srchhighlighter");
        standardZimlets.add("com_zimbra_tooltip");
        standardZimlets.add("com_zimbra_ucconfig");
        standardZimlets.add("com_zimbra_url");
        standardZimlets.add("com_zimbra_viewmail");
        standardZimlets.add("com_zimbra_voiceprefs");
        standardZimlets.add("com_zimbra_webex");
        standardZimlets.add("com_zimbra_xmbxsearch");
        standardZimlets.add("com_zimbra_ymemoticons");
    }

    @Override
    void doUpgrade() throws ServiceException {
        ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
        try {
            doGlobalConfig(zlc);
            doAllDomain(zlc);
            doAllCos(zlc);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    private void doEntry(ZLdapContext zlc, Entry entry, String entryName, String attr) throws ServiceException {

        printer.println();
        printer.println("------------------------------");
        printer.println("Upgrading " + entryName + ": ");

        if (verbose) {
            printer.println("");
            printer.println("Checking " + entryName + ", attribute: " + attr);
        }

        Set<String> values = entry.getMultiAttrSet(attr);
        if (values.isEmpty()) {
            if (verbose) {
                printer.println("Current value is empty. No changes needed for " + entryName + ", attribute: " + attr);
            }
            return;
        }

        boolean modified = false;
        Map<String, Object> attrs = new HashMap<String, Object>();
        for (String value : values) {
            String zimletName = value;
            if (value.startsWith("!") || value.startsWith("+") || value.startsWith("-")) {
                zimletName = value.substring(1);
            }
            if (!standardZimlets.contains(zimletName) && !value.startsWith("-")) {
                StringUtil.addToMultiMap(attrs, attr, "-" + zimletName);
                modified = true;
            } else {
                StringUtil.addToMultiMap(attrs, attr, value);
            }
        }

        if (!modified) {
            if (verbose) {
                printer.println("No changes needed for " + entryName + ", attribute: " + attr);
            }
            return;
        }

        try {
            modifyAttrs(zlc, entry, attrs);
        } catch (ServiceException e) {
            // log the exception and continue
            printer.println("Caught ServiceException while modifying " + entryName + " attribute " + attr);
            printer.printStackTrace(e);
        }
    }

    private void doGlobalConfig(ZLdapContext zlc) throws ServiceException {
        Config config = prov.getConfig();
        doEntry(zlc, config, "global config", Provisioning.A_zimbraZimletDomainAvailableZimlets);
    }
    
    private void doAllDomain(ZLdapContext zlc) throws ServiceException {
        List<Domain> domains = prov.getAllDomains();
        for (Domain domain : domains) {
            String name = "domain " + domain.getName();
            doEntry(zlc, domain, name, Provisioning.A_zimbraZimletDomainAvailableZimlets);
        }
    }

    private void doAllCos(ZLdapContext zlc) throws ServiceException {
        List<Cos> coses = prov.getAllCos();
        for (Cos cos : coses) {
            String name = "cos " + cos.getName();
            doEntry(zlc, cos, name, Provisioning.A_zimbraZimletAvailableZimlets);
        }
    }
}