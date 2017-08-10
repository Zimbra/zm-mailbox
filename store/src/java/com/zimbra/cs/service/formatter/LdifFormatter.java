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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.servlet.ServletException;

import org.apache.commons.codec.binary.Base64;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.cs.mailbox.MailItem;
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
        Iterator<? extends MailItem> contacts = null;
        StringBuilder sb = new StringBuilder();
        sb.append("version: 1"); // ldif version
        sb.append(NEW_LINE);
        try {
            contacts = getMailItems(context, -1, -1, Integer.MAX_VALUE);
            ArrayList<Map<String, String>> allContacts = new ArrayList<Map<String, String>>();
            HashSet<String> fields = new HashSet<String>();
            UserServletUtil.populateContactFields(contacts, context.targetMailbox,
                context.opContext, allContacts, fields);
            ArrayList<String> allFields = new ArrayList<String>();
            allFields.addAll(fields);
            Collections.sort(allFields);
            for (Map<String, String> contactMap : allContacts) {
                toLDIFContact(allFields, contactMap, sb);
                sb.append(NEW_LINE);
            }
        } finally {
            if (contacts instanceof QueryResultIterator)
                ((QueryResultIterator) contacts).finished();
        }
        String filename = context.itemPath;
        if (filename == null || filename.length() == 0)
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

    public void toLDIFContact(List<String> fields, Map<String, String> contact, StringBuilder sb) {
        int NUL = 0;
        int LF = 10;
        int CR = 13;
        int SPACE = 32;
        int COLON = 58;
        int LESSTHAN = 60;
        for (String field : fields) {
            String value = contact.get(field);
            if (value != null) {
                sb.append(field);
                boolean valueBase64Encoded = false;
                if (ContactConstants.A_userCertificate.equals(field)
                    || ContactConstants.A_userSMIMECertificate.equals(field)) {
                    // base64 encoded attribute appended with double colon
                    sb.append(":: ");
                    sb.append(value);
                    sb.append(NEW_LINE);
                    return;
                }
                if (value.chars().allMatch(c -> c < 128)) {
                    boolean containsCharsOtherThanSAFE_CHAR = false;
                    boolean beginsWithCharOtheThanSAFE_INIT_CHAR = false;
                    boolean isFirstChar = true;
                    char[] chars = value.toCharArray();
                    char lastChar = chars[chars.length - 1];
                    if (lastChar != SPACE) {
                        for (int i = 0; i < chars.length; i++) {
                            char c = chars[i];
                            if (isFirstChar) {
                                if (c == NUL || c == LF || c == CR || c == SPACE || c == COLON
                                    || c == LESSTHAN) {
                                    beginsWithCharOtheThanSAFE_INIT_CHAR = true;
                                    break;
                                }
                                isFirstChar = false;
                            }
                            if (c == NUL || c == LF || c == CR) {
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
                            value = new String(Base64.encodeBase64(value.getBytes()));
                            valueBase64Encoded = true;
                        }
                    } else {
                        // Values that end with SPACE SHOULD be base-64 encoded
                        value = new String(Base64.encodeBase64(value.getBytes()));
                        valueBase64Encoded = true;
                    }
                } else {
                    // if value contains all Non-ASCII characters, MUST be
                    // base-64 encoded.
                    value = new String(Base64.encodeBase64(value.getBytes()));
                    valueBase64Encoded = true;
                }
                if (valueBase64Encoded) {
                    // base64 encoded attribute appended with double colon
                    sb.append(":: ");
                } else {
                    sb.append(": ");
                }
                sb.append(value);
                sb.append(NEW_LINE);
            }
        }
    }
}