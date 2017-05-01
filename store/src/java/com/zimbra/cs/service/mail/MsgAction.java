/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import com.google.common.base.Joiner;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @since Jul 8, 2004
 * @author dkarp
 */
public class MsgAction extends ItemAction {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element action = request.getElement(MailConstants.E_ACTION);
        String operation = action.getAttribute(MailConstants.A_OPERATION).toLowerCase();

        String successes = Joiner.on(",").join(handleCommon(context, request, MailItem.Type.MESSAGE).getSuccessIds());

        Element response = lc.createElement(MailConstants.MSG_ACTION_RESPONSE);
        Element act = response.addUniqueElement(MailConstants.E_ACTION);
        act.addAttribute(MailConstants.A_ID, successes);
        act.addAttribute(MailConstants.A_OPERATION, operation);
        return response;
    }
}
