/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.soap.ZimbraSoapContext;


public class GetSpellDictionaries
extends MailDocumentHandler {

    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zc = getZimbraSoapContext(context);
        Server server = Provisioning.getInstance().getLocalServer();
        Element response = zc.createElement(MailConstants.GET_SPELL_DICTIONARIES_RESPONSE);

        for (String dictionary : server.getSpellAvailableDictionary()) {
            response.addElement(MailConstants.E_DICTIONARY).setText(dictionary);
        }
        
        return response;
    }
}
