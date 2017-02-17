/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
