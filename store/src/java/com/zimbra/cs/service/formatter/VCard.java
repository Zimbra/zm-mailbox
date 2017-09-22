/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import net.fortuna.ical4j.data.FoldingWriter;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.QuotedPrintableCodec;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Contact.Attachment;
import com.zimbra.cs.mailbox.Contact.DerefGroupMembersOption;
import com.zimbra.cs.mailbox.VCardParamsAndValue;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.cs.util.Zimbra;

public class VCard {

    public String uid;
    public String fn;
    private final String formatted;
    public Map<String, String> fields;
    public List<Attachment> attachments;

    private VCard(String xfn, String xformatted, Map<String, String> xfields, List<Attachment> xattachments, String xuid) {
        fn = xfn;  formatted = xformatted;  fields = xfields;  attachments = xattachments;  uid = xuid;
    }

    public ParsedContact asParsedContact() throws ServiceException {
        return new ParsedContact(fields, attachments);
    }


    private static final Set<String> PROPERTY_NAMES =
            new HashSet<String>(Arrays.asList(
                    "BEGIN",
                    "FN",
                    "N",
                    "NICKNAME",
                    "PHOTO",
                    "KEY",
                    "BDAY",
                    "ADR",
                    "TEL",
                    "EMAIL",
                    "URL",
                    "ORG",
                    "TITLE",
                    "NOTE",
                    "AGENT",
                    "END",
                    "UID",
                    "X-ZIMBRA-IMADDRESS1",
                    "X-ZIMBRA-IMADDRESS2",
                    "X-ZIMBRA-IMADDRESS3",
                    "X-ZIMBRA-ANNIVERSARY",
                    "X-ZIMBRA-MAIDENNAME"
    ));

    static final Map<String, String> PARAM_ABBREVIATIONS = new HashMap<String, String>();
        static {
            PARAM_ABBREVIATIONS.put("BASE64", "ENCODING=B");
            PARAM_ABBREVIATIONS.put("QUOTED-PRINTABLE", "ENCODING=QUOTED-PRINTABLE");
            PARAM_ABBREVIATIONS.put("HOME",   "TYPE=HOME");
            PARAM_ABBREVIATIONS.put("WORK",   "TYPE=WORK");
            PARAM_ABBREVIATIONS.put("FAX",    "TYPE=FAX");
            PARAM_ABBREVIATIONS.put("CELL",   "TYPE=CELL");
            PARAM_ABBREVIATIONS.put("PAGER",  "TYPE=PAGER");
            PARAM_ABBREVIATIONS.put("CAR",    "TYPE=CAR");
        }

    private enum Encoding { NONE, B, Q }

    private static class VCardProperty {
        private String group;
        private String name;
        private final Set<String> params = new HashSet<String>();
        private String charset;
        private Encoding encoding = Encoding.NONE;
        private String value;
        private boolean isEmpty;

        boolean isEmpty()       { return isEmpty; }
        Encoding getEncoding()  { return encoding; }
        boolean containsParam(String param) {
            String ucaseParam = param.toUpperCase();
            for (String par : params) {
                if (par.equalsIgnoreCase(ucaseParam)) {
                    return true;
                }
            }
            return false;
        }

        String getParamValue(String pname) {
            pname = pname.toUpperCase() + '=';
            for (String param : params) {
                if (param.startsWith(pname)) {
                    return param.substring(pname.length());
                }
            }
            return null;
        }

        private void reset() {
            group = name = value = null;
            charset = MimeConstants.P_CHARSET_UTF8;
            params.clear();
            encoding = Encoding.NONE;
            isEmpty = false;
        }

        String parse(StringBuilder line) throws ServiceException {
            reset();
            isEmpty = isAllWhitespace(line);
            if (isEmpty) {
                return "";
            }

            // find the delimiter between property name and property value
            int colon = line.indexOf(":");
            if (colon == -1) {
                throw ServiceException.PARSE_ERROR("missing ':' in line " + shortFormForLogging(line), null);
            }
            value = line.substring(colon + 1);

            // find the property name, stripping off any groups (e.g. "FOO.ADR")
            int i, start;
            char c = '\0';
            for (i = start = 0; i < colon; i++) {
                if ((c = line.charAt(i)) == '.') {
                    start = i + 1;
                } else if (c == ';') {
                    break;
                }
            }
            if (start != 0) {
                group = line.substring(0, start - 1);
            }
            name = line.substring(start, i).trim().toUpperCase();

            // get the property's parameters
            String pname = null;
            while (i < colon) {
                for (start = ++i; i < colon; i++) {
                    if ((c = line.charAt(i)) == ';')     break;
                    else if (c == ',' && pname != null)  break;
                    else if (c == '=' && pname == null && i > start) {
                        pname = line.substring(start, i).toUpperCase();
                        start = i + 1;
                    }
                }
                String pval = line.substring(start, i);
                if (!pval.equals("")) {
                    String pvalUCase = pval.toUpperCase();
                    String param = (pname != null ? pname + '=' + pval : PARAM_ABBREVIATIONS.get(pvalUCase));
                    if (param != null) {
                        String paramUCase = param.toUpperCase();
                        if (paramUCase.equals("ENCODING=B")) {
                            encoding = Encoding.B;
                        } else if (paramUCase.equals("ENCODING=BASE64")) {
                            encoding = Encoding.B;
                        } else if (paramUCase.equals("ENCODING=QUOTED-PRINTABLE")) {
                            encoding = Encoding.Q;
                        } else if (pname != null && pname.equals("CHARSET")) {
                            charset = pval;
                        } else {
                            params.add(param);
                        }
                    }
                }

                if (c == ';') {
                    pname = null;
                }
            }

            return name;
        }

