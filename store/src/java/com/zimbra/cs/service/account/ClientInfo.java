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

import com.zimbra.common.account.Key;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;

import com.google.common.collect.ImmutableSet;
import com.zimbra.soap.account.message.ClientInfoRequest;
import com.zimbra.soap.account.message.ClientInfoResponse;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;


/**
 * ClientInfo returns domain attributes that may interest a client.
 */
public class ClientInfo extends AccountDocumentHandler {

    private static final Set<String> bootstrapInfoAttrs = ImmutableSet.of(
            ZAttrProvisioning.A_zimbraSkinLogoURL,
            ZAttrProvisioning.A_zimbraSkinLogoAppBanner,
            ZAttrProvisioning.A_zimbraSkinLogoLoginBanner,
            ZAttrProvisioning.A_zimbraWebClientLoginURL,
            ZAttrProvisioning.A_zimbraWebClientLogoutURL,
            ZAttrProvisioning.A_zimbraWebClientStaySignedInDisabled,
            ZAttrProvisioning.A_zimbraSkinBackgroundColor,
            ZAttrProvisioning.A_zimbraSkinForegroundColor,
            ZAttrProvisioning.A_zimbraSkinSecondaryColor,
            ZAttrProvisioning.A_zimbraSkinSelectionColor,
            ZAttrProvisioning.A_zimbraSkinFavicon,
            ZAttrProvisioning.A_zimbraFeatureResetPasswordStatus);

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        ClientInfoRequest req = JaxbUtil.elementToJaxb(request);

        ClientInfoResponse response = new ClientInfoResponse();
        Domain domain = findDomain(req.getHostname());
        if (domain != null) {
            response.setFeatureResetPasswordStatus(domain.getFeatureResetPasswordStatusAsString());
            response.setSkinBackgroundColor(domain.getSkinBackgroundColor());
            response.setSkinFavicon(domain.getSkinFavicon());
            response.setSkinForegroundColor(domain.getSkinForegroundColor());
            response.setSkinLogoAppBanner(domain.getSkinLogoAppBanner());
            response.setSkinLogoLoginBanner(domain.getSkinLogoLoginBanner());
            response.setSkinLogoURL(domain.getSkinLogoURL());
            response.setSkinSecondaryColor(domain.getSkinSecondaryColor());
            response.setSkinSelectionColor(domain.getSkinSelectionColor());
            response.setWebClientLoginURL(domain.getWebClientLoginURL());
            response.setWebClientLogoutURL(domain.getWebClientLogoutURL());
            response.setWebClientStaySignedInDisabled(domain.isWebClientStaySignedInDisabled());
        }
        return JaxbUtil.jaxbToElement(response);
    }

    protected Domain findDomain(String name) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Domain domain = null;

        String[] split = name.split("\\.");
        while (split.length > 1) {
            String partial = String.join(".", split);
            domain = prov.getDomain(Key.DomainBy.virtualHostname, partial, true);
            if (domain != null) {
                break;
            }
            split = Arrays.copyOfRange(split, 1, split.length);
        }
        return domain;
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
