/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;

public class AccountVersion {

    public static final int UNSET_VERSION = 0;

    public static final int CURRENT_VERSION = 1;

    public static boolean isAccountVersionCurrent(Account acct) {
        return acct.getIntAttr(Provisioning.A_zimbraVersion, UNSET_VERSION) < CURRENT_VERSION;
    }

    public static void migrateAccount(Account acct, Mailbox mailbox) throws ServiceException {
        if (isAccountVersionCurrent(acct)) return;
        Provisioning prov = Provisioning.getInstance();
        OperationContext opCtxt = new OperationContext(acct);

        int currentVersion =  acct.getIntAttr(Provisioning.A_zimbraVersion, UNSET_VERSION);

        switch (currentVersion) {
            case UNSET_VERSION:
                migrateToVersion1(acct, mailbox, prov, opCtxt);
            case 1:
                break;
        }

    }

    private static void migrateToVersion1(Account acct, Mailbox mailbox, Provisioning prov, OperationContext opCtxt) throws ServiceException {
        // place holder
    }
}
