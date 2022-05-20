/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import com.google.common.base.Strings;
import com.zimbra.common.calendar.TZIDMapper;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.KeyValuePair;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager.AttrRightChecker;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.AttributeManager.IDNType;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.EntrySearchFilter.Multi;
import com.zimbra.cs.account.EntrySearchFilter.Single;
import com.zimbra.cs.account.EntrySearchFilter.Visitor;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.Signature.SignatureContent;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.soap.account.type.Attr;
import com.zimbra.soap.account.type.HABGroup;

public class ToXML {

    public static Set<String> skipAttrs = new HashSet<String>();
    static {
        skipAttrs.add(Provisioning.A_member);
        skipAttrs.add(Provisioning.A_zimbraMailForwardingAddress);
        };
    
    static Element encodeAccount(Element parent, Account account) {
        return encodeAccount(parent, account, true, null, null);
    }

    static Element encodeAccount(Element parent, Account account, boolean applyCos,
            Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        Element acctElem = parent.addNonUniqueElement(AccountConstants.E_ACCOUNT);
        acctElem.addAttribute(AccountConstants.A_NAME, account.getUnicodeName());
        acctElem.addAttribute(AccountConstants.A_ID, account.getId());
        Map attrs = account.getUnicodeAttrs(applyCos);
        encodeAttrs(acctElem, attrs, AccountConstants.A_N, reqAttrs, attrRightChecker);
        return acctElem;
    }

    static Element encodeCalendarResource(Element parent, CalendarResource resource) {
        return encodeCalendarResource(parent, resource, false, null, null);
    }

    static Element encodeCalendarResource(Element parent, CalendarResource resource, boolean applyCos) {
        return encodeCalendarResource(parent, resource, applyCos, null, null);
    }

    static Element encodeCalendarResource(Element parent, CalendarResource resource, boolean applyCos,
            Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        Element resElem = parent.addNonUniqueElement(AccountConstants.E_CALENDAR_RESOURCE);
        resElem.addAttribute(AccountConstants.A_NAME, resource.getUnicodeName());
        resElem.addAttribute(AccountConstants.A_ID, resource.getId());
        Map attrs = resource.getUnicodeAttrs(applyCos);
        encodeAttrs(resElem, attrs, AccountConstants.A_N, reqAttrs, attrRightChecker);
        return resElem;
    }

    public static Element encodeCalendarResource(Element parent, String id, String name, Map attrs,
            Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        Element resElem = parent.addNonUniqueElement(AccountConstants.E_CALENDAR_RESOURCE);
        resElem.addAttribute(AccountConstants.A_NAME, name);
        resElem.addAttribute(AccountConstants.A_ID, id);
        encodeAttrs(resElem, attrs, AccountConstants.A_N, reqAttrs, attrRightChecker);
        return resElem;
    }

    public static Element encodePasswordRules(Element parent, Account account) {

        ToXML.encodeAttr(parent, "zimbraPasswordAllowUsername", Boolean.toString(LC.allow_username_within_password.booleanValue()));
        ToXML.encodeAttr(parent, Provisioning.A_zimbraPasswordLocked, Boolean.toString(account.isPasswordLocked()));
        ToXML.encodeAttr(parent, Provisioning.A_zimbraPasswordMinLength, Integer.toString(account.getPasswordMinLength()));
        ToXML.encodeAttr(parent, Provisioning.A_zimbraPasswordMaxLength, Integer.toString(account.getPasswordMaxLength()));
        ToXML.encodeAttr(parent, Provisioning.A_zimbraPasswordMinUpperCaseChars, Integer.toString(account.getPasswordMinUpperCaseChars()));
        ToXML.encodeAttr(parent, Provisioning.A_zimbraPasswordMinLowerCaseChars, Integer.toString(account.getPasswordMinLowerCaseChars()));
        ToXML.encodeAttr(parent, Provisioning.A_zimbraPasswordMinPunctuationChars, Integer.toString(account.getPasswordMinPunctuationChars()));
        ToXML.encodeAttr(parent, Provisioning.A_zimbraPasswordMinNumericChars, Integer.toString(account.getPasswordMinNumericChars()));
        ToXML.encodeAttr(parent, Provisioning.A_zimbraPasswordMinDigitsOrPuncs, Integer.toString(account.getPasswordMinDigitsOrPuncs()));
        ToXML.encodeAttr(parent, Provisioning.A_zimbraPasswordMinAge, Integer.toString(account.getPasswordMinAge()));
        ToXML.encodeAttr(parent, Provisioning.A_zimbraPasswordMaxAge, Integer.toString(account.getPasswordMaxAge()));
        ToXML.encodeAttr(parent, Provisioning.A_zimbraPasswordEnforceHistory, Integer.toString(account.getPasswordEnforceHistory()));
        ToXML.encodeAttr(parent, Provisioning.A_zimbraPasswordBlockCommonEnabled, Boolean.toString(account.isPasswordBlockCommonEnabled()));
        return parent;
    }

