/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.util.WebClientL10nUtil;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAllLocales extends AdminDocumentHandler {

    @Override
    public boolean domainAuthSufficient(Map<String, Object> context) {
        return true;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);

        Locale locales[] = WebClientL10nUtil.getAllLocalesSorted();
        Element response = zsc.createElement(AdminConstants.GET_ALL_LOCALES_RESPONSE);
        for (Locale locale : locales) {
            com.zimbra.cs.service.account.ToXML.encodeLocale(response, locale, Locale.US);
        }
        return response;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add("Allow all admins");
    }
}
