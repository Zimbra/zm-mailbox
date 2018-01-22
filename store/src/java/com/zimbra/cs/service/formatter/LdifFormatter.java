/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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
package com.zimbra.cs.service.formatter;

import java.io.IOException;

import com.zimbra.cs.account.ldap.LdapGalMapRule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.servlet.ServletException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapGalMapRules;
import com.zimbra.cs.account.ldap.LdapObjectClass;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.service.UserServletContext;
import com.zimbra.cs.service.UserServletException;
import com.zimbra.cs.service.formatter.FormatterFactory.FormatType;
import com.zimbra.cs.service.util.UserServletUtil;

public class LdifFormatter extends Formatter {

    private static final String NEW_LINE = "\r\n";

    @Override
    public FormatType getType() {
        return FormatType.LDIF;
    }

    @Override
    public void formatCallback(UserServletContext context) throws UserServletException,
        ServiceException, IOException, ServletException, MessagingException {
        Provisioning prov = Provisioning.getInstance();
        Domain d = prov.getDomain(context.getAuthAccount());
        LdapGalMapRules rules = (LdapGalMapRules) d.getCachedData("GAL_RULES");
        if (rules == null) {
            rules = new LdapGalMapRules(d, false);
            d.setCachedData("GAL_RULES", rules);
        }
        String[] galLdapAttrMap = d.getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap);
        Iterator<? extends MailItem> contacts = null;
        StringBuilder sb = new StringBuilder();
        try {
            contacts = getMailItems(context, -1, -1, Integer.MAX_VALUE);
            ArrayList<Map<String, String>> allContacts = new ArrayList<Map<String, String>>();
            HashSet<String> fields = new HashSet<String>();
            UserServletUtil.populateContactFields(contacts, context.targetMailbox,
                context.opContext, allContacts, fields);
            for (Map<String, String> contactMap : allContacts) {
                if (toLDIFContact(contactMap, sb, galLdapAttrMap)) {
                    sb.append(NEW_LINE);
                }
            }
        } finally {
            if (contacts instanceof QueryResultIterator)
                ((QueryResultIterator) contacts).finished();
        }
        String filename = context.itemPath;
        if (StringUtil.isNullOrEmpty(filename))
            filename = "contacts";
        if (!filename.toLowerCase().endsWith(".ldif")) {
            filename = filename + ".ldif";
        }
        String cd = HttpUtil.createContentDisposition(context.req, Part.ATTACHMENT, filename);
        context.resp.addHeader("Content-Disposition", cd);
        context.resp.setCharacterEncoding(context.getCharset().name());
        context.resp.setContentType(MimeConstants.CT_TEXT_LDIF);
        context.resp.getWriter().print(sb.toString());
    }

    protected boolean toLDIFContact(Map<String, String> contact, StringBuilder sb, String[] galLdapAttrMap) {
        if (!hasRequiredFields(contact)
            || ContactConstants.TYPE_GROUP.equals(contact.get(ContactConstants.A_type))) {
            return false;
        }
        addLDIFEntry(LdapConstants.ATTR_dn, getDistinguishedName(contact), sb, false);
        addObjectClass(sb);
        String cn = getCommonName(contact);
        if (!StringUtil.isNullOrEmpty(cn)) {
            encodeAndAddLDIFEntry("cn", cn, sb);
        }
        for (String attr : galLdapAttrMap) {
            LdapGalMapRule rule = new LdapGalMapRule(attr, null);
            addLDIFEntriesForGalRule(rule, sb, contact);
        }
        return true;
    }

    private boolean hasRequiredFields(Map<String, String> contact) {
        return (!StringUtil.isNullOrEmpty(contact.get(ContactConstants.A_email))
            || !StringUtil.isNullOrEmpty(contact.get(ContactConstants.A_firstName))
            || !StringUtil.isNullOrEmpty(contact.get(ContactConstants.A_lastName))
            || !StringUtil.isNullOrEmpty(contact.get(ContactConstants.A_company)));
    }

    private boolean addLDIFEntriesForGalRule(LdapGalMapRule rule, StringBuilder sb,
        Map<String, String> contact) {
        String[] contactAttrs = rule.getContactAttrs();
        String[] ldapAttrs = rule.getLdapAttrs();
        String key = getContactKey(contactAttrs);
        if (key != null) {
            String value = contact.get(key);
            if (value != null) {
                if (key.equals(ContactConstants.A_lastName)
                    || key.equals(ContactConstants.A_email)
                    || key.equals(ContactConstants.A_jobTitle)
                    || key.equals(ContactConstants.A_workStreet)
                    || key.equals(ContactConstants.A_workCity)
                    || key.equals(ContactConstants.A_workState)
                    || key.equals(ContactConstants.A_workPostalCode)
                    || key.equals(ContactConstants.A_workPhone)
                    || key.equals(ContactConstants.A_workFax)
                    || key.equals(ContactConstants.A_mobilePhone)
                    || key.equals(ContactConstants.A_homePhone)
                    || key.equals(ContactConstants.A_pager)
                    || key.equals(ContactConstants.A_company)
                    || key.equals(ContactConstants.A_notes) || key.equals(ContactConstants.A_office)
                    || key.equals(ContactConstants.A_department)) {
                    // Append these attributes encoded value only for last ldap mapping
                    String encodedValue = encodeValue(value);
                    String ldapAttr = ldapAttrs[ldapAttrs.length - 1];
                    addLDIFEntry(ldapAttr, encodedValue, sb, !encodedValue.equals(value));
                    return true;
                } else if (key.equals(ContactConstants.A_firstName)) {
                    // Append this attributes encoded value for first ldap mapping
                    String encodedValue = encodeValue(value);
                    String ldapAttr = ldapAttrs[0];
                    addLDIFEntry(ldapAttr, encodedValue, sb, !encodedValue.equals(value));
                    return true;
                } else if (key.equals(ContactConstants.A_userCertificate)
                    || key.equals(ContactConstants.A_userSMIMECertificate)) {
                    // Append these attributes value without attempting encoding
                    // since its already base64 encoded
                    String ldapAttr = ldapAttrs[ldapAttrs.length - 1];
                    addLDIFEntry(ldapAttr, value, sb, true);
                    return true;
                } else if (key.equals(ContactConstants.A_workCountry)) {
                    String encodedValue = encodeValue(value);
                    String workCountryAttributeName = getWorkCountryAttributeName();
                    if (workCountryAttributeName == null) {
                        addLDIFEntry(ldapAttrs[ldapAttrs.length - 1], encodedValue, sb,
                            !encodedValue.equals(value));
                    } else {
                        addLDIFEntry(workCountryAttributeName, encodedValue, sb,
                            !encodedValue.equals(value));
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private static String getContactKey(String[] contactAttrs) {
        String key = null;
        List<String> contactAttrList = Arrays.asList(contactAttrs);
        if (contactAttrList.contains(ContactConstants.A_lastName)) {
            key = ContactConstants.A_lastName;
        } else if (contactAttrList.contains(ContactConstants.A_email)) {
            key = ContactConstants.A_email;
        } else if (contactAttrList.contains(ContactConstants.A_jobTitle)) {
            key = ContactConstants.A_jobTitle;
        } else if (contactAttrList.contains(ContactConstants.A_workStreet)) {
            key = ContactConstants.A_workStreet;
        } else if (contactAttrList.contains(ContactConstants.A_workCity)) {
            key = ContactConstants.A_workCity;
        } else if (contactAttrList.contains(ContactConstants.A_workState)) {
            key = ContactConstants.A_workState;
        } else if (contactAttrList.contains(ContactConstants.A_workPostalCode)) {
            key = ContactConstants.A_workPostalCode;
        } else if (contactAttrList.contains(ContactConstants.A_workCountry)) {
            key = ContactConstants.A_workCountry;
        } else if (contactAttrList.contains(ContactConstants.A_workPhone)) {
            key = ContactConstants.A_workPhone;
        } else if (contactAttrList.contains(ContactConstants.A_workFax)) {
            key = ContactConstants.A_workFax;
        } else if (contactAttrList.contains(ContactConstants.A_mobilePhone)) {
            key = ContactConstants.A_mobilePhone;
        } else if (contactAttrList.contains(ContactConstants.A_homePhone)) {
            key = ContactConstants.A_homePhone;
        } else if (contactAttrList.contains(ContactConstants.A_pager)) {
            key = ContactConstants.A_pager;
        } else if (contactAttrList.contains(ContactConstants.A_company)) {
            key = ContactConstants.A_company;
        } else if (contactAttrList.contains(ContactConstants.A_notes)) {
            key = ContactConstants.A_notes;
        } else if (contactAttrList.contains(ContactConstants.A_office)) {
            key = ContactConstants.A_office;
        } else if (contactAttrList.contains(ContactConstants.A_department)) {
            key = ContactConstants.A_department;
        } else if (contactAttrList.contains(ContactConstants.A_firstName)) {
            key = ContactConstants.A_firstName;
        } else if (contactAttrList.contains(ContactConstants.A_userCertificate)) {
            key = ContactConstants.A_userCertificate;
        } else if (contactAttrList.contains(ContactConstants.A_userSMIMECertificate)) {
            key = ContactConstants.A_userSMIMECertificate;
        }
        return key;
    }

    protected void addLDIFEntry(String name, String value, StringBuilder sb, boolean isEncoded) {
        sb.append(name);
        // base64 encoded attribute appended with double colon
        String separator = isEncoded ? ":: " : ": ";
        sb.append(separator);
        sb.append(value);
        sb.append(NEW_LINE);
    }

    protected void encodeAndAddLDIFEntry(String attrName, String attrValue, StringBuilder sb) {
        if (!StringUtil.isNullOrEmpty(attrName) && !StringUtil.isNullOrEmpty(attrValue)) {
            String encodedValue = encodeValue(attrValue);
            addLDIFEntry(attrName, encodedValue, sb, !encodedValue.equals(attrValue));
        }
    }

    private enum NonSafeChars {
        NUL(0), LF(10), CR(13);
        private int value;
        private NonSafeChars(int value) {
            this.value = value;
        }
    }

    private enum NonSafeInitChars {
        NUL(0), LF(10), CR(13), SPACE(32), COLON(58), LESSTHAN(60);
        private int value;
        private NonSafeInitChars(int value) {
            this.value = value;
        }
    }

    private static boolean isNonSafeInitChar(int v) {
        for (NonSafeInitChars c : NonSafeInitChars.values()) {
            if (c.value == v) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNonSafeChar(int v) {
        for (NonSafeChars c : NonSafeChars.values()) {
            if (c.value == v) {
                return true;
            }
        }
        return false;
    }

    private static String encodeValue(String value) {
        if (StringUtil.isNullOrEmpty(value)) {
            return null;
        }
        String encodedValue = value;
        if (value.chars().allMatch(c -> c < 128)) {
            boolean containsCharsOtherThanSAFE_CHAR = false;
            boolean beginsWithCharOtheThanSAFE_INIT_CHAR = false;
            boolean isFirstChar = true;
            char[] chars = value.toCharArray();
            char lastChar = chars[chars.length - 1];
            if (lastChar != NonSafeInitChars.SPACE.value) {
                for (int i = 0; i < chars.length; i++) {
                    char c = chars[i];
                    if (isFirstChar) {
                        if (isNonSafeInitChar(c)) {
                            beginsWithCharOtheThanSAFE_INIT_CHAR = true;
                            break;
                        }
                        isFirstChar = false;
                    }
                    if (isNonSafeChar(c)) {
                        containsCharsOtherThanSAFE_CHAR = true;
                        break;
                    }
                }
                // if value contains characters other than those defined
                // as SAFE-CHAR, or begins with a character other than
                // those defined as SAFE-INIT-CHAR, MUST be base-64
                // encoded.
                if (containsCharsOtherThanSAFE_CHAR
                    || beginsWithCharOtheThanSAFE_INIT_CHAR) {
                    encodedValue = new String(Base64.encodeBase64(value.getBytes()));
                }
            } else {
                // Values that end with SPACE SHOULD be base-64 encoded
                encodedValue = new String(Base64.encodeBase64(value.getBytes()));
            }
        } else {
            // if value contains all Non-ASCII characters, MUST be
            // base-64 encoded.
            encodedValue = new String(Base64.encodeBase64(value.getBytes()));
        }
        return encodedValue;
    }

    private static String extractUid(String value) {
        if (StringUtil.isNullOrEmpty(value)) {
            return null;
        }
        ParsedAddress in = new ParsedAddress(value, null).parse();
        return in.firstName;
    }

    private static String[] extractDc(String value) {
        if (StringUtil.isNullOrEmpty(value)) {
            return null;
        }
        String[] dc = null;
        int lastIndexOfAt = value.lastIndexOf("@");
        String domain;

        if (lastIndexOfAt != -1) {
            domain = value.substring(lastIndexOfAt + 1, value.length());
            dc = domain.split("\\.");
        }
        return dc;
    }

    protected String getCommonName(Map<String, String> contact) {
        return contact.get(ContactConstants.A_fullName);
    }

    protected String getDistinguishedName(Map<String, String> contact) {
        String email = contact.get(ContactConstants.A_email);
        if (StringUtil.isNullOrEmpty(email)) {
            return "";
        }
        String uid = null;
        String[] dc = null;
        List<String> dnElements = new ArrayList<String>();
        uid = extractUid(email);
        dc = extractDc(email);

        if (uid != null) {
            dnElements.add(LdapConstants.ATTR_uid + "=" + uid);
        }
        dnElements.add(LdapConstants.ATTR_ou + "=" + LdapConstants.PEOPLE);
        if (dc != null) {
            for (String dcPart : dc) {
                dnElements.add(LdapConstants.ATTR_dc + "=" + dcPart);
            }
        }
        return StringUtils.join(dnElements.toArray(), ",");
    }

    protected void addObjectClass(StringBuilder sb) {
        addLDIFEntry(LdapConstants.ATTR_objectClass, LdapObjectClass.ZIMBRA_DEFAULT_PERSON_OC, sb, false);
    }

    protected String getWorkCountryAttributeName() {
        return null;
    }
}