        String getGroup() {
            return group;
        }

        String getValue() {
            // if it's a 2.1 vCard, decode the property value if necessary
            try {
                if (encoding == Encoding.B) {
                    byte[] encoded = value.getBytes();
                    if (Base64.isBase64(encoded))
                        value = new String(Base64.decodeBase64(encoded), charset);
                } else if (encoding == Encoding.Q) {
                    value = new QuotedPrintableCodec(charset).decode(value);
                }
                encoding = Encoding.NONE;
            } catch (Exception ignored) { }
            return value;
        }

        byte[] getDecoded() {
            byte[] encoded = value.getBytes();
            try {
                if (encoding == Encoding.B && Base64.isBase64(encoded))
                    encoded = Base64.decodeBase64(encoded);
                encoding = Encoding.NONE;
            } catch (Exception ignored) { }
            return encoded;
        }
    }

    public static List<VCard> parseVCard(String vcard) throws ServiceException {
        List<VCard> cards = new ArrayList<VCard>();

        Map<String, String> fields = new HashMap<String, String>();
        ListMultimap<String, VCardParamsAndValue> xprops = ArrayListMultimap.create();
        List<Attachment> attachments = new ArrayList<Attachment>();

        VCardProperty vcprop = new VCardProperty();
        int depth = 0;
        int cardstart = 0;
        String uid = null;
        StringBuilder line = new StringBuilder(256);
        for (int start, pos = 0, limit = vcard.length(); pos < limit;) {
            // unfold the next line in the vcard
            line.setLength(0);
            String name = null;
            String value;
            int linestart = pos;
            boolean folded = true;
            do {
                start = pos;
                while (pos < limit && vcard.charAt(pos) != '\r' && vcard.charAt(pos) != '\n') {
                    pos++;
                }
                line.append(vcard.substring(start, pos));
                if (pos < limit) {
                    if (pos < limit && vcard.charAt(pos) == '\r')  pos++;
                    if (pos < limit && vcard.charAt(pos) == '\n')  pos++;
                }
                if (pos < limit && (vcard.charAt(pos) == ' ' || vcard.charAt(pos) == '\t')) {
                    pos++;
                } else {
                    name = vcprop.parse(line);
                    if (    (vcprop.getEncoding() != Encoding.Q) ||
                            !((line.length() > 0) && ('=' == (line.charAt(line.length() - 1))))) {
                        folded = false;
                    }
                }
            } while (folded);
            if (vcprop.isEmpty()) {
                continue;
            }

            if (Strings.isNullOrEmpty(name)) {
                throw ServiceException.PARSE_ERROR("missing property name in line " + shortFormForLogging(line), null);
            } else if (name.equals("VERSION") || name.equals("REV") || name.equals("PRODID")) {
                continue;
            } else if ((name.startsWith("X-") && !name.startsWith("X-ZIMBRA-")) ||
                        (!PROPERTY_NAMES.contains(name) /* Assume iana-token */)) {
                VCardParamsAndValue ppav;
                if (Encoding.B.equals(vcprop.encoding)) {
                    // Keep value encoded as don't trust binary data stored in metadata
                    Set<String> params = vcprop.params;
                    params.add("ENCODING=B");
                    if ((vcprop.charset != null) && (!MimeConstants.P_CHARSET_UTF8.equals(vcprop.charset))) {
                        params.add(String.format("CHARSET=%s", vcprop.charset));
                    }
                    ppav = new VCardParamsAndValue(vcprop.value, vcprop.params);
                } else {
                    ppav = new VCardParamsAndValue(vcfDecode(vcprop.getValue()), vcprop.params);
                }
                // handle multiple occurrences of xprops with the same key
                String group = vcprop.getGroup();
                String key = (group == null) ? name : group + "." + name;
                xprops.put(key, ppav);
            } else if (name.equals("BEGIN")) {
                if (++depth == 1) {
                    // starting a top-level vCard; reset state
                    fields = new HashMap<String, String>();
                    xprops = ArrayListMultimap.create();
                    attachments = new ArrayList<Attachment>();
                    cardstart = linestart;
                    uid = null;
                }
                continue;
            } else if (name.equals("END")) {
                if (depth > 0 && depth-- == 1) {
                    if (!xprops.isEmpty()) {
                        fields.put(ContactConstants.A_vCardXProps, Contact.encodeUnknownVCardProps(xprops));
                    }

                    // finished a vCard; add to list if non-empty
                    if (!fields.isEmpty()) {
                        Contact.normalizeFileAs(fields);
                        cards.add(new VCard(fields.get(ContactConstants.A_fullName), vcard.substring(cardstart, pos),
                                fields, attachments, uid));
                    }
                }
                continue;
            } else if (depth <= 0) {
                continue;
            } else if (name.equals("AGENT")) {
                // catch AGENT on same line as BEGIN block when rest of AGENT is not on the same line
                if (vcprop.getValue().trim().toUpperCase().matches("BEGIN\\s*:\\s*VCARD"))
                    depth++;
                continue;
            }

            if (vcprop.getEncoding() == Encoding.B && !vcprop.containsParam("VALUE=URI")) {
                if (name.equals("PHOTO")) {
                    String suffix = vcprop.getParamValue("TYPE").toUpperCase();
                    String ctype = null;
                    if (!Strings.isNullOrEmpty(suffix)) {
                        ctype = "image/" + suffix.toLowerCase();
                        suffix = '.' + suffix;
                    }
                    attachments.add(new Attachment(vcprop.getDecoded(), ctype, "image", "image" + suffix));
                    continue;
                }
                if (name.equals("KEY")) {
                    String encoded = new String(Base64.encodeBase64Chunked(vcprop.getDecoded()));
                    fields.put(ContactConstants.A_userCertificate, encoded);
                    continue;
                }
            }

            value = vcprop.getValue();

            // decode the property's value and assign to the appropriate contact field(s)
            if (name.equals("FN"))             addField(ContactConstants.A_fullName, vcfDecode(value), "altFullName", 2, fields);
            else if (name.equals("N"))         decodeStructured(value, NAME_FIELDS, fields);
            else if (name.equals("NICKNAME"))  // TODO: VCARD 4 NICKNAME is multi-valued (COMMA separated)
                                               //       It is treated as a single value here
                                               addField(ContactConstants.A_nickname, vcfDecode(value), "altNickName", 2, fields);
            else if (name.equals("PHOTO"))     fields.put(ContactConstants.A_image, vcfDecode(value)); // Assumption: Do not want multiple photos.
            else if (name.equals("BDAY"))      addField(ContactConstants.A_birthday, vcfDecode(value), null, 2, fields);
            else if (name.equals("ADR"))       decodeAddress(value, vcprop, fields);
            else if (name.equals("TEL"))       decodeTelephone(value, vcprop, fields);
            else if (name.equals("URL"))       decodeURL(value, vcprop, fields);
            else if (name.equals("ORG"))       decodeStructured(value, ORG_FIELDS, fields);
            else if (name.equals("TITLE"))     addField(ContactConstants.A_jobTitle, vcfDecode(value), "altJobTitle", 2, fields);
            else if (name.equals("NOTE"))      addField(ContactConstants.A_notes, vcfDecode(value), null, 2, fields);
            else if (name.equals("EMAIL"))     addField(ContactConstants.A_email, vcfDecode(value), null, 2, fields);
            else if (name.equals("X-ZIMBRA-MAIDENNAME"))   fields.put(ContactConstants.A_maidenName, vcfDecode(value));
            else if (name.startsWith("X-ZIMBRA-IMADDRESS"))    addField("imAddress", true, vcfDecode(value), null, 1, fields);
            else if (name.equals("X-ZIMBRA-ANNIVERSARY"))  addField(ContactConstants.A_anniversary, vcfDecode(value), null, 2, fields);
            else if (name.equals("UID")) uid = value;
        }

        return cards;
    }

    static String shortFormForLogging(StringBuilder line) {
        if (line.length() <= 70) {
            return line.toString();
        } else {
            return line.substring(0, 70) + "...";
        }
    }

    static boolean isAllWhitespace(StringBuilder line) {
        int length = line.length();
        for (int ndx = 0; ndx < length; ndx++) {
            if (!Character.isWhitespace(line.charAt(ndx))) {
                return false;
            }
        }
        return true;
    }


    private static void removeField(String firstKey, boolean skipFirstKey, int firstSuffix, Map<String, String> fields) {
        if (!skipFirstKey) {
            fields.remove(firstKey);
        }
        for (int suffix = firstSuffix; suffix < 20; suffix++) {
            String trialKey = new StringBuilder(firstKey).append(String.valueOf(suffix)).toString();
            if (fields.remove(trialKey) == null) {
                break;
            }
        }
    }

    /*
     * Removes all the fields exposed through VCard.
     */

    private static void removeVCardFields(Map<String, String> fields) {
        fields.remove(ContactConstants.A_fullName);
        fields.remove(ContactConstants.A_company);
        fields.remove(ContactConstants.A_lastName);
        fields.remove(ContactConstants.A_firstName);
        fields.remove(ContactConstants.A_middleName);
        fields.remove(ContactConstants.A_namePrefix);
        fields.remove(ContactConstants.A_nameSuffix);
        fields.remove(ContactConstants.A_nickname);
        fields.remove(ContactConstants.A_image);
        fields.remove(ContactConstants.A_birthday);
        fields.remove(ContactConstants.A_company);
        fields.remove(ContactConstants.A_department);
        fields.remove(ContactConstants.A_jobTitle);
        fields.remove(ContactConstants.A_notes);
        fields.remove(ContactConstants.A_type);
        fields.remove(ContactConstants.A_groupMember);
        fields.remove(ContactConstants.A_vCardXProps);

        removeField(ContactConstants.A_homeStreet, false, 2, fields);
        removeField(ContactConstants.A_homeCity, false, 2, fields);
        removeField(ContactConstants.A_homeState, false, 2, fields);
        removeField(ContactConstants.A_homePostalCode, false, 2, fields);
        removeField(ContactConstants.A_homeCountry, false, 2, fields);
        removeField(ContactConstants.A_workStreet, false, 2, fields);
        removeField(ContactConstants.A_workCity, false, 2, fields);
        removeField(ContactConstants.A_workState, false, 2, fields);
        removeField(ContactConstants.A_workPostalCode, false, 2, fields);
        removeField(ContactConstants.A_workCountry, false, 2, fields);
        removeField(ContactConstants.A_otherStreet, false, 2, fields);
        removeField(ContactConstants.A_otherCity, false, 2, fields);
        removeField(ContactConstants.A_otherState, false, 2, fields);
        removeField(ContactConstants.A_otherPostalCode, false, 2, fields);
        removeField(ContactConstants.A_otherCountry, false, 2, fields);

        removeField(ContactConstants.A_carPhone, false, 2, fields);
        removeField(ContactConstants.A_homeFax, false, 2, fields);
        removeField(ContactConstants.A_homePhone, false, 2, fields);
        removeField(ContactConstants.A_mobilePhone, false, 2, fields);
        removeField(ContactConstants.A_otherFax, false, 2, fields);
        removeField(ContactConstants.A_otherPhone, false, 2, fields);
        removeField(ContactConstants.A_pager, false, 2, fields);
        removeField(ContactConstants.A_workFax, false, 2, fields);
        removeField(ContactConstants.A_workPhone, false, 2, fields);
        removeField(ContactConstants.A_email, false, 2, fields);

        removeField(ContactConstants.A_homeURL, false, 2, fields);
        removeField(ContactConstants.A_otherURL, false, 2, fields);
        removeField(ContactConstants.A_workURL, false, 2, fields);

        removeField("workEmail", true, 1, fields);
    }

