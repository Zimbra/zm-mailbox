/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.QuotedPrintableCodec;

import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Contact.Attachment;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedContact;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;

public class VCard {

    public String fn;
    public String formatted;
    public Map<String, String> fields;
    public List<Attachment> attachments;

    private VCard(String xfn, String xformatted, Map<String, String> xfields, List<Attachment> xattachments) {
        fn = xfn;  formatted = xformatted;  fields = xfields;  attachments = xattachments;
    }

    public ParsedContact asParsedContact() throws ServiceException {
        return new ParsedContact(fields, attachments);
    }


    private static final Set<String> PROPERTY_NAMES = new HashSet<String>(Arrays.asList(new String[] {
            "BEGIN", "FN", "N", "NICKNAME", "PHOTO", "BDAY", "ADR", "TEL", "EMAIL", "URL", "ORG", "TITLE", "NOTE", "AGENT", "END"
    }));

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

    private enum Encoding { NONE, B, Q };

    private static class VCardProperty {
        private String name;
        private Set<String> params = new HashSet<String>();
        private String charset;
        private Encoding encoding = Encoding.NONE;
        private String value;
        private boolean isEmpty;

        boolean isEmpty()       { return isEmpty; }
        Encoding getEncoding()  { return encoding; }
        boolean containsParam(String param)  { return params.contains(param); }
        String getParamValue(String pname) {
            pname = pname.toUpperCase() + '=';
            for (String param : params)
                if (param.startsWith(pname))
                    return param.substring(pname.length());
            return null;
        }

        private void reset() {
            name = value = null;
            charset = Mime.P_CHARSET_UTF8;
            params.clear();
            encoding = Encoding.NONE;
            isEmpty = false;
        }

        String parse(String line) throws ServiceException {
            reset();
            if ((isEmpty = line.trim().equals("")) == true)
                return "";

            // find the delimiter between property name and property value
            int colon = line.indexOf(':');
            if (colon == -1)
                throw ServiceException.PARSE_ERROR("missing ':' in line " + line, null);
            value = line.substring(colon + 1);

            // find the property name, stripping off any groups (e.g. "FOO.ADR")
            int i, start;
            char c = '\0';
            for (i = start = 0; i < colon; i++) {
                if ((c = line.charAt(i)) == '.')  start = i + 1;
                else if (c == ';')                break;
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
                String pval = line.substring(start, i).toUpperCase();
                if (!pval.equals("")) {
                    String param = (pname != null ? pname + '=' + pval : PARAM_ABBREVIATIONS.get(pval));
                    if (param == null)                                   continue;
                    else if (param.equals("ENCODING=B"))                 encoding = Encoding.B;
                    else if (param.equals("ENCODING=BASE64"))            encoding = Encoding.B;
                    else if (param.equals("ENCODING=QUOTED-PRINTABLE"))  encoding = Encoding.Q;
                    else if (pname != null && pname.equals("CHARSET"))   charset = pval;
                    else                                                 params.add(param);
                }

                if (c == ';')
                    pname = null;
            }

            return name;
        }

        String getValue() {
            // if it's a 2.1 vCard, decode the property value if necessary
            try {
                if (encoding == Encoding.B) {
                    byte[] encoded = value.getBytes();
                    if (Base64.isArrayByteBase64(encoded))
                        value = new String(Base64.decodeBase64(encoded), charset);
                } else if (encoding == Encoding.Q) {
                    value = new QuotedPrintableCodec(charset).decode(value);
                }
                encoding = Encoding.NONE;
            } catch (Exception e) { }
            return value;
        }

        byte[] getDecoded() {
            byte[] encoded = value.getBytes();
            try {
                if (encoding == Encoding.B && Base64.isArrayByteBase64(encoded))
                    encoded = Base64.decodeBase64(encoded);
                encoding = Encoding.NONE;
            } catch (Exception e) { }
            return encoded;
        }
    }

