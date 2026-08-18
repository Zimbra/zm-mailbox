/*
* ***** BEGIN LICENSE BLOCK *****
* Zimbra Collaboration Suite Server
* Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.service.admin;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.auth.ropc.IRopcCredCache;
import com.zimbra.soap.ZimbraSoapContext;
import java.util.List;
import java.util.Map;

/**
 * Admin SOAP handler for clearing the MFA/ROPC rejection cache.
 *
 * <p>Handles {@code ClearMFARejectionCacheRequest} which allows global admins to
 * clear the push rejection counter cache either for a specific user or for
 * all users.</p>
 *
 * <h3>Usage:</h3>
 * <ul>
 *   <li>Per-user: {@code zmprov cmfarc user@domain.com}</li>
 *   <li>All users: {@code zmprov cmfarc --all}</li>
 * </ul>
 *
 * <h3>Access Control:</h3>
 * <p>Only global admins can use this handler. Domain admins are denied.</p>
 */

public class ClearMFARejectionCache extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        // global-admin-only check
        checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);
        Provisioning prov = Provisioning.getInstance();
        Account adminAccount = prov.getAccountById(zsc.getAuthtokenAccountId());
        String adminEmail = adminAccount != null ? adminAccount.getName() : zsc.getAuthtokenAccountId();
        Element accountEl = request.getOptionalElement(AdminConstants.E_ACCOUNT);
        int entriesCleared;
        if (accountEl != null) {
            String accountValue = accountEl.getText();
            String by = accountEl.getAttribute(AdminConstants.A_BY, "name");
            Account account = prov.get(com.zimbra.common.account.Key.AccountBy.fromString(by), accountValue);
            if (account == null) {
                throw ServiceException.INVALID_REQUEST("account not found: "
                        + accountValue, null);
            }

            String email = account.getName();
            boolean removed = IRopcCredCache.invalidateRejectionCacheByUsername(email);
            entriesCleared = removed ? 1 : 0;
            ZimbraLog.account.info(
                    "ClearMFARejectionCache: cleared MFA rejection cache for account=%s"
                            + " by admin=%s, entriesCleared=%d",
                    email, adminEmail, entriesCleared);

        } else {
            // --all: clear entire cache
            long count = IRopcCredCache.invalidateAllRejectionCache();
            entriesCleared = (int) count;

            ZimbraLog.account.info(
                    "ClearMFARejectionCache: cleared entire MFA rejection cache"
                            + " by admin=%s, entriesCleared=%d",
                    adminEmail, entriesCleared);
        }

        Element response = zsc.createElement(AdminConstants.E_CLEAR_MFA_REJECTION_CACHE_RESPONSE);
        response.addAttribute(AdminConstants.A_STATUS, "success");
        response.addAttribute(AdminConstants.A_ENTRIES_CLEARED, entriesCleared);

        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
    }
}
