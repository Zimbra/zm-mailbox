/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016, 2019, 2022 Synacor, Inc.
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

import java.util.Arrays;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

import com.zimbra.common.localconfig.LC;
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
            String webClientLogoutURL = domain.getWebClientLogoutURL();
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
            ToXML.encodeAttr(parent, Provisioning.A_zimbraWebClientLogoutURL, webClientLogoutURL);
            ToXML.encodeAttr(parent, Provisioning.A_zimbraWebClientStaySignedInDisabled, String.valueOf(domain.isWebClientStaySignedInDisabled()));
            ToXML.encodeAttr(parent, Provisioning.A_zimbraHelpModernURL, domain.getHelpModernURL());
            // TODO: ZCS-11319 update this line to read from LDAP property once this is moved out of LC.
            // e.g. change the `split(LC.web_client_logoff_..)` -> `domain.getWebClientLogoffURLs()`
            encodeAttrSkipLogoff(parent, webClientLogoutURL, StringUtils.split(LC.zimbra_web_client_logoff_urls.value()));
        }
        return parent;
    }

    /**
     * Add zimbraWebClientSkipLogoff to instruct the webclient to skip full logoff
     * when sending EndSession request, if the configured zimbraWebClientLogoutURL is
     * known to handle token de-registration.
     *
     * @param parent The element to add the attr to
     * @param webClientLogoutURL The configured webclient logout url
     * @param logoffURLs URLs to skip logoff for - we expect them to handle it when we send the user there
     */
    protected void encodeAttrSkipLogoff(Element parent, String webClientLogoutURL, String[] logoffURLs) {
        // always include the attr if webclient logout url is non-empty
        if (StringUtils.isNotEmpty(webClientLogoutURL)) {
            boolean skipLogoff = false;
            if (logoffURLs != null) {
                skipLogoff = Arrays.stream(logoffURLs).anyMatch(u -> webClientLogoutURL.equals(u));
            }
            // TODO: ZCS-11319 update this Provisioning.A_zimbraWebClientSkipLogoff
            ToXML.encodeAttr(parent, "zimbraWebClientSkipLogoff", String.valueOf(skipLogoff));
        }
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