    public static List<VCard> parseVCard(String vcard) throws ServiceException {
        List<VCard> cards = new ArrayList<VCard>();

        Map<String, String> fields = new HashMap<String, String>();
        List<Attachment> attachments = new ArrayList<Attachment>();

        VCardProperty vcprop = new VCardProperty();
        int depth = 0;
        int cardstart = 0, emails = 0;
        for (int start = 0, pos = 0, lines = 0, limit = vcard.length(); pos < limit; lines++) {
            // unfold the next line in the vcard
            String line = "", name = null, value = null;
            int linestart = pos;
            boolean folded = true;
            do {
                start = pos;
                while (pos < limit && vcard.charAt(pos) != '\r' && vcard.charAt(pos) != '\n')
                    pos++;
                line += vcard.substring(start, pos);
                if (pos < limit) {
                    if (pos < limit && vcard.charAt(pos) == '\r')  pos++;
                    if (pos < limit && vcard.charAt(pos) == '\n')  pos++;
                }
                if (pos < limit && (vcard.charAt(pos) == ' ' || vcard.charAt(pos) == '\t'))
                    pos++;
                else {
                    name = vcprop.parse(line);
                    if (vcprop.getEncoding() != Encoding.Q || !line.endsWith("="))
                        folded = false;
                }
            } while (folded);
            if (vcprop.isEmpty())
                continue;

            if (name.equals("")) {
                throw ServiceException.PARSE_ERROR("missing property name in line " + line, null);
            } else if (!PROPERTY_NAMES.contains(name)) {
                continue;
            } else if (name.equals("BEGIN")) {
                if (++depth == 1) {
                    // starting a top-level vCard; reset state
                    fields = new HashMap<String, String>();
                    attachments = new ArrayList<Attachment>();
                    cardstart = linestart;
                    emails = 0;
                }
                continue;
            } else if (name.equals("END")) {
                if (depth > 0 && depth-- == 1) {
                    // finished a vCard; add to list if non-empty
                    if (!fields.isEmpty()) {
                        Contact.normalizeFileAs(fields);
                        cards.add(new VCard(fields.get(Contact.A_fullName), vcard.substring(cardstart, pos), fields, attachments));
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
                    String suffix = vcprop.getParamValue("TYPE"), ctype = null;
                    if (suffix != null && !suffix.equals("")) {
                        ctype = "image/" + suffix.toLowerCase();
                        suffix = '.' + suffix;
                    }
                    attachments.add(new Attachment(vcprop.getDecoded(), ctype, "image", "image" + suffix));
                    continue;
                }
            }

            value = vcprop.getValue();

            // decode the property's value and assign to the appropriate contact field(s)
            if (name.equals("FN"))             fields.put(Contact.A_fullName, vcfDecode(value));
            else if (name.equals("N"))         decodeStructured(value, NAME_FIELDS, fields);
            else if (name.equals("NICKNAME"))  fields.put(Contact.A_nickname, vcfDecode(value));
            else if (name.equals("PHOTO"))     fields.put(Contact.A_image, vcfDecode(value));
            else if (name.equals("BDAY"))      fields.put(Contact.A_birthday, vcfDecode(value));
            else if (name.equals("ADR"))       decodeAddress(value, vcprop, fields);
            else if (name.equals("TEL"))       decodeTelephone(value, vcprop, fields);
            else if (name.equals("URL"))       decodeURL(value, vcprop, fields);
            else if (name.equals("ORG"))       decodeStructured(value, ORG_FIELDS, fields);
            else if (name.equals("TITLE"))     fields.put(Contact.A_jobTitle, vcfDecode(value));
            else if (name.equals("NOTE"))      fields.put(Contact.A_notes, vcfDecode(value));
            else if (name.equals("EMAIL") && emails < EMAIL_FIELDS.length)
                fields.put(EMAIL_FIELDS[emails++], vcfDecode(value));
        }

        return cards;
    }

    private static void decodeTelephone(String value, VCardProperty vcprop, Map<String, String> fields) {
        value = vcfDecode(value);
        if (vcprop.containsParam("TYPE=CAR"))         { fields.put(Contact.A_carPhone, value);  return; }
        else if (vcprop.containsParam("TYPE=CELL"))   { fields.put(Contact.A_mobilePhone, value);  return; }
        else if (vcprop.containsParam("TYPE=PAGER"))  { fields.put(Contact.A_pager, value);  return; }

        boolean home = vcprop.containsParam("TYPE=HOME"), work = vcprop.containsParam("TYPE=WORK");
        boolean fax = vcprop.containsParam("TYPE=FAX"), voice = vcprop.containsParam("TYPE=VOICE");
        if (home) {
            if (fax)  fields.put(Contact.A_homeFax, value);
            if (voice || !fax) {
                if (!fields.containsKey(Contact.A_homePhone))        fields.put(Contact.A_homePhone, value);
                else if (!fields.containsKey(Contact.A_homePhone2))  fields.put(Contact.A_homePhone2, value);
            }
        }
        if (work) {
            if (fax)  fields.put(Contact.A_workFax, value);
            if (voice || !fax) {
                if (!fields.containsKey(Contact.A_workPhone))        fields.put(Contact.A_workPhone, value);
                else if (!fields.containsKey(Contact.A_workPhone2))  fields.put(Contact.A_workPhone2, value);
            }
        }
        if (!home && !work) {
            if (fax)  fields.put(Contact.A_otherFax, value);
            if ((voice || !fax) && !fields.containsKey(Contact.A_otherPhone))  fields.put(Contact.A_otherPhone, value);
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
        if (home)            fields.put(Contact.A_homeURL, vcfDecode(value));
        if (work)            fields.put(Contact.A_workURL, vcfDecode(value));
        if (!home && !work)  fields.put(Contact.A_otherURL, vcfDecode(value));
    }

    private static final String[] NAME_FIELDS = new String[] {
        Contact.A_lastName, Contact.A_firstName, Contact.A_middleName, Contact.A_namePrefix, Contact.A_nameSuffix
    };
    private static final String[] ADR_HOME_FIELDS = new String[] {
        Contact.A_homeStreet, Contact.A_homeStreet, Contact.A_homeStreet,
        Contact.A_homeCity, Contact.A_homeState, Contact.A_homePostalCode, Contact.A_homeCountry
    };
    private static final String[] ADR_WORK_FIELDS = new String[] {
        Contact.A_workStreet, Contact.A_workStreet, Contact.A_workStreet,
        Contact.A_workCity, Contact.A_workState, Contact.A_workPostalCode, Contact.A_workCountry
    };
    private static final String[] ADR_OTHER_FIELDS = new String[] {
        Contact.A_otherStreet, Contact.A_otherStreet, Contact.A_otherStreet,
        Contact.A_otherCity, Contact.A_otherState, Contact.A_otherPostalCode, Contact.A_otherCountry
    };
    private static final String[] ORG_FIELDS = new String[] {
        Contact.A_company, Contact.A_department
    };
    private static final String[] EMAIL_FIELDS = new String[] {
        Contact.A_email, Contact.A_email2, Contact.A_email3
    };

    private static void decodeStructured(String value, String[] keys, Map<String, String> fields) {
        for (int i = 0, start = 0, f = 0, len = value.length(); i < len && f < keys.length; start = ++i, f++) {
            char c;
            for (boolean escaped = false; i < len && ((c = value.charAt(i)) != ';' || escaped); i++)
                escaped = !escaped && c == '\\';
            if (i > start && keys[f] != null)
                fields.put(keys[f], vcfDecode(value.substring(start, i)));
        }
    }

    private static String vcfDecode(String value) {
        if (value == null || value.equals(""))
            return "";
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = 0, len = value.length(); i < len; i++) {
            char c = value.charAt(i);
            if (c == '\\' && !escaped)  escaped = true;
            else if (!escaped)          sb.append(c);
            else {
                if (c == 'n' || c == 'N')       sb.append('\n');
                else if (c == 't' || c == 'T')  sb.append('\t');
                else                            sb.append(c);
                escaped = false;
            }
        }
        return sb.toString();
    }


    public static VCard formatContact(Contact con) {
        Map<String, String> fields = con.getFields();
        List<Attachment> attachments = con.getAttachments();
        List<String> emails = con.getEmailAddresses();

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD\r\n");
        sb.append("VERSION:3.0\r\n");

        // FN is the only mandatory component of the vCard -- try our best to find or generate one
        String fn = fields.get(Contact.A_fullName);
        if (fn == null || fn.trim().equals(""))
            try { fn = con.getFileAsString(); } catch (ServiceException e) { fn = ""; }
        if (fn.trim().equals("") && !emails.isEmpty())
            fn = emails.get(0);
        sb.append("FN:").append(vcfEncode(fn)).append("\r\n");

        String n = vcfEncode(fields.get(Contact.A_lastName)) + ';' +
                   vcfEncode(fields.get(Contact.A_firstName)) + ';' +
                   vcfEncode(fields.get(Contact.A_middleName)) + ';' +
                   vcfEncode(fields.get(Contact.A_namePrefix)) + ';' +
                   vcfEncode(fields.get(Contact.A_nameSuffix));
        if (!n.equals(";;;;"))
            sb.append("N:").append(n).append("\r\n");

        encodeField(sb, "NICKNAME", fields.get(Contact.A_nickname));
        encodeField(sb, "PHOTO;VALUE=URI", fields.get(Contact.A_image));
        String bday = fields.get(Contact.A_birthday);
        if (bday != null) {
            Date date = DateUtil.parseDateSpecifier(bday);
            if (date != null)
                sb.append("BDAY;VALUE=date:").append(new SimpleDateFormat("yyyy-MM-dd").format(date)).append("\r\n");
        }

        encodeAddress(sb, "home,postal,parcel", fields.get(Contact.A_homeStreet),
                fields.get(Contact.A_homeCity), fields.get(Contact.A_homeState),
                fields.get(Contact.A_homePostalCode), fields.get(Contact.A_homeCountry));
        encodeAddress(sb, "work,postal,parcel", fields.get(Contact.A_workStreet),
                fields.get(Contact.A_workCity), fields.get(Contact.A_workState),
                fields.get(Contact.A_workPostalCode), fields.get(Contact.A_workCountry));
        encodeAddress(sb, "postal,parcel", fields.get(Contact.A_otherStreet),
                fields.get(Contact.A_otherCity), fields.get(Contact.A_otherState),
                fields.get(Contact.A_otherPostalCode), fields.get(Contact.A_otherCountry));

        // omitting callback phone for now
        encodePhone(sb, "car,voice", fields.get(Contact.A_carPhone));
        encodePhone(sb, "home,fax", fields.get(Contact.A_homeFax));
        encodePhone(sb, "home,voice", fields.get(Contact.A_homePhone));
        encodePhone(sb, "home,voice", fields.get(Contact.A_homePhone2));
        encodePhone(sb, "cell,voice", fields.get(Contact.A_mobilePhone));
        encodePhone(sb, "fax", fields.get(Contact.A_otherFax));
        encodePhone(sb, "voice", fields.get(Contact.A_otherPhone));
        encodePhone(sb, "pager", fields.get(Contact.A_pager));
        encodePhone(sb, "work,fax", fields.get(Contact.A_workFax));
        encodePhone(sb, "work,voice", fields.get(Contact.A_workPhone));
        encodePhone(sb, "work,voice", fields.get(Contact.A_workPhone2));
        
        for (String email : emails)
            encodeField(sb, "EMAIL;TYPE=internet", email);

        encodeField(sb, "URL;TYPE=home", fields.get(Contact.A_homeURL));
        encodeField(sb, "URL", fields.get(Contact.A_otherURL));
        encodeField(sb, "URL;TYPE=work", fields.get(Contact.A_workURL));

        String org = fields.get(Contact.A_company);
        if (org != null && !org.trim().equals("")) {
            org = vcfEncode(org);
            String dept = fields.get(Contact.A_department);
            if (dept != null && !dept.trim().equals(""))
                org += ';' + vcfEncode(dept);
            sb.append("ORG:").append(org).append("\r\n");
        }
        encodeField(sb, "TITLE", fields.get(Contact.A_jobTitle));

        encodeField(sb, "NOTE", fields.get(Contact.A_notes));

        if (attachments != null) {
            for (Attachment attach : attachments) {
                try {
                    if (attach.getName().equalsIgnoreCase(Contact.A_image)) {
                        String field = "PHOTO;ENCODING=B";
                        if (attach.getContentType().startsWith("image/"))
                            field += ";TYPE=" + attach.getContentType().substring(6).toUpperCase();
                        String encoded = new String(Base64.encodeBase64Chunked(attach.getContent(con))).trim().replace("\r\n", "\r\n ");
                        sb.append(field).append(":\r\n ").append(encoded).append("\r\n");
                    }
                } catch (Throwable t) {
                    ZimbraLog.misc.info("error fetching attachment content: " + attach.getName(), t);
                }
            }
        }

        try {
            List<Tag> tags = con.getTagList();
            if (!tags.isEmpty()) {
                StringBuilder sbtags = new StringBuilder();
                for (Tag tag : tags)
                    sbtags.append(sbtags.length() == 0 ? "" : ",").append(vcfEncode(tag.getName()));
                sb.append("CATEGORIES:").append(sbtags).append("\r\n");
            }
        } catch (ServiceException e) { }

        sb.append("REV:").append(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date(con.getDate()))).append("\r\n");
        sb.append("UID:").append(con.getMailbox().getAccountId()).append(':').append(con.getId()).append("\r\n");
        // sb.append("MAILER:Zimbra ").append(BuildInfo.VERSION).append("\r\n");
        sb.append("END:VCARD\r\n");
        return new VCard(fn, sb.toString(), fields, attachments);
    }

