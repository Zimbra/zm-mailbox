/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.GetAccountInfoRequest;

/**
 * @author schemers
 */
public class GetAccountInfo extends AdminDocumentHandler  {


    /**
     * must be careful and only return accounts a domain admin can see
     */
    @Override
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    /**
     * @return true - which means accept responsibility for measures to prevent account harvesting by delegate admins
     */
    @Override
    public boolean defendsAgainstDelegateAdminAccountHarvesting() {
        return true;
    }

    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        GetAccountInfoRequest req = zsc.elementToJaxb(request);
        AccountBy accountBy = req.getAccount().getBy().toKeyAccountBy();
        String accountSelectorKey = req.getAccount().getKey();

        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(accountBy, accountSelectorKey, zsc.getAuthToken());
        defendAgainstAccountOrCalendarResourceHarvesting(account, accountBy, accountSelectorKey, zsc,
                Admin.R_getAccountInfo, Admin.R_getCalendarResourceInfo);
        Element response = zsc.createElement(AdminConstants.GET_ACCOUNT_INFO_RESPONSE);
        response.addNonUniqueElement(AdminConstants.E_NAME).setText(account.getName());
        addAttr(response, Provisioning.A_zimbraId, account.getId());
        addAttr(response, Provisioning.A_zimbraMailHost, account.getAttr(Provisioning.A_zimbraMailHost));
        addAttr(response, Provisioning.A_zimbraServiceAccountNumber, account.getAttr(Provisioning.A_zimbraServiceAccountNumber));

        doCos(account, response);
        addUrls(response, account);

        return response;
    }

    static void addUrls(Element response, Account account) throws ServiceException {

        Server server = Provisioning.getInstance().getServer(account);
        if (server == null) return;
        String hostname = server.getAttr(Provisioning.A_zimbraServiceHostname);
        if (hostname == null) return;

        String http = URLUtil.getSoapURL(server, false);
        String https = URLUtil.getSoapURL(server, true);

        if (http != null)
            response.addNonUniqueElement(AdminConstants.E_SOAP_URL).setText(http);

        if (https != null && !https.equalsIgnoreCase(http))
            response.addNonUniqueElement(AdminConstants.E_SOAP_URL).setText(https);

        String adminUrl = URLUtil.getAdminURL(server);
        if (adminUrl != null)
            response.addNonUniqueElement(AdminConstants.E_ADMIN_SOAP_URL).setText(adminUrl);

        String webMailUrl = URLUtil.getPublicURLForDomain(server, Provisioning.getInstance().getDomain(account), "", true);
        if (webMailUrl != null)
            response.addNonUniqueElement(AdminConstants.E_PUBLIC_MAIL_URL).setText(webMailUrl);

    }

    private static void addAttr(Element response, String name, String value) {
        if (value != null && !value.equals("")) {
            Element e = response.addNonUniqueElement(AdminConstants.E_A);
            e.addAttribute(AdminConstants.A_N, name);
            e.setText(value);
        }
    }

    static void doCos(Account acct, Element response) throws ServiceException {
        Cos cos = Provisioning.getInstance().getCOS(acct);
        if (cos != null) {
            Element eCos = response.addUniqueElement(AdminConstants.E_COS);
            eCos.addAttribute(AdminConstants.A_ID, cos.getId());
            eCos.addAttribute(AdminConstants.A_NAME, cos.getName());
        }
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getAccountInfo);
        relatedRights.add(Admin.R_getCalendarResourceInfo);
    }
}
