package com.zimbra.cs.service.formatter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.Part;
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.ParseException;

import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.UserServlet.Context;
import com.zimbra.cs.util.DateUtil;

public class VcfFormatter extends Formatter {

    private Pattern ILLEGAL_CHARS = Pattern.compile("[\\/\\:\\*\\?\\\"\\<\\>\\|]");

    public String getType() {
        return "vcf";
    }

    public String getDefaultSearchTypes() {
        return MailboxIndex.SEARCH_FOR_CONTACTS;
    }
    
    public boolean canBeBlocked() {
        return false;
    }

    public void format(Context context, MailItem item) throws IOException, ServiceException {
        ContentDisposition cd;
        try {
            cd = new ContentDisposition(Part.ATTACHMENT);
        } catch (ParseException e) {
            throw ServiceException.FAILURE(e.getMessage(), e);
        }

        if (item instanceof Contact) {
            ParsedVcf vcf = formatContact((Contact) item);

            cd.setParameter("filename", getZipEntryName(vcf, null));
            context.resp.addHeader("Content-Disposition", cd.toString());
            context.resp.setContentType("text/x-vcard");
            context.resp.setCharacterEncoding("utf-8");
            context.resp.getOutputStream().write(vcf.formatted.getBytes("utf-8"));
            return;
        }

        // passed-in item is a folder or a tag or somesuch -- get the list of contacts
        Iterator iterator = getMailItems(context, item, getDefaultStartTime(), getDefaultEndTime());

        cd.setParameter("filename", "contacts.zip");
        context.resp.addHeader("Content-Disposition", cd.toString());
        context.resp.setContentType("application/x-zip-compressed");

        // create the ZIP file
        ZipOutputStream out = new ZipOutputStream(context.resp.getOutputStream());
        HashSet<String> usedNames = new HashSet<String>();

        while (iterator.hasNext()) {
            MailItem itItem = (MailItem) iterator.next();
            if (!(itItem instanceof Contact))
                continue;
            ParsedVcf vcf = formatContact((Contact) itItem);

            // add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(getZipEntryName(vcf, usedNames)));
            out.write(vcf.formatted.getBytes("utf-8"));
            out.closeEntry();
        }
        // complete the ZIP file
        out.close();
    }

    private String getZipEntryName(ParsedVcf vcf, HashSet<String> used) {
        // TODO: more bullet proofing on path lengths and illegal chars
        String fn = vcf.fn, folder = (used == null ? "" : "contacts/"), path;
        if (fn.length() > 115)
            fn = fn.substring(0, 114);
        int counter = 0;
        do {
            path = folder + ILLEGAL_CHARS.matcher(fn).replaceAll("_");
            if (counter > 0)
                path += "-" + counter;
            counter++;
        } while (used != null && used.contains(path));
        return path + ".vcf";
    }

    public static class ParsedVcf {
        public String fn;
        public String formatted;
        ParsedVcf(String xfn, String xformatted)  { this.fn = xfn; this.formatted = xformatted; }
    }

    public static ParsedVcf formatContact(Contact con) {
        Map<String, String> fields = con.getFields();
        List<String> emails = con.getEmailAddresses();

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD\n");
        sb.append("VERSION:3.0\n");

        // FN is the only mandatory component of the vCard -- try our best to find or generate one
        String fn = fields.get(Contact.A_fullName);
        if (fn == null || fn.trim().equals(""))
            try { fn = con.getFileAsString(); } catch (ServiceException e) { fn = ""; }
        if (fn.trim().equals("") && !emails.isEmpty())
            fn = emails.get(0);
        sb.append("FN:").append(vcfEncode(fn)).append('\n');

        String n = vcfEncode(fields.get(Contact.A_lastName)) + ';' +
                   vcfEncode(fields.get(Contact.A_firstName)) + ';' +
                   vcfEncode(fields.get(Contact.A_middleName)) + ';' +
                   vcfEncode(fields.get(Contact.A_namePrefix)) + ';' +
                   vcfEncode(fields.get(Contact.A_nameSuffix));
        if (!n.equals(";;;;"))
            sb.append("N:").append(n).append('\n');

        encodeField(sb, "NICKNAME", fields.get(Contact.A_nickname));

        String bday = fields.get(Contact.A_birthday);
        if (bday != null) {
            Date date = DateUtil.parseDateSpecifier(bday);
            if (date != null)
                sb.append("BDAY;VALUE=date:").append(new SimpleDateFormat("yyyy-MM-dd").format(date)).append('\n');
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

        encodeField(sb, "URL", fields.get(Contact.A_homeURL));
        encodeField(sb, "URL", fields.get(Contact.A_otherURL));
        encodeField(sb, "URL", fields.get(Contact.A_workURL));

        String org = fields.get(Contact.A_company);
        if (org != null && !org.trim().equals("")) {
            org = vcfEncode(org);
            String dept = fields.get(Contact.A_department);
            if (dept != null && !dept.trim().equals(""))
                org += ';' + vcfEncode(dept);
            sb.append("ORG:").append(org).append('\n');
        }
        encodeField(sb, "TITLE", fields.get(Contact.A_jobTitle));

        encodeField(sb, "NOTE", fields.get(Contact.A_notes));

        try {
            List<Tag> tags = con.getTagList();
            if (!tags.isEmpty()) {
                StringBuilder sbtags = new StringBuilder();
                for (Tag tag : tags)
                    sbtags.append(sbtags.length() == 0 ? "" : ",").append(vcfEncode(tag.getName()));
                sb.append("CATEGORIES:").append(sbtags).append('\n');
            }
        } catch (ServiceException e) { }

        sb.append("REV:").append(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date(con.getDate()))).append('\n');
        sb.append("UID:").append(con.getMailbox().getAccountId()).append(':').append(con.getId()).append('\n');
        // sb.append("MAILER:Zimbra ").append(BuildInfo.VERSION).append("\n");
        sb.append("END:VCARD\n");
        return new ParsedVcf(fn, sb.toString());
    }

    private static void encodeField(StringBuilder sb, String name, String value) {
        if (sb == null || name == null || value == null)
            return;
        sb.append(name).append(':').append(vcfEncode(value)).append('\n');
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
            sb.append("ADR;TYPE=").append(type).append(':').append(addr).append('\n');
    }
    
    private static void encodePhone(StringBuilder sb, String type, String phone) {
        if (sb == null || type == null || phone == null || phone.equals(""))
            return;
        // FIXME: really are supposed to reformat the phone to some standard
        sb.append("TEL;TYPE=").append(type).append(':').append(phone).append('\n');
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
}