    private static void encodeField(StringBuilder sb, String name, String value) {
        if (sb == null || name == null || value == null)
            return;
        sb.append(name).append(':').append(vcfEncode(value)).append("\r\n");
    }

    private static void encodeAddress(StringBuilder sb, String type, String street, String city, String state, String zip, String country) {
        if (sb == null || type == null)
            return;
        if (street == null && city == null && state == null && zip == null && country == null)
            return;
        String addr = ";;" + vcfEncode(street, true) +
                      ';'  + vcfEncode(city) +
                      ';'  + vcfEncode(state) +
                      ';'  + vcfEncode(zip) +
                      ';'  + vcfEncode(country);
        if (!addr.equals(";;;;;;"))
            sb.append("ADR;TYPE=").append(type).append(':').append(addr).append("\r\n");
    }
    
    private static void encodePhone(StringBuilder sb, String type, String phone) {
        if (sb == null || type == null || phone == null || phone.equals(""))
            return;
        // FIXME: really are supposed to reformat the phone to some standard
        sb.append("TEL;TYPE=").append(type).append(':').append(phone).append("\r\n");
    }

    private static String vcfEncode(String value) {
        return vcfEncode(value, false);
    }
    private static String vcfEncode(String value, boolean newlineToComma) {
        if (value == null || value.equals(""))
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = value.length(); i < len; i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == ',')
                sb.append('\\').append(c);
            else if (c == '\n')
                sb.append(newlineToComma ? "," : "\\N");
            else if (c == '\t' || (c >= 0x20 && c != 0x7F))
                sb.append(c);
        }
        return sb.toString();
    }


    public static void main(String args[]) throws ServiceException {
        parseVCard("BEGIN:VCARD\r\n\r\nFN\n :dr. john doe\nADR;HOME;WORK:;;Hambone Ltd.\\N5 Main St.;Charlotte;NC;24243\nEMAIL:foo@bar.con\nEMAIL:bar@goo.com\nN:doe;john;\\;\\\\;dr.;;;;\nEND:VCARD\n");
        parseVCard("BEGIN:VCARD\r\n\r\nFN\n :john doe\\, jr.\nORG:Zimbra;Marketing;Annoying Marketing\nA.TEL;type=fax,WORK:+1-800-555-1212\nTEL;type=home,work,voice:+1-800-555-1313\nNOTE;QUOTED-PRINTABLE:foo=3Dbar\nc.D.e.NOTE;ENCODING=B;charset=iso-8859-1:SWYgeW91IGNhbiByZWFkIHRoaXMgeW8=\nEND:VCARD\n");
        parseVCard("BEGIN : VCARD\nFN\n :john doe\\, jr.\nAGENT:\\nBEGIN:VCARD\\nEND:VCARD\nEND:VCARD");
//        parseVCard("BEGIN:VCARD\r\n\r\nFN\n :john doe\nA.TEL;WORK:+1-800-555-1212\n.:?\n:\nEND:VCARD\n");
    }
}
