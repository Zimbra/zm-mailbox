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
package com.zimbra.cs.account.ldap.upgrade;

import com.zimbra.common.service.ServiceException;

public enum UpgradeTask {
    
    BUG_11562(BUG_11562.class),
    BUG_14531(BUG_14531.class),
    BUG_18277(BUG_18277.class),
    BUG_22033(BUG_22033.class),
    BUG_27075(BUG_27075.class), // e.g. -b 27075 5.0.12
    BUG_29978(BUG_29978.class),
    // BUG_31284(ZimbraPrefFromDisplay.class),
    BUG_31694(BUG_31694.class),
    BUG_32557(BUG_32557.class),
    BUG_32719(BUG_32719.class),
    BUG_33814(BUG_33814.class),
    BUG_41000(BUG_41000.class),
    BUG_42828(BUG_42828.class),
    BUG_42877(BUG_42877.class),
    BUG_42896(BUG_42896.class),
    BUG_43147(BUG_43147.class),
    BUG_43779(BUG_43779.class),
    BUG_46297(BUG_46297.class),
    BUG_46883(BUG_46883.class),
    BUG_46961(BUG_46961.class),
    BUG_47934(BUG_47934.class),
    BUG_50258(BUG_50258.class),
    BUG_50458(BUG_50458.class),
    BUG_50465(BUG_50465.class),
    BUG_53745(BUG_53745.class),
    BUG_55649(BUG_55649.class),
    BUG_57039(BUG_57039.class),
    BUG_57205(BUG_57205.class),
    BUG_57425(BUG_57425.class),
    BUG_57855(BUG_57855.class),
    BUG_57866(BUG_57866.class),
    BUG_57875(BUG_57875.class),
    BUG_58084(BUG_58084.class),
    BUG_58481(BUG_58481.class),
    BUG_58514(BUG_58514.class),
    BUG_59720(BUG_59720.class),
    BUG_63475(BUG_63475.class),
    BUG_63722(BUG_63722.class),
    BUG_64380(BUG_64380_63587.class),
    BUG_65070(BUG_65070.class),
    BUG_66001(BUG_66001.class);

    
    private Class<? extends UpgradeOp> upgradeOpClass;
    
    private UpgradeTask(Class<? extends UpgradeOp> upgradeOpClass) {
        this.upgradeOpClass = upgradeOpClass;
    }
    
    UpgradeOp getUpgradeOp() throws ServiceException {
        try {
            return upgradeOpClass.newInstance();
        } catch (IllegalAccessException e) {
            throw ServiceException.FAILURE("IllegalAccessException: " + upgradeOpClass.getCanonicalName(), e);
        } catch (InstantiationException e) {
            throw ServiceException.FAILURE("InstantiationException: " + upgradeOpClass.getCanonicalName(), e);
        }
    }
    
    static UpgradeTask getTaskByBug(String bugNumber) throws ServiceException {
        String bug = "BUG_" + bugNumber;
        
        try {
            return UpgradeTask.valueOf(bug);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
