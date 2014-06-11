/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;

public class BUG_88766 extends UpgradeOp {

    private static final String ATTR_NAME = Provisioning.A_zimbraPrefHtmlEditorDefaultFontFamily;
    private static final String FROM_VALUE = "times new roman, new york, times, serif";
    private static final String TO_VALUE = "arial, helvetica, sans-serif";

    @Override
    void doUpgrade() throws ServiceException {
        ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
        try {
            doCos(zlc);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @Override
    Description getDescription() {
        return new Description(this,
                new String[] {ATTR_NAME},
                new EntryType[] {EntryType.COS},
                FROM_VALUE,
                TO_VALUE,
                "Update html editor fonts to 8.5 default for all COSes where it is set to the previous default");
    }


    private void doCos(ZLdapContext zlc) throws ServiceException {
        List<Cos> classes = prov.getAllCos();
        if (classes != null) {
            for (Cos cos : classes) {
                String fonts = cos.getAttr(ATTR_NAME, "");
                if (FROM_VALUE.equalsIgnoreCase(fonts)) {
                    Map<String, Object> attrs = new HashMap<String, Object>();
                    attrs.put(ATTR_NAME, TO_VALUE);
                    modifyAttrs(cos, attrs);
                }
            }
        }
    }
}