/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.mailbox.ContactRankings;
import com.zimbra.soap.ZimbraSoapContext;

public class RankingAction extends MailDocumentHandler {
    
    public static final String OP_RESET = "reset";
    public static final String OP_DELETE = "delete";
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT("");
        Element action = request.getElement(MailConstants.E_ACTION);
        String operation = action.getAttribute(MailConstants.A_OPERATION).toLowerCase();
        if (operation.equals(OP_RESET)) {
            ContactRankings.reset(account.getId());
        } else if (operation.equals(OP_DELETE)) {
            String email = action.getAttribute(MailConstants.A_EMAIL);
            ContactRankings.remove(account.getId(), email);
        }
        return zsc.createElement(MailConstants.RANKING_ACTION_RESPONSE);
    }
}
