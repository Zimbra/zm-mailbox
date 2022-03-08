/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager.AttrRightChecker;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.AttributeManager.IDNType;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.util.AccountUtil;

public class ToXML {
    public static Element encodeAccount(Element parent, Account account) {
        return encodeAccount(parent, account, true, null, null);
    }

    public static Element encodeAccount(Element parent, Account account, boolean applyCos) {
        return encodeAccount(parent, account, applyCos, null, null);
    }

    public static Element encodeAccount(Element parent, Account account, boolean applyCos,
            Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        return encodeAccount(parent, account,
                applyCos, false, reqAttrs,  attrRightChecker);
    }

    public static Element encodeAccount(Element parent, Account account,
            boolean applyCos, boolean needsExternalIndicator,
            Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        return encodeAccount(parent, account,
                applyCos, needsExternalIndicator, reqAttrs,  attrRightChecker, false);
    }
    
    public static Element encodeAccount(Element parent, Account account,
            boolean applyCos, boolean needsExternalIndicator,
            Set<String> reqAttrs, AttrRightChecker attrRightChecker, boolean isEffectiveQuota) {
        Element acctElem = parent.addNonUniqueElement(AccountConstants.E_ACCOUNT);
        acctElem.addAttribute(AccountConstants.A_NAME, account.getUnicodeName());
        acctElem.addAttribute(AccountConstants.A_ID, account.getId());

        if (needsExternalIndicator) {
            try {
                boolean isExternal = account.isAccountExternal();
                acctElem.addAttribute(AccountConstants.A_IS_EXTERNAL, isExternal);
            } catch (ServiceException e) {
                ZimbraLog.account.warn("unable to determine if account is external", e);
            }
        }
        Map attrs = account.getUnicodeAttrs(applyCos);
        if (isEffectiveQuota) {
            try {
                // setting effective quota value refer ZBUG-1869
                long effectiveQuota = AccountUtil.getEffectiveQuota(account);
                attrs.put(Provisioning.A_zimbraMailQuota, String.valueOf(effectiveQuota));
            } catch (ServiceException e) {
                ZimbraLog.account.error("error in getting effective zimbra mail quota", e);
            }
        }
        encodeAttrs(acctElem, attrs, AdminConstants.A_N, reqAttrs, attrRightChecker);
        return acctElem;
    }

    public static Element encodeCalendarResource(Element parent, CalendarResource resource) {
        return encodeCalendarResource(parent, resource, false, null, null);
    }

    public static Element encodeCalendarResource(Element parent, CalendarResource resource, boolean applyCos) {
        return encodeCalendarResource(parent, resource, applyCos, null, null);
    }

    public static Element encodeCalendarResource(Element parent, CalendarResource resource, boolean applyCos,
            Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        Element resElem = parent.addNonUniqueElement(AccountConstants.E_CALENDAR_RESOURCE);
        resElem.addAttribute(AccountConstants.A_NAME, resource.getUnicodeName());
        resElem.addAttribute(AccountConstants.A_ID, resource.getId());
        Map attrs = resource.getUnicodeAttrs(applyCos);
        encodeAttrs(resElem, attrs, AdminConstants.A_N, reqAttrs, attrRightChecker);
        return resElem;
    }

    public static void encodeAttrs(Element e, Map attrs, Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        encodeAttrs(e, attrs, AdminConstants.A_N, reqAttrs, attrRightChecker);
    }

    public static void encodeAttrs(Element e, Map attrs, String key,
            Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        encodeAttrs(e, attrs, key, reqAttrs, null, attrRightChecker);
    }

    public static void encodeAttrs(Element e, Map attrs, String key,
            Set<String> reqAttrs, Set<String> hideAttrs, AttrRightChecker attrRightChecker) {
        AttributeManager attrMgr = null;
        try {
            attrMgr = AttributeManager.getInstance();
        } catch (ServiceException se) {
            ZimbraLog.account.warn("failed to get AttributeManager instance", se);
        }

        Set<String> reqAttrsLowerCase = null;
        if (reqAttrs != null) {
            reqAttrsLowerCase = new HashSet<String>();
            for (String reqAttr : reqAttrs) {
                reqAttrsLowerCase.add(reqAttr.toLowerCase());
            }
        }

        for (Iterator iter = attrs.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Entry) iter.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();

            // Never return data source passwords
            if (name.equalsIgnoreCase(Provisioning.A_zimbraDataSourcePassword)) {
                continue;
            }

            value = Provisioning.sanitizedAttrValue(name, value);

            // only returns requested attrs
            if (reqAttrsLowerCase != null && !reqAttrsLowerCase.contains(name.toLowerCase())) {
                continue;
            }

            // do not return attrs hidden by protocol
            if (hideAttrs != null && hideAttrs.contains(name)) {
                continue;
            }

            boolean allowed = attrRightChecker == null ? true : attrRightChecker.allowAttr(name);

            IDNType idnType = AttributeManager.idnType(attrMgr, name);

            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++) {
                    encodeAttr(e, name, sv[i], AdminConstants.E_A, key, idnType, allowed);
                }
            } else if (value instanceof String) {
                value = com.zimbra.cs.service.account.ToXML.fixupZimbraPrefTimeZoneId(name, (String)value);
                encodeAttr(e, name, (String)value, AdminConstants.E_A, key, idnType, allowed);
            }
        }
    }

    public static void encodeAttr(Element parent, String key, String value, String eltname, String attrname,
            IDNType idnType, boolean allowed) {

        Element e = parent.addNonUniqueElement(eltname);
        e.addAttribute(attrname, key);

        if (allowed) {
            e.setText(IDNUtil.toUnicode(value, idnType));
        } else {
            e.addAttribute(AccountConstants.A_PERM_DENIED, true);
        }
    }
}
