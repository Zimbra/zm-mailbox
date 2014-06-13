/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import com.google.common.base.Strings;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetAccountInfo extends AccountDocumentHandler  {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Element a = request.getElement(AccountConstants.E_ACCOUNT);
        String key = a.getAttribute(AccountConstants.A_BY);
        String value = a.getText();

        if (Strings.isNullOrEmpty(value)) {
            throw ServiceException.INVALID_REQUEST(
                "no text specified for the " + AccountConstants.E_ACCOUNT + " element", null);
        }
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.fromString(key), value, zsc.getAuthToken());

        // prevent directory harvest attack, mask no such account as permission denied
        if (account == null)
            throw ServiceException.PERM_DENIED("can not access account");

        Element response = zsc.createElement(AccountConstants.GET_ACCOUNT_INFO_RESPONSE);
        response.addAttribute(AccountConstants.E_NAME, account.getName(), Element.Disposition.CONTENT);
        response.addKeyValuePair(Provisioning.A_zimbraId, account.getId(), AccountConstants.E_ATTR, AccountConstants.A_NAME);
        response.addKeyValuePair(Provisioning.A_zimbraMailHost, account.getAttr(Provisioning.A_zimbraMailHost), AccountConstants.E_ATTR, AccountConstants.A_NAME);
        response.addKeyValuePair(Provisioning.A_displayName, account.getAttr(Provisioning.A_displayName), AccountConstants.E_ATTR, AccountConstants.A_NAME);
        addUrls(response, account);
        return response;
    }

    static void addUrls(Element response, Account account) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();

        Server server = prov.getServer(account);
        String hostname = server.getAttr(Provisioning.A_zimbraServiceHostname);
        Domain domain = prov.getDomain(account);
        if (server != null && hostname != null) {
            String httpSoap = URLUtil.getSoapPublicURL(server, domain, false);
            String httpsSoap = URLUtil.getSoapPublicURL(server, domain, true);

            if (httpSoap != null)
                response.addAttribute(AccountConstants.E_SOAP_URL /* soapURL */, httpSoap, Element.Disposition.CONTENT);

            if (httpsSoap != null && !httpsSoap.equalsIgnoreCase(httpSoap))
                /* Note: addAttribute with Element.Disposition.CONTENT REPLACEs any previous attribute with the same name.
                 * i.e. Will NOT end up with both httpSoap and httpsSoap as values for "soapURL"
                 */
                response.addAttribute(AccountConstants.E_SOAP_URL /* soapURL */, httpsSoap, Element.Disposition.CONTENT);

            String pubUrl = URLUtil.getPublicURLForDomain(server, domain, "", true);
            if (pubUrl != null)
                response.addAttribute(AccountConstants.E_PUBLIC_URL, pubUrl, Element.Disposition.CONTENT);

            String changePasswordUrl = null;
            if (domain != null)
                changePasswordUrl = domain.getAttr(Provisioning.A_zimbraChangePasswordURL);
            if (changePasswordUrl != null)
                response.addAttribute(AccountConstants.E_CHANGE_PASSWORD_URL, changePasswordUrl, Element.Disposition.CONTENT);
        }
        //add a Community redirect URL
        if(account.getBooleanAttr(Provisioning.A_zimbraFeatureSocialExternalEnabled, false)) {
            String clientID = account.getAttr(Provisioning.A_zimbraCommunityAPIClientID);
            if(clientID == null) {
                ZimbraLog.account.error("Zimbra Community client ID is not properly configured. zimbraCommunityAPIClientID cannot be empty.");
            }
            String clientSecret = account.getAttr(Provisioning.A_zimbraCommunityAPIClientSecret);
            if(clientSecret == null) {
                ZimbraLog.account.error("Zimbra Community client secret is not properly configured. zimbraCommunityAPIClientSecret cannot be empty.");
            }
            String nameAttribute = account.getAttr(Provisioning.A_zimbraCommunityUsernameMapping);
            if(nameAttribute == null) {
                ZimbraLog.account.error("Zimbra Community name mapping is not properly configured. zimbraCommunityUsernameMapping cannot be empty");
            }
            String socialBaseURL = account.getAttr(Provisioning.A_zimbraCommunityBaseURL);
            if(socialBaseURL == null) {
                ZimbraLog.account.error("Zimbra Community base URL is not properly configured. zimbraCommunityBaseURL cannot be empty");
            } else {
                if(socialBaseURL.endsWith("/")) { //avoid double slashes
                    socialBaseURL = socialBaseURL.substring(0,socialBaseURL.length() - 1);
                }
            }
            String socialTabURL = account.getAttr(Provisioning.A_zimbraCommunityHomeURL);
            if(socialTabURL == null) {
                ZimbraLog.account.error("Zimbra Community home URL is not properly configured. zimbraCommunityHomeURL cannot be empty");
            } else {
                if(!socialTabURL.startsWith("/")) { //make sure the path is relative
                    socialTabURL = "/".concat(socialTabURL);
                }
            }
            if(clientID != null && clientSecret != null && nameAttribute != null && socialBaseURL != null && socialTabURL != null) {
                try {
                    Date today = new Date();
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                    formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
                    Mac mac = Mac.getInstance("HmacSHA256");
                    SecretKeySpec key = new SecretKeySpec(clientSecret.getBytes("UTF8"), "HmacSHA256");
                    mac.init(key);
                    byte [] rawHmac = mac.doFinal(
                            String.format(
                                    "%s%s%s%s",
                                    account.getUid(),
                                    formatter.format(today),
                                    socialBaseURL,
                                    socialTabURL).getBytes("UTF8"));
                    String Base64Signature = Base64.encodeBase64String(rawHmac);

                    String szURL = String.format("%s/api.ashx/v2/oauth/redirect?client_id=%s&username=%s&time_stamp=%s&redirect_uri=%s&signature=%s",
                            socialBaseURL,
                            URLEncoder.encode(clientID,"UTF8"),
                            account.getAttr(nameAttribute),
                            URLEncoder.encode(formatter.format(today),"UTF8"),
                            URLEncoder.encode(socialBaseURL.concat(socialTabURL),"UTF8"),
                            URLEncoder.encode(Base64Signature,"UTF8"));
                    response.addAttribute(AccountConstants.E_COMMUNITY_URL, szURL, Element.Disposition.CONTENT);
                } catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException  e) {
                    throw ServiceException.FAILURE("Failed to generate community URL", e);
                }
            }
        }
    }
}