    static void encodeAttrs(Element e, Map attrs, Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        encodeAttrs(e, attrs, AccountConstants.A_N, reqAttrs, attrRightChecker);
    }

    private static void encodeAttrs(Element e, Map attrs, String key, Set<String> reqAttrs, AttrRightChecker attrRightChecker) {
        AttributeManager attrMgr = null;
        try {
            attrMgr = AttributeManager.getInstance();
        } catch (ServiceException se) {
            ZimbraLog.account.warn("failed to get AttributeManager instance", se);
        }

        for (Iterator iter = attrs.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Entry) iter.next();
            String name = (String) entry.getKey();
            Object value = entry.getValue();

            // Never return data source passwords
            if (name.equalsIgnoreCase(Provisioning.A_zimbraDataSourcePassword))
                continue;

            value = Provisioning.sanitizedAttrValue(name, value);

            // only returns requested attrs
            if (reqAttrs != null && !reqAttrs.contains(name))
                continue;

            boolean allowed = attrRightChecker == null ? true : attrRightChecker.allowAttr(name);

            IDNType idnType = AttributeManager.idnType(attrMgr, name);

            if (value instanceof String[]) {
                String sv[] = (String[]) value;
                for (int i = 0; i < sv.length; i++) {
                    encodeAttr(e, name, sv[i], AccountConstants.E_A, key, idnType, allowed);
                }
            } else if (value instanceof String) {
                value = fixupZimbraPrefTimeZoneId(name, (String)value);
                encodeAttr(e, name, (String)value, AccountConstants.E_A, key, idnType, allowed);
            }
        }
    }

    private static class EntrySearchFilterXmlVisitor implements Visitor {
        Stack<Element> mParentStack;
        Element mRootElement;

        public EntrySearchFilterXmlVisitor(Element parent) {
            mParentStack = new Stack<Element>();
            mParentStack.push(parent);
        }

        public Element getRootElement() { return mRootElement; }

        @Override
        public void visitSingle(Single term) {
            Element parent = mParentStack.peek();
            Element elem = parent.addNonUniqueElement(AccountConstants.E_ENTRY_SEARCH_FILTER_SINGLECOND);
            if (mRootElement == null) mRootElement = elem;
            if (term.isNegation())
                elem.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_NEGATION, true);
            elem.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_ATTR, term.getLhs());
            elem.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OP, term.getOperator().toString());
            elem.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_VALUE, term.getRhs());
        }

        @Override
        public void enterMulti(Multi term) {
            Element parent = mParentStack.peek();
            Element elem = parent.addNonUniqueElement(AccountConstants.E_ENTRY_SEARCH_FILTER_MULTICOND);
            if (mRootElement == null) mRootElement = elem;
            if (term.isNegation())
                elem.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_NEGATION, true);
            if (!term.isAnd())
                elem.addAttribute(AccountConstants.A_ENTRY_SEARCH_FILTER_OR, true);
            mParentStack.push(elem);
        }

        @Override
        public void leaveMulti(Multi term) {
            mParentStack.pop();
        }
    }

    public static Element encodeEntrySearchFilter(Element parent, EntrySearchFilter filter) {
        EntrySearchFilterXmlVisitor visitor = new EntrySearchFilterXmlVisitor(parent);
        filter.traverse(visitor);
        return visitor.getRootElement();
    }

    public static Element encodeLocale(Element parent, Locale locale, Locale inLocale) {
        Element e = parent.addNonUniqueElement(AccountConstants.E_LOCALE);
        String id = locale.toString();

        // name in the locale itself, if it is present.
        String name = L10nUtil.getMessage(false /* shoutIfMissing */,
                L10nUtil.L10N_MSG_FILE_BASENAME, id, Locale.getDefault());
        if (name == null) {
            name = locale.getDisplayName(inLocale);
        }

        // name in the locale of choice
        String localName = locale.getDisplayName(inLocale);

        e.addAttribute(AccountConstants.A_ID, id);
        e.addAttribute(AccountConstants.A_NAME, name != null ? name : id);
        e.addAttribute(AccountConstants.A_LOCAL_NAME, localName != null ? localName : id);
        return e;
    }

    public static Element encodeIdentity(Element parent, Identity identity) {
        Element e = parent.addNonUniqueElement(AccountConstants.E_IDENTITY);
        e.addAttribute(AccountConstants.A_NAME, identity.getName());
        e.addAttribute(AccountConstants.A_ID, identity.getId());
        encodeAttrs(e, identity.getUnicodeAttrs(), AccountConstants.A_NAME, null, null);
        return e;
    }

    public static Element encodeSignature(Element parent, Signature signature) {
        Element e = parent.addNonUniqueElement(AccountConstants.E_SIGNATURE);
        e.addAttribute(AccountConstants.A_NAME, signature.getName());
        e.addAttribute(AccountConstants.A_ID, signature.getId());

        Set<SignatureContent> contents = signature.getContents();
        for (SignatureContent c : contents) {
            e.addNonUniqueElement(AccountConstants.E_CONTENT)
                .addAttribute(AccountConstants.A_TYPE, c.getMimeType()).addText(c.getContent());
        }

        String contactId = signature.getAttr(Provisioning.A_zimbraPrefMailSignatureContactId);
        if (contactId != null)
            e.addNonUniqueElement(AccountConstants.E_CONTACT_ID).setText(contactId);

        return e;
    }

    public static Element encodeDataSource(Element parent, DataSource ds) {
        Element e = parent.addNonUniqueElement(AccountConstants.E_DATA_SOURCE);
        e.addAttribute(AccountConstants.A_NAME, ds.getName());
        e.addAttribute(AccountConstants.A_ID, ds.getId());
        e.addAttribute(AccountConstants.A_TYPE, ds.getType().name());
        encodeAttrs(e, ds.getUnicodeAttrs(), AccountConstants.A_N, null, null);
        return e;
    }

    public static void encodeAttr(Element parent, String key, String value, String eltname, String attrname,
            IDNType idnType, boolean allowed) {

        KeyValuePair kvPair;
        if (allowed) {
            kvPair = parent.addKeyValuePair(key, IDNUtil.toUnicode(value, idnType), eltname, attrname);
        } else {
            kvPair = parent.addKeyValuePair(key, "", eltname, attrname);
            kvPair.addAttribute(AccountConstants.A_PERM_DENIED, true);
        }
    }

    public static void encodeAttr(Element response, String key, Object value) {
        if (value instanceof String[]) {
            String sa[] = (String[]) value;
            for (int i = 0; i < sa.length; i++) {
                if (!Strings.isNullOrEmpty(sa[i])) {
                    response.addKeyValuePair(key, sa[i], AccountConstants.E_ATTR, AccountConstants.A_NAME);
                }
            }
        } else if (value instanceof String) {
            if (!Strings.isNullOrEmpty((String) value)) {
                response.addKeyValuePair(key, (String) value, AccountConstants.E_ATTR, AccountConstants.A_NAME);
            }
        }
    }

    /**
     * Fixup for time zone id.  Always use canonical (Olson ZoneInfo) ID.
     */
    public static String fixupZimbraPrefTimeZoneId(String attrName, String attrValue) {
        if (Provisioning.A_zimbraPrefTimeZoneId.equals(attrName))
            return TZIDMapper.canonicalize(attrValue);
        else
            return attrValue;
    }

    public static Element encodeACE(Element parent, ZimbraACE ace) {
        Element eACE = parent.addNonUniqueElement(AccountConstants.E_ACE)
                .addAttribute(AccountConstants.A_ZIMBRA_ID, ace.getGrantee())
                .addAttribute(AccountConstants.A_GRANT_TYPE, ace.getGranteeType().getCode())
                .addAttribute(AccountConstants.A_RIGHT, ace.getRight().getName())
                .addAttribute(AccountConstants.A_DISPLAY, ace.getGranteeDisplayName());

        if (ace.getGranteeType() == GranteeType.GT_KEY) {
            eACE.addAttribute(AccountConstants.A_ACCESSKEY, ace.getSecret());
        } else if (ace.getGranteeType() == GranteeType.GT_GUEST) {
            eACE.addAttribute(AccountConstants.A_PASSWORD, ace.getSecret());
        }
        if (ace.deny()) {
            eACE.addAttribute(AccountConstants.A_DENY, ace.deny());
        }
        return eACE;
    }
    
    /**
     * 
     * @param parent parent element of XML response
     * @param grp the group to encode
     * @param parentId id of the parent group
     */
    public static void encodeHabGroup(Element parent, HABGroup grp, String parentId) {
        Element habGroup = parent.addNonUniqueElement(AccountConstants.E_HAB_GROUP);
        habGroup.addAttribute(AccountConstants.A_NAME, grp.getName());
        for (Attr attr : grp.getAttrs()) {
            if (!skipAttrs.contains(attr.getKey())) {
                ToXML.encodeAttr(habGroup, attr.getKey(), attr.getValue());
            }
        }
        if (parentId != null) {
            habGroup.addAttribute(AccountConstants.A_PARENT_HAB_GROUP_ID, parentId);
        }

        habGroup.addAttribute(AdminConstants.A_ID, grp.getId());
        habGroup.addAttribute(AccountConstants.A_HAB_SENIORITY_INDEX, grp.getSeniorityIndex());
        for (HABGroup habGrp : grp.getChildGroups()) {
            if (habGrp.getId() != null) {
                encodeHabGroup(habGroup, habGrp, grp.getId());
            }
        }

    }

}