    /*
     * Merge back the fields that are not exposed in VCard.
     */

    public void merge(Contact existingContact) {
        Map<String, String> existingFields = existingContact.getAllFields();
        removeVCardFields(existingFields);
        for (Entry<String, String> entry : existingFields.entrySet()) {
            if (!fields.containsKey(entry.getKey())) {
                fields.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void addField(String firstKey, String value, String customPrefix,
            int firstSuffix, Map<String, String> fields) {
        addField(firstKey, false, value, customPrefix, firstSuffix, fields);
    }

    private static void addField(String firstKey, boolean skipFirstKey, String value, String customPrefix,
            int firstSuffix, Map<String, String> fields) {
        if (!skipFirstKey && !fields.containsKey(firstKey)) {
            fields.put(firstKey, value);
        } else {
            if (customPrefix == null) customPrefix = firstKey;
            for (int suffix = firstSuffix;suffix < 20 ;suffix++) {
                String trialKey = new StringBuilder(customPrefix).append(String.valueOf(suffix)).toString();
                if (!fields.containsKey(trialKey)) {
                    fields.put(trialKey, value);
                    break;
                }
            }
        }
    }

    private static void decodeTelephone(String value, VCardProperty vcprop, Map<String, String> fields) {
        value = vcfDecode(value);
        if (vcprop.containsParam("TYPE=CAR"))         { addField(ContactConstants.A_carPhone, value, null, 2, fields); return; }
        else if (vcprop.containsParam("TYPE=CELL"))   { addField(ContactConstants.A_mobilePhone, value, null, 2, fields); return; }
        else if (vcprop.containsParam("TYPE=PAGER"))  { addField(ContactConstants.A_pager, value, null, 2, fields); return; }

        boolean home = vcprop.containsParam("TYPE=HOME"), work = vcprop.containsParam("TYPE=WORK");
        boolean fax = vcprop.containsParam("TYPE=FAX"), voice = vcprop.containsParam("TYPE=VOICE");
        if (home) {
            if (fax)  addField(ContactConstants.A_homeFax, value, null, 2, fields);
            if (voice || !fax) addField(ContactConstants.A_homePhone, value, null, 2, fields);
        }
        if (work) {
            if (fax)  addField(ContactConstants.A_workFax, value, null, 2, fields);
            if (voice || !fax) addField(ContactConstants.A_workPhone, value, null, 2, fields);
        }
        if (!home && !work) {
            if (fax)  addField(ContactConstants.A_otherFax, value, null, 2, fields);
            if (voice || !fax) addField(ContactConstants.A_otherPhone, value, null, 2, fields);
        }
    }

    private static void decodeAddress(String value, VCardProperty vcprop, Map<String, String> fields) {
        boolean home = vcprop.containsParam("TYPE=HOME"), work = vcprop.containsParam("TYPE=WORK");
        if (home)            decodeStructured(value, ADR_HOME_FIELDS, fields);
        if (work)            decodeStructured(value, ADR_WORK_FIELDS, fields);
        if (!home && !work)  decodeStructured(value, ADR_OTHER_FIELDS, fields);
    }

    private static void decodeURL(String value, VCardProperty vcprop, Map<String, String> fields) {
        boolean home = vcprop.containsParam("TYPE=HOME"), work = vcprop.containsParam("TYPE=WORK");
        if (home)            addField(ContactConstants.A_homeURL, value, null, 2, fields);
        if (work)            addField(ContactConstants.A_workURL, value, null, 2, fields);
        if (!home && !work)  addField(ContactConstants.A_otherURL, value, null, 2, fields);
    }

    private static final String[] NAME_FIELDS = new String[] {
        ContactConstants.A_lastName, ContactConstants.A_firstName, ContactConstants.A_middleName, ContactConstants.A_namePrefix, ContactConstants.A_nameSuffix
    };
    private static final String[] ADR_HOME_FIELDS = new String[] {
        ContactConstants.A_homeStreet, ContactConstants.A_homeStreet, ContactConstants.A_homeStreet,
        ContactConstants.A_homeCity, ContactConstants.A_homeState, ContactConstants.A_homePostalCode, ContactConstants.A_homeCountry
    };
    private static final String[] ADR_WORK_FIELDS = new String[] {
        ContactConstants.A_workStreet, ContactConstants.A_workStreet, ContactConstants.A_workStreet,
        ContactConstants.A_workCity, ContactConstants.A_workState, ContactConstants.A_workPostalCode, ContactConstants.A_workCountry
    };
    private static final String[] ADR_OTHER_FIELDS = new String[] {
        ContactConstants.A_otherStreet, ContactConstants.A_otherStreet, ContactConstants.A_otherStreet,
        ContactConstants.A_otherCity, ContactConstants.A_otherState, ContactConstants.A_otherPostalCode, ContactConstants.A_otherCountry
    };
    private static final String[] ORG_FIELDS = new String[] {
        ContactConstants.A_company, ContactConstants.A_department
    };

    private static void decodeStructured(String value, String[] keys, Map<String, String> fields) {
        // Support user defined extra fields.  If, for instance, a home address has previously
        // been defined, can add a second one where all the constituent properties have the
        // suffix "2" and so on.
        String suffix = "";
        int suffixInt = 1;
        boolean keyAvailable;
        do {
            keyAvailable = true;
            for (String key : keys) {
                String trialKey = new StringBuilder(key).append(suffix).toString();
                if (fields.containsKey(trialKey)) {
                    keyAvailable = false;
                    break;
                }
            }
            if (!keyAvailable) {
                suffixInt++;
                suffix = String.valueOf(suffixInt);
            }
        } while (!keyAvailable && (suffixInt < 20));

        for (int i = 0, start = 0, f = 0, len = value.length(); i < len && f < keys.length; start = ++i, f++) {
            char c;
            for (boolean escaped = false; i < len && ((c = value.charAt(i)) != ';' || escaped); i++)
                escaped = !escaped && c == '\\';
            if (i > start && keys[f] != null) {
                String keyToUse = new StringBuilder(keys[f]).append(suffix).toString();
                String val = fields.get(keyToUse);
                if (val == null) {
                    fields.put(keyToUse, vcfDecode(value.substring(start, i)));
                } else {
                    fields.put(keyToUse, val + ", " + vcfDecode(value.substring(start, i)));
                }
            }
        }
    }

    private static String vcfDecode(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = 0, len = value.length(); i < len; i++) {
            char c = value.charAt(i);
            if (c == '\\' && !escaped) {
                escaped = true;
            } else if (!escaped) {
                sb.append(c);
            } else {
                if (c == 'n' || c == 'N') {
                    sb.append('\n');
                } else if (c == 't' || c == 'T') {
                    sb.append('\t');
                } else {
                    sb.append(c);
                }
                escaped = false;
            }
        }
        return sb.toString();
    }


    public static VCard formatContact(Contact con) {
        return formatContact(con, null, false);
    }

    public static VCard formatContact(Contact con, Collection<String> vcattrs, boolean includeXProps) {
        return formatContact(con, vcattrs, includeXProps, true);
    }

    public static VCard formatContact(Contact con, Collection<String> vcattrs, boolean includeXProps,
            boolean includeZimbraXProps) {
        Map<String, String> fields = con.getFields();
        List<Attachment> attachments = con.getAttachments();
        List<String> emails = con.getEmailAddresses(DerefGroupMembersOption.NONE);

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD\r\n");

        // This is from RFC 2426 (vCard version 3.0) Section 1:
        // Profile special notes: The vCard object MUST contain the FN, N and VERSION types.

        if (vcattrs == null || vcattrs.contains("VERSION"))
            sb.append("VERSION:3.0\r\n");

        // FN is a mandatory component of the vCard -- try our best to find or generate one
        String fn = fields.get(ContactConstants.A_fullName);
        if (vcattrs == null || vcattrs.contains("FN")) {
            if (fn == null || fn.trim().equals(""))
                try { fn = con.getFileAsString(); } catch (ServiceException e) { fn = ""; }
            if (fn.trim().equals("") && !emails.isEmpty())
                fn = emails.get(0);
            if (fn.trim().equals("")) {
                String org = fields.get(ContactConstants.A_company);
                if (org != null && !org.trim().equals("")) {
                    fn = org;
                }
            }
            sb.append("FN:").append(vcfEncode(fn)).append("\r\n");
        }

        if (vcattrs == null || vcattrs.contains("N")) {
            StringBuilder nSb = new StringBuilder();
            nSb.append(vcfEncode(fields.get(ContactConstants.A_lastName))).append(';')
                .append(vcfEncode(fields.get(ContactConstants.A_firstName))).append(';')
                .append(vcfEncode(fields.get(ContactConstants.A_middleName))).append(';')
                .append(vcfEncode(fields.get(ContactConstants.A_namePrefix))).append(';')
                .append(vcfEncode(fields.get(ContactConstants.A_nameSuffix)));
            String n = nSb.toString();
            // N is mandatory according to  RFC 2426 Section 1, so include it even if all components are empty
            // In fact, clients like Mac OS X Mavericks Contacts will just have blank names if it is blank,
            // so, try to avoid that.
            if (";;;;".equals(n)) {
                n = vcfEncode(fn) + ";;;;";
            }
            sb.append("N:").append(n).append("\r\n");
        }

        if (vcattrs == null || vcattrs.contains("NICKNAME"))
            encodeField(sb, "NICKNAME", fields.get(ContactConstants.A_nickname));
        if (vcattrs == null || vcattrs.contains("PHOTO"))
            encodeField(sb, "PHOTO;VALUE=URI", fields.get(ContactConstants.A_image));
        if (vcattrs == null || vcattrs.contains("BDAY")) {
            String bday = fields.get(ContactConstants.A_birthday);
            if (bday != null) {
                Date date = DateUtil.parseDateSpecifier(bday);
                if (date != null)
                    sb.append("BDAY;VALUE=date:").append(new SimpleDateFormat("yyyy-MM-dd").format(date)).append("\r\n");
            }
        }

        if (vcattrs == null || vcattrs.contains("ADR")) {
            encodeAddress(sb, "home,postal,parcel", ContactConstants.A_homeStreet,
                    ContactConstants.A_homeCity, ContactConstants.A_homeState,
                    ContactConstants.A_homePostalCode, ContactConstants.A_homeCountry, 2, fields);
            encodeAddress(sb, "work,postal,parcel", ContactConstants.A_workStreet,
                    ContactConstants.A_workCity, ContactConstants.A_workState,
                    ContactConstants.A_workPostalCode, ContactConstants.A_workCountry, 2, fields);
            encodeAddress(sb, "postal,parcel", ContactConstants.A_otherStreet,
                    ContactConstants.A_otherCity, ContactConstants.A_otherState,
                    ContactConstants.A_otherPostalCode, ContactConstants.A_otherCountry, 2, fields);
        }

        if (vcattrs == null || vcattrs.contains("TEL")) {
            // omitting callback phone for now
            encodePhone(sb, "car,voice", ContactConstants.A_carPhone, 2, fields);
            encodePhone(sb, "home,fax", ContactConstants.A_homeFax, 2, fields);
            encodePhone(sb, "home,voice", ContactConstants.A_homePhone, 2, fields);
            encodePhone(sb, "cell,voice", ContactConstants.A_mobilePhone, 2, fields);
            encodePhone(sb, "fax", ContactConstants.A_otherFax, 2, fields);
            encodePhone(sb, "voice", ContactConstants.A_otherPhone, 2, fields);
            encodePhone(sb, "pager", ContactConstants.A_pager, 2, fields);
            encodePhone(sb, "work,fax", ContactConstants.A_workFax, 2, fields);
            encodePhone(sb, "work,voice", ContactConstants.A_workPhone, 2, fields);
        }

        if (vcattrs == null || vcattrs.contains("EMAIL")) {
            encodeField(sb, "EMAIL;TYPE=internet", ContactConstants.A_email, false, 2, fields);
            encodeField(sb, "EMAIL;TYPE=internet", "workEmail", true, 1, fields);
        }

        if (vcattrs == null || vcattrs.contains("URL")) {
            encodeField(sb, "URL;TYPE=home", ContactConstants.A_homeURL, false, 2, fields);
            encodeField(sb, "URL", ContactConstants.A_otherURL, false, 2, fields);
            encodeField(sb, "URL;TYPE=work", ContactConstants.A_workURL, false, 2, fields);
        }

        if (vcattrs == null || vcattrs.contains("ORG")) {
            String org = fields.get(ContactConstants.A_company);
            if (org != null && !org.trim().equals("")) {
                org = vcfEncode(org);
                String dept = fields.get(ContactConstants.A_department);
                if (dept != null && !dept.trim().equals("")) {
                    org += ';' + vcfEncode(dept);
                }
                sb.append("ORG:").append(org).append("\r\n");
            }
        }
        if (vcattrs == null || vcattrs.contains("TITLE"))
            encodeField(sb, "TITLE", fields.get(ContactConstants.A_jobTitle));

        if (vcattrs == null || vcattrs.contains("NOTE"))
            encodeField(sb, "NOTE", fields.get(ContactConstants.A_notes));

        if ((vcattrs == null || vcattrs.contains("PHOTO")) && attachments != null) {
            for (Attachment attach : attachments) {
                try {
                    if (attach.getName().equalsIgnoreCase(ContactConstants.A_image)) {
                        String field = "PHOTO;ENCODING=B";
                        if (attach.getContentType().startsWith("image/")) {
                            // We want just the subtype, ignoring any name etc
                            try {
                                ContentType ct = new ContentType(attach.getContentType());
                                if (ct != null) {
                                    String subType = ct.getSubType();
                                    if (!Strings.isNullOrEmpty(subType)) {
                                        field += ";TYPE=" + ct.getSubType().toUpperCase();
                                    }
                                }
                            } catch (ParseException e) {
                            }
                        }
                        String encoded = new String(Base64.encodeBase64Chunked(attach.getContent())).trim().replace("\r\n", "\r\n ");
                        sb.append(field).append(":\r\n ").append(encoded).append("\r\n");
                    }
                } catch (OutOfMemoryError e) {
                    Zimbra.halt("out of memory", e);
                } catch (Throwable t) {
                    ZimbraLog.misc.info("error fetching attachment content: " + attach.getName(), t);
                }
            }
        }

        if (vcattrs == null || vcattrs.contains("KEY")) {
            String smimeCert = fields.get(ContactConstants.A_userSMIMECertificate);
            if (smimeCert == null) {
                smimeCert = fields.get(ContactConstants.A_userCertificate);
            }
            if (smimeCert != null) {
                smimeCert = smimeCert.trim().replace("\r\n", "\r\n ");
                String field = "KEY;ENCODING=B";
                sb.append(field).append(":\r\n ").append(smimeCert).append("\r\n");
            }
        }

        if (vcattrs == null || vcattrs.contains("CATEGORIES")) {
            String[] tags = con.getTags();
            if (tags.length > 0) {
                StringBuilder sbtags = new StringBuilder();
                for (String tagName : tags) {
                    sbtags.append(sbtags.length() == 0 ? "" : ",").append(vcfEncode(tagName));
                }
                sb.append("CATEGORIES:").append(sbtags).append("\r\n");
            }
        }

        String uid = getUid(con);
        if (vcattrs == null || vcattrs.contains("REV")) {
            sb.append("REV:").append(
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date(con.getDate()))).append("\r\n");
        }
        if (vcattrs == null || vcattrs.contains("UID")) {
            sb.append("UID:").append(uid).append("\r\n");
        }
        // sb.append("MAILER:Zimbra ").append(BuildInfo.VERSION).append("\r\n");
        if ((vcattrs == null && includeZimbraXProps) || (vcattrs != null && vcattrs.contains("X-ZIMBRA-IMADDRESS"))) {
            encodeField(sb, "X-ZIMBRA-IMADDRESS", "imAddress", true, 1, fields);
        }
        if ((vcattrs == null && includeZimbraXProps) || (vcattrs != null && vcattrs.contains("X-ZIMBRA-ANNIVERSARY"))) {
            encodeField(sb, "X-ZIMBRA-ANNIVERSARY", ContactConstants.A_anniversary, false, 2, fields);
        }
        if ((vcattrs == null && includeZimbraXProps) || (vcattrs != null && vcattrs.contains("X-ZIMBRA-MAIDENNAME"))) {
            String maidenName = con.get(ContactConstants.A_maidenName);
            if (maidenName != null)
                sb.append("X-ZIMBRA-MAIDENNAME:").append(maidenName).append("\r\n");
        }
        if (includeXProps) {
            ListMultimap<String, VCardParamsAndValue> unknownVCardProps = con.getUnknownVCardProps();
            for (String key : unknownVCardProps.keySet()) {
                for (VCardParamsAndValue paramsAndValue : unknownVCardProps.get(key)) {
                    StringWriter sw = new StringWriter();
                    try (FoldingWriter writer = new FoldingWriter(sw)) {
                        writer.write(key);
                        String value = paramsAndValue.getValue();
                        Set<String> params = paramsAndValue.getParams();
                        if (!params.isEmpty()) {
                            writer.write(";");
                            writer.write(Joiner.on(";").join(params));
                        }
                        String vcfEncodedValue;
                        if (params.contains("ENCODING=B")) {
                            vcfEncodedValue = value;  // should be raw BASE64
                        } else {
                            vcfEncodedValue = vcfEncode(value);
                        }
                        writer.write(":");
                        writer.write(vcfEncodedValue);
                        writer.write("\r\n");
                        sb.append(sw.toString());
                    } catch (IOException e) {
                         ZimbraLog.misc.debug("Problem with adding property '%s' to VCARD - ignoring", key, e);
                    }
                }
            }
        }
        sb.append("END:VCARD\r\n");
        return new VCard(fn, sb.toString(), fields, attachments, uid);
    }

    public static String getUid(Contact con) {
        String uid = con.get(ContactConstants.A_vCardUID);
        if (uid != null)
            return uid;
        return con.getAccountId() + ":" + con.getId();
    }

    public static String getUrl(Contact con) {
        String url = con.get(ContactConstants.A_vCardURL);
        if (url != null)
            return url.replaceAll("/", "//");
        return getUid(con).replaceAll("/", "//");
    }

    private static void encodeField(StringBuilder sb, String name, String value) {
        if (sb == null || name == null || value == null)
            return;
        sb.append(name).append(':').append(vcfEncode(value)).append("\r\n");
    }

    private static void encodeField(StringBuilder sb, String name, String firstKey, boolean skipFirstKey, int firstSuffix, Map<String, String> fields) {

        if (sb == null || name == null)
            return;

        String value;
        if (!skipFirstKey) {
            value= fields.get(firstKey);
            if  (value == null) {
                return;
            }
            sb.append(name).append(':').append(vcfEncode(value)).append("\r\n");
        }
        for (int suffix = firstSuffix; suffix < 20 ;suffix++) {
            String key = new StringBuilder(firstKey).append(String.valueOf(suffix)).toString();
            value = fields.get(key);
            if (value == null)
                return;
            sb.append(name).append(':').append(vcfEncode(value)).append("\r\n");
        }
    }

    private static void encodeAddress(StringBuilder sb, String type, String streetKey, String cityKey, String stateKey, String zipKey, String countryKey,
                                                    int firstSuffix, Map<String, String> fields) {
        if (sb == null || type == null)
            return;
        String street = fields.get(streetKey);
        String city = fields.get(cityKey);
        String state = fields.get(stateKey);
        String zip = fields.get(zipKey);
        String country = fields.get(countryKey);

        if (street == null && city == null && state == null && zip == null && country == null)
            return;
        String addr = ";;" + vcfEncode(street, true) + ';'  + vcfEncode(city) + ';'  + vcfEncode(state) +';'  + vcfEncode(zip) + ';'  + vcfEncode(country);
        if (!addr.equals(";;;;;;")) {
            sb.append("ADR;TYPE=").append(type).append(':').append(addr).append("\r\n");
        }
        for (int suffix = firstSuffix; suffix < 20 ;suffix++) {
            String key = new StringBuilder(streetKey).append(String.valueOf(suffix)).toString();
            street = fields.get(key);
            key = new StringBuilder(cityKey).append(String.valueOf(suffix)).toString();
            city = fields.get(key);
            key = new StringBuilder(stateKey).append(String.valueOf(suffix)).toString();
            state = fields.get(key);
            key = new StringBuilder(zipKey).append(String.valueOf(suffix)).toString();
            zip = fields.get(key);
            key = new StringBuilder(countryKey).append(String.valueOf(suffix)).toString();
            country = fields.get(key);
            if (street == null && city == null && state == null && zip == null && country == null)
                return;
            addr = ";;" + vcfEncode(street, true) + ';'  + vcfEncode(city) + ';'  + vcfEncode(state) +';'  + vcfEncode(zip) + ';'  + vcfEncode(country);
            if (!addr.equals(";;;;;;")) {
                sb.append("ADR;TYPE=").append(type).append(':').append(addr).append("\r\n");
            }
        }
    }

    private static void encodePhone(StringBuilder sb, String type, String firstKey, int firstSuffix, Map<String, String> fields) {
        if (sb == null || type == null)
            return;

        String phone = fields.get(firstKey);
        if (phone == null)
            return;
        if (!phone.isEmpty()) {
            // FIXME: really are supposed to reformat the phone to some standard
            sb.append("TEL;TYPE=").append(type).append(':').append(phone).append("\r\n");
        }
        for (int suffix = firstSuffix; suffix < 20 ;suffix++) {
            String key = new StringBuilder(firstKey).append(String.valueOf(suffix)).toString();
            phone = fields.get(key);
            if (phone == null)
                return;
            if (!phone.isEmpty()) {
                sb.append("TEL;TYPE=").append(type).append(':').append(phone).append("\r\n");
            }
        }
    }

    private static String vcfEncode(String value) {
        return vcfEncode(value, false);
    }

    /**
     * Encode a value of a property for use in VCARD.
     * @param value <table>
     * <tr><td>Property with multiple values separated by COMMA characters</td>
     *     <td>Use this method to get the encoding separately for each individual value.</td></tr>
     * <tr><td>Property with multiple fields separated by SEMICOLON characters</td>
     *     <td>Use this method to get the encoding separately for each individual field.</td></tr>
     * <tr><td>Property with single value</td>
     *     <td>Use this method to get the whole value.</td></tr>
     * </table>
     * @param newlineToComma Set true if new lines in {@code value} should be replaced with ","
     */
    private static String vcfEncode(String value, boolean newlineToComma) {
        if (value == null || value.equals(""))
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = value.length(); i < len; i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == ',' || c == ';')
                sb.append('\\').append(c);
            else if (c == '\n')
                sb.append(newlineToComma ? "," : "\\N");
            else if (c == '\t' || (c >= 0x20 && c != 0x7F))
                sb.append(c);
        }
        return sb.toString();
    }

    public String getFormatted() {
        return formatted;
    }

    public static void main(String args[]) throws ServiceException {
        // Test data moved to VCardTest
    }
}
