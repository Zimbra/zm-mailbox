/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.service.mail;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.fb.FreeBusy;
import com.zimbra.cs.fb.WorkingHours;
import com.zimbra.soap.ZimbraSoapContext;

public class GetWorkingHours extends GetFreeBusy {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account authAcct = getAuthenticatedAccount(zsc);
        boolean asAdmin = zsc.isUsingAdminPrivileges();
        
        long rangeStart = request.getAttributeLong(MailConstants.A_CAL_START_TIME);
        long rangeEnd = request.getAttributeLong(MailConstants.A_CAL_END_TIME);
        validateRange(rangeStart, rangeEnd);
        
        String idParam = request.getAttribute(MailConstants.A_ID, null);    // comma-separated list of account zimbraId GUIDs
        String nameParam = request.getAttribute(MailConstants.A_NAME, null); // comma-separated list of account emails

        Provisioning prov = Provisioning.getInstance();
        Map<String /* zimbraId or name */, String /* zimbraId */> idMap = new LinkedHashMap<String, String>();  // preserve iteration order
        if (idParam != null) {
            String[] idStrs = idParam.split(",");
            for (String idStr : idStrs) {
                idMap.put(idStr, idStr);
            }
        }
        if (nameParam != null) {
            String[] nameStrs = nameParam.split(",");
            for (String nameStr : nameStrs) {
                Account acct = prov.get(AccountBy.name, nameStr);
                String idStr = null;
                if (acct != null) {
                    idStr = acct.getId();
                }
                idMap.put(nameStr, idStr);
            }
        }

        Element response = getResponseElement(zsc);
        for (Iterator<Map.Entry<String, String>> entryIter = idMap.entrySet().iterator(); entryIter.hasNext(); ) {
            Map.Entry<String, String> entry = entryIter.next();
            String idOrName = entry.getKey();
            String acctId = entry.getValue();
            Account acct = acctId != null ? prov.get(AccountBy.id, acctId) : null;
            FreeBusy workHours;
            if (acct != null) {
                String name; 
                if (!idOrName.equalsIgnoreCase(acctId))  // requested by name; use the same name in the response
                    name = idOrName;
                else
                    name = acct.getName();
                workHours = WorkingHours.getWorkingHours(authAcct, asAdmin, acct, name, rangeStart, rangeEnd);
            } else {
                workHours = FreeBusy.nodataFreeBusy(idOrName, rangeStart, rangeEnd);
            }
            ToXML.encodeFreeBusy(response, workHours);
        }
        return response;
    }
}
