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
package com.zimbra.cs.gal;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.gal.GalOp;

public class ZimbraGalSearchBase {

    public static enum PredefinedSearchBase {
        DOMAIN,
        SUBDOMAINS,
        ROOT;
    };
    
    public static String getSearchBaseRaw(Domain domain, GalOp galOp) 
    throws ServiceException {
        String sb;
        if (galOp == GalOp.sync) {
            sb = domain.getAttr(Provisioning.A_zimbraGalSyncInternalSearchBase);
            if (sb == null) {
                sb = domain.getAttr(Provisioning.A_zimbraGalInternalSearchBase, 
                        PredefinedSearchBase.DOMAIN.name());
            }
        } else {
            sb = domain.getAttr(Provisioning.A_zimbraGalInternalSearchBase, 
                    PredefinedSearchBase.DOMAIN.name());
        }
        return sb;
    }
    
    public static String getSearchBase(Domain domain, GalOp galOp) 
    throws ServiceException {
        String sb = getSearchBaseRaw(domain, galOp);
        return domain.getGalSearchBase(sb);
    }
    
}
