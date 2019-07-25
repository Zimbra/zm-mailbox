/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra GraphQL Extension
 * Copyright (C) 2019 Synacor, Inc.
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
package com.zimbra.cs.service.common;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.UserRight;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author Zimbra API Team
 * @package com.zimbra.cs.service.common
 * @copyright Copyright © 2019
 */
public class SoapGqlCommonService {
    private ZimbraSoapContext zsc;

    @SuppressWarnings("unused")
    private SoapGqlCommonService() {
        // to make it unusable
    }

    /**
     * @param zsc
     */
    public SoapGqlCommonService(ZimbraSoapContext zsc) {
        this.zsc = zsc;
    }

    public Map<Right, Set<Entry>> discoverRights(List<String> rights, boolean onMaster) throws ServiceException {
        Account account = DocumentHandler.getRequestedAccount(zsc);
        RightManager rightMgr = RightManager.getInstance();
        Set<Right> rightsSet = Sets.newHashSet();
        for (String right : rights) {
            UserRight ur = rightMgr.getUserRight(right);
            rightsSet.add(ur);
        }

        if (rights.size() == 0) {
            throw ServiceException.INVALID_REQUEST("no right is specified", null);
        }

        AccessManager accessMgr = AccessManager.getInstance();
        Map<Right, Set<Entry>> discoveredRights = accessMgr.discoverUserRights(account, rightsSet, onMaster);

        return discoveredRights;
    }
}
