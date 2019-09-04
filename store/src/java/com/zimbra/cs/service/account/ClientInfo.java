/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016, 2019 Synacor, Inc.
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
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.ClientInfoRequest;


/**
 * ClientInfo returns domain attributes that may interest a client.
 */
public class ClientInfo extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        ClientInfoRequest req = JaxbUtil.elementToJaxb(request);

        Domain domain = Provisioning.getInstance().get(req.getDomain());

        Element parent =  zsc.createElement(AccountConstants.CLIENT_INFO_RESPONSE);
        if (domain != null) {
            ToXML.encodeAttr(parent, Provisioning.A_zimbraFeatureResetPasswordStatus, domain.getFeatureResetPasswordStatusAsString());
            ToXML.encodeAttr(parent, Provisioning.A_zimbraSkinBackgroundColor, domain.getSkinBackgroundColor());
            ToXML.encodeAttr(parent, Provisioning.A_zimbraSkinFavicon, domain.getSkinFavicon());
            ToXML.encodeAttr(parent, Provisioning.A_zimbraSkinForegroundColor, domain.getSkinForegroundColor());
            ToXML.encodeAttr(parent, Provisioning.A_zimbraSkinLogoAppBanner, domain.getSkinLogoAppBanner());
            ToXML.encodeAttr(parent, Provisioning.A_zimbraSkinLogoLoginBanner, domain.getSkinLogoLoginBanner());
            ToXML.encodeAttr(parent, Provisioning.A_zimbraSkinLogoURL, domain.getSkinLogoURL());
            ToXML.encodeAttr(parent, Provisioning.A_zimbraSkinSecondaryColor, domain.getSkinSecondaryColor());
            ToXML.encodeAttr(parent, Provisioning.A_zimbraSkinSelectionColor, domain.getSkinSelectionColor());
            ToXML.encodeAttr(parent, Provisioning.A_zimbraWebClientLoginURL, domain.getWebClientLoginURL());
            ToXML.encodeAttr(parent, Provisioning.A_zimbraWebClientLogoutURL, domain.getWebClientLogoutURL());
            ToXML.encodeAttr(parent, Provisioning.A_zimbraWebClientStaySignedInDisabled, String.valueOf(domain.isWebClientStaySignedInDisabled()));
        }
        return parent;
    }

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }

    @Override
    public boolean needsAdminAuth(Map<String, Object> context) {
        return false;
    }

}
