/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.CreateDisposableAddressBaseNameRequest;
import com.zimbra.soap.account.message.CreateDisposableAddressBaseNameResponse;

public class CreateDisposableAddressBaseName extends DocumentHandler {
    public Element handle(Element request, Map<String, Object> context) throws ServiceException, SoapFaultException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        CreateDisposableAddressBaseNameRequest req = zsc.elementToJaxb(request);
        String baseName = req.getBaseName();
        if (StringUtil.isNullOrEmpty(baseName)) {
            throw ServiceException.INVALID_REQUEST("empty base name", null);
        }
        if (StringUtil.isNullOrEmpty(account.getDisposableAddressBaseName())) {
            //TODO check base name is unique
            account.setDisposableAddressBaseName(baseName);
            ZimbraLog.account.info("disposable address base name set to %s", baseName);
        } else {
            throw ServiceException.INVALID_REQUEST("base name already created for this account", null);
        }
        CreateDisposableAddressBaseNameResponse response = new CreateDisposableAddressBaseNameResponse();
        return zsc.jaxbToElement(response);
    }
}

