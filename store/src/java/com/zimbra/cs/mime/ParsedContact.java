/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mime;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.SharedInputStream;
import javax.mail.util.ByteArrayDataSource;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.solr.common.SolrInputDocument;
import org.json.JSONException;

import com.google.common.base.Strings;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.MimeDetect;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CalculatorStream;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZMimeBodyPart;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Contact.Attachment;
import com.zimbra.cs.mailbox.Contact.DerefGroupMembersOption;
import com.zimbra.cs.mailbox.ContactGroup;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.object.ObjectHandlerException;
import com.zimbra.cs.util.JMSession;

public final class ParsedContact {

    private Map<String, String> contactFields;
    private List<Attachment> contactAttachments;
    private InputStream sharedStream; // Used when parsing existing blobs.
    private MimeMessage mimeMessage; // Used when assembling a contact from attachments.
    private String digest;
    private long size;

    private List<IndexDocument> indexDocs;

    /**
     * @param fields maps field names to either a <tt>String</tt> or <tt>String[]</tt> value.
     */
    public ParsedContact(Map<String, ? extends Object> fields) throws ServiceException {
        init(fields, null);
    }

    /**
     * @param fields maps field names to either a <tt>String</tt> or <tt>String[]</tt> value.
     */
    public ParsedContact(Map<String, ? extends Object> fields, byte[] content) throws ServiceException {
        InputStream in = null;
        if (content != null) {
            in = new SharedByteArrayInputStream(content);
        }
        init(fields, in);
    }

    /**
     * @param fields maps field names to either a <tt>String</tt> or <tt>String[]</tt> value.
     */
    public ParsedContact(Map<String, String> fields, InputStream in) throws ServiceException {
        init(fields, in);
    }

    /**
     * @param fields maps field names to either a <tt>String</tt> or <tt>String[]</tt> value.
     */
    public ParsedContact(Map<String, ? extends Object> fields, List<Attachment> attachments) throws ServiceException {
        init(fields, null);

        if (attachments != null && !attachments.isEmpty()) {
            try {
                validateImageAttachments(attachments);
                contactAttachments = attachments;
                mimeMessage = generateMimeMessage(attachments);
                digest = ByteUtil.getSHA256Digest(Mime.getInputStream(mimeMessage), true);

                for (Attachment attach : contactAttachments) {
                    contactFields.remove(attach.getName());
                }
                if (contactFields.isEmpty()) {
                    throw ServiceException.INVALID_REQUEST("contact must have fields", null);
                }
                initializeSizeAndDigest(); // This didn't happen in init() because there was no stream.
            } catch (MessagingException me) {
                throw MailServiceException.MESSAGE_PARSE_ERROR(me);
            } catch (IOException ioe) {
                throw MailServiceException.MESSAGE_PARSE_ERROR(ioe);
            }
        }
    }

    private static void validateImageAttachments(List<Attachment> attachments) throws ServiceException, IOException {
        for (Attachment attach : attachments) {
            if (attach.getName().equals(ContactConstants.A_image)) {
                String contentType = MimeDetect.getMimeDetect().detect(attach.getInputStream());
                if (contentType == null || contentType.matches(MimeConstants.CT_IMAGE_WILD) == false)
                    throw MailServiceException.INVALID_IMAGE("Attached image is not a valid image file");
            }
        }
    }

    public ParsedContact(Contact con) throws ServiceException {
        init(con.getAllFields(), con.getContentStream());
    }

    private void init(Map<String, ? extends Object> fields, InputStream in) throws ServiceException {
        if (fields == null) {
            throw ServiceException.INVALID_REQUEST("contact must have fields", null);
        }
        // Initialized shared stream.
        try {
            if (in instanceof SharedInputStream) {
                sharedStream = in;
            } else if (in != null) {
                byte[] content = ByteUtil.getContent(in, 1024);
                sharedStream = new SharedByteArrayInputStream(content);
            }
        } catch (IOException e) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(e);
        }

        // Initialize fields.
        Map<String, String> map = new HashMap<String, String>();
        for (Map.Entry<String, ? extends Object> entry : fields.entrySet()) {
            String key = StringUtil.stripControlCharacters(entry.getKey());
            String value = null;
            if (entry.getValue() instanceof String[]) {
                // encode multi value attributes as JSONObject
                try {
                    value = Contact.encodeMultiValueAttr((String[]) entry.getValue());
                } catch (JSONException e) {
                    ZimbraLog.index.warn("Error encoding multi valued attribute " + key, e);
                }
            } else if (entry.getValue() instanceof String) {
                value = StringUtil.stripControlCharacters((String) entry.getValue());
            } else if (entry.getValue() instanceof ContactGroup) {
                value = ((ContactGroup) entry.getValue()).encode();
            }

            if (key != null && !key.trim().isEmpty() && !Strings.isNullOrEmpty(value)) {
                if (key.length() > ContactConstants.MAX_FIELD_NAME_LENGTH) {
                    throw ServiceException.INVALID_REQUEST("too big filed name", null);
                } else if (value.length() > ContactConstants.MAX_FIELD_VALUE_LENGTH) {
                    throw MailServiceException.CONTACT_TOO_BIG(ContactConstants.MAX_FIELD_VALUE_LENGTH, value.length());
                }
                map.put(key, value);
            }
        }
        if (map.isEmpty()) {
            throw ServiceException.INVALID_REQUEST("contact must have fields", null);
        } else if (map.size() > ContactConstants.MAX_FIELD_COUNT) {
            throw ServiceException.INVALID_REQUEST("too many fields", null);
        }
        contactFields = map;

        // Initialize attachments.
        if (sharedStream != null) {
            InputStream contentStream = null;
            try {
                // Parse attachments.
                contentStream = getContentStream();
                contactAttachments = parseBlob(contentStream);
                for (Attachment attach : contactAttachments) {
                    contactFields.remove(attach.getName());
                }
                initializeSizeAndDigest();
            } catch (MessagingException me) {
                throw MailServiceException.MESSAGE_PARSE_ERROR(me);
            } catch (IOException ioe) {
                throw MailServiceException.MESSAGE_PARSE_ERROR(ioe);
            } finally {
                ByteUtil.closeStream(contentStream);
            }
        }
    }

    private void initializeSizeAndDigest() throws IOException {
        CalculatorStream calc = new CalculatorStream(getContentStream());
        size = ByteUtil.getDataLength(calc);
        digest = calc.getDigest();
    }

    private static MimeMessage generateMimeMessage(List<Attachment> attachments)
    throws MessagingException {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        MimeMultipart multi = new ZMimeMultipart("mixed");
        int part = 1;
        for (Attachment attach : attachments) {
            ContentDisposition cdisp = new ContentDisposition(Part.ATTACHMENT);
            cdisp.setParameter("filename", attach.getFilename()).setParameter("field", attach.getName());

            MimeBodyPart bp = new ZMimeBodyPart();
            // MimeBodyPart.setDataHandler() invalidates Content-Type and CTE if there is any, so make sure
            // it gets called before setting Content-Type and CTE headers.
            try {
                bp.setDataHandler(new DataHandler(new ByteArrayDataSource(attach.getContent(), attach.getContentType())));
            } catch (IOException e) {
                throw new MessagingException("could not generate mime part content", e);
            }
            bp.addHeader("Content-Disposition", cdisp.toString());
            bp.addHeader("Content-Type", attach.getContentType());
            bp.addHeader("Content-Transfer-Encoding", MimeConstants.ET_8BIT);
            multi.addBodyPart(bp);

            attach.setPartName(Integer.toString(part++));
        }
        mm.setContent(multi);
        mm.saveChanges();

        return mm;
    }

    private static List<Attachment> parseBlob(InputStream in) throws ServiceException, MessagingException, IOException {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), in);
        MimeMultipart multi = null;
        try {
            multi = (MimeMultipart)mm.getContent();
        } catch (ClassCastException x) {
            throw ServiceException.FAILURE("MimeMultipart content expected but got " + mm.getContent().toString(), x);
        }

        List<Attachment> attachments = new ArrayList<Attachment>(multi.getCount());
        for (int i = 1; i <= multi.getCount(); i++) {
            MimeBodyPart bp = (MimeBodyPart) multi.getBodyPart(i - 1);
            ContentDisposition cdisp = new ContentDisposition(bp.getHeader("Content-Disposition", null));

            Attachment attachment = new Attachment(bp.getDataHandler(), cdisp.getParameter("field"));
            attachment.setPartName(Integer.toString(i));
            attachments.add(attachment);
        }
        return attachments;
    }

    public Map<String, String> getFields() {
        return contactFields;
    }

    public boolean hasAttachment() {
        return contactAttachments != null && !contactAttachments.isEmpty();
    }

    public List<Attachment> getAttachments() {
        return contactAttachments;
    }

    /**
     * Returns the stream to this contact's blob, or <tt>null</tt> if it has no attachments.
     */
    public InputStream getContentStream() throws IOException {
        if (sharedStream != null) {
            return ((SharedInputStream) sharedStream).newStream(0, -1);
        }
        if (mimeMessage != null) {
            return Mime.getInputStream(mimeMessage);
        }
        return null;
    }

    public long getSize() {
        return size;
    }

    public String getDigest() {
        return digest;
    }

    public static abstract class FieldDelta {
        private final Op op;

        public static enum Op {
            ADD,
            REMOVE;

            public static Op fromString(String opStr) throws ServiceException {
                if (opStr == null) {
                    return null;
                }

                if ("+".equals(opStr)) {
                    return ADD;
                } else if ("-".equals(opStr)) {
                    return REMOVE;
                }
                throw ServiceException.INVALID_REQUEST("unknown op: " + opStr, null);
            }
        }

        private FieldDelta(Op op) {
            this.op = op;
        }

        Op getOp() {
            return op;
        }
    }

    private static class AttrDelta extends FieldDelta {
        private final String name;
        private final String value;

        private AttrDelta(String name, String value, Op op) {
            super(op);
            this.name = name;
            this.value = value;
        }

        private String getName() {
            return name;
        }

        private String getValue() {
            return value;
        }
    }

    private static class GroupMemberDelta extends FieldDelta {
        private final ContactGroup.Member.Type memberType;
        private final String value;

        private GroupMemberDelta(ContactGroup.Member.Type memberType, String value, Op op) {
            super(op);
            this.memberType = memberType;
            this.value = value;
        }

        private ContactGroup.Member.Type getMemberType() {
            return memberType;
        }

        private String getValue() {
            return value;
        }
    }

    public static class FieldDeltaList {
        private final List<FieldDelta> deltaList = new ArrayList<FieldDelta>();

        public void addAttrDelta(String name, String value, FieldDelta.Op op) {
            // name cannot be null or empty
            if (name == null || name.trim().equals("")) {
                return;
            }
            deltaList.add(new AttrDelta(name, value, op));
        }

        public void addGroupMemberDelta(ContactGroup.Member.Type memberType, String value, FieldDelta.Op op) {
            deltaList.add(new GroupMemberDelta(memberType, value, op));
        }

        private List<FieldDelta> getDeltaList() {
            return deltaList;
        }

        private void removeAllAttrDeltaByName(String name) {
            for (Iterator<FieldDelta> iter = deltaList.iterator(); iter.hasNext();) {
                FieldDelta delta = iter.next();
                if (delta instanceof AttrDelta) {
                    AttrDelta attrDelta = (AttrDelta) delta;
                    if (attrDelta.getName().equals(name)) {
                        iter.remove();
                    }
                }
            }
        }
    }

    public ParsedContact modifyField(String name, String newValue) {
        if (Strings.isNullOrEmpty(newValue))
            contactFields.remove(name);
        else
            contactFields.put(name, newValue);
        return this;
    }

    // convert legacy API to the new API
    public ParsedContact modify(Map<String, String> fieldDelta, List<Attachment> attachDelta)
    throws ServiceException {
        FieldDeltaList fieldDeltaList = new FieldDeltaList();

        for (Map.Entry<String, String> entry : fieldDelta.entrySet()) {
            fieldDeltaList.addAttrDelta(entry.getKey(), entry.getValue(), null);
        }

        return modify(fieldDeltaList, attachDelta);
    }

    public ParsedContact modify(FieldDeltaList fieldDeltaList, List<Attachment> attachDelta)
    throws ServiceException {
        return modify(fieldDeltaList, attachDelta, false);
    }
    public ParsedContact modify(FieldDeltaList fieldDeltaList, List<Attachment> attachDelta,
            boolean discardExistingMembers)
    throws ServiceException {
        if (attachDelta != null && !attachDelta.isEmpty()) {
            try {
                validateImageAttachments(attachDelta);
            } catch (IOException ioe) {
                throw MailServiceException.MESSAGE_PARSE_ERROR(ioe);
            }
            for (Attachment attach : attachDelta) {
                // make sure we don't have anything referenced in both fieldDelta and attachDelta
                fieldDeltaList.removeAllAttrDeltaByName(attach.getName());

                // add the new attachments to the contact
                removeAttachment(attach.getName());
                if (contactAttachments == null) {
                    contactAttachments = new ArrayList<Attachment>(attachDelta.size());
                }
                contactAttachments.add(attach);
            }
        }

        ContactGroup contactGroup = null;
        String encodedContactGroup = contactFields.get(ContactConstants.A_groupMember);
        contactGroup = encodedContactGroup == null?
                ContactGroup.init() : ContactGroup.init(encodedContactGroup);

        boolean contactGroupMemberChanged = false;
        if (discardExistingMembers && contactGroup.hasMembers()) {
            contactGroup.removeAllMembers();
            contactGroupMemberChanged = true;
        }
        for (FieldDelta delta : fieldDeltaList.getDeltaList()) {
            if (delta instanceof AttrDelta) {
                processAttrDelta((AttrDelta) delta);
            } else if (delta instanceof GroupMemberDelta) {
                processGroupMemberDelta((GroupMemberDelta) delta, contactGroup);
                contactGroupMemberChanged = true;
            }
        }

        if (contactFields.isEmpty())
            throw ServiceException.INVALID_REQUEST("contact must have fields", null);

        if (contactGroupMemberChanged) {
            contactFields.put(ContactConstants.A_groupMember, contactGroup.encode());
        }

        digest = null;
        indexDocs = null;

        if (contactAttachments != null) {
            try {
                mimeMessage = generateMimeMessage(contactAttachments);

                // Original stream is now stale.
                ByteUtil.closeStream(sharedStream);
                sharedStream = null;

                initializeSizeAndDigest();
            } catch (MessagingException me) {
                throw MailServiceException.MESSAGE_PARSE_ERROR(me);
            } catch (IOException e) {
                throw MailServiceException.MESSAGE_PARSE_ERROR(e);
            }
        } else {
            // No attachments.  Wipe out any previous reference to a blob.
            ByteUtil.closeStream(sharedStream);
            sharedStream = null;
            mimeMessage = null;
            size = 0;
            digest = null;
        }

        return this;
    }

    private void processAttrDelta(AttrDelta delta) throws ServiceException {

        String name  = StringUtil.stripControlCharacters(delta.getName());
        if (name == null || name.trim().equals("")) {
            return;
        }
        // kill any attachment with that field name
        removeAttachment(name);

        String newValue = StringUtil.stripControlCharacters(delta.getValue());
        FieldDelta.Op op = delta.getOp();

        if (op == null) {
            // legacy behavior before bug 59738
            if (newValue == null || newValue.equals(""))
                contactFields.remove(name);
            else
                contactFields.put(name, newValue);

            return;
        }

        // do not allow adding or removing an empty string
        if (newValue == null || newValue.equals("")) {
            throw ServiceException.INVALID_REQUEST("adding or removing empty value is not allowed", null);
        }

        String curValue = contactFields.get(name);

        if (curValue == null) {
            if (op == FieldDelta.Op.REMOVE) {
                // do nothing
            } else {
                contactFields.put(name, newValue);
            }

        } else {
            List<String> curValuesList = null;
            try {
                curValuesList = new ArrayList<String>(Arrays.asList(Contact.parseMultiValueAttr(curValue)));
            } catch (JSONException e) {
                // log a warning and continue
                ZimbraLog.misc.warn("unable to modify contact for: " +
                        "field=" + name + ", value=" + newValue + ", op=" + op.name() +
                        ".  delta entry ignored", e);
                return;
            }

            if (op == FieldDelta.Op.REMOVE) {
                // remove all occurrences of the value
                for (Iterator<String> iter = curValuesList.iterator(); iter.hasNext();) {
                    if (newValue.equals(iter.next())) {
                        iter.remove();
                    }
                }
            } else {
                // add the value only if it does not already exist
                if (curValuesList.contains(newValue)) {
                    return;
                } else {
                    curValuesList.add(newValue);
                }
            }

            if (curValuesList.size() > 0) {
                // convert updated list to a new json array value
                String[] newValues = curValuesList.toArray(new String[curValuesList.size()]);
                String newMultiValues = null;
                try {
                    newMultiValues = Contact.encodeMultiValueAttr(newValues);
                } catch (JSONException e) {
                    // log a warning and continue
                    ZimbraLog.misc.warn("unable to modify contact for: " +
                            "field=" + name + ", value=" + newValue + ", op=" + op.name() +
                            ".  delta entry ignored", e);
                    return;
                }

                // finally, put the new value back
                contactFields.put(name, newMultiValues);
            } else {
                contactFields.remove(name);
            }
        }
    }

    private void processGroupMemberDelta(GroupMemberDelta delta, ContactGroup contactGroup)
    throws ServiceException {

        FieldDelta.Op op = delta.getOp();
        ContactGroup.Member.Type memberType =  delta.getMemberType();
        String value = delta.getValue();

        if (op == FieldDelta.Op.ADD) {
            contactGroup.addMember(memberType, value);
        } else if (op == FieldDelta.Op.REMOVE){
            contactGroup.removeMember(memberType, value);
        }
    }

    private void removeAttachment(String name) {
        if (contactAttachments == null) {
            return;
        }
        for (Iterator<Attachment> it = contactAttachments.iterator(); it.hasNext(); ) {
            if (it.next().getName().equals(name)) {
                it.remove();
            }
        }
        if (contactAttachments.isEmpty()) {
            contactAttachments = null;
        }
    }


    public ParsedContact analyze(Account acc, boolean indexAttachments) throws ServiceException {
        try {
            analyzeContact(acc, indexAttachments);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            ZimbraLog.index.warn("exception while analyzing contact; attachments will be partially indexed", e);
        }
        return this;
    }

    boolean mHasTemporaryAnalysisFailure = false;
    public boolean hasTemporaryAnalysisFailure() { return mHasTemporaryAnalysisFailure; }

    private void analyzeContact(Account acct, boolean indexAttachments) throws ServiceException {
        if (indexDocs != null) {
            return;
        }
        indexDocs = new ArrayList<IndexDocument>();
        StringBuilder attachContent = new StringBuilder();

        ServiceException conversionError = null;
        if (contactAttachments != null) {
            for (Attachment attach: contactAttachments) {
                try {
                    analyzeAttachment(attach, attachContent, indexAttachments);
                } catch (MimeHandlerException e) {
                    String part = attach.getPartName();
                    String ctype = attach.getContentType();
                    ZimbraLog.index.warn("Parse error on attachment " + part + " (" + ctype + ")", e);
                    if (conversionError == null && ConversionException.isTemporaryCauseOf(e)) {
                        conversionError = ServiceException.FAILURE("failed to analyze part", e.getCause());
                        mHasTemporaryAnalysisFailure = true;
                    }
                } catch (ObjectHandlerException e) {
                    String part = attach.getPartName();
                    String ctype = attach.getContentType();
                    ZimbraLog.index.warn("Parse error on attachment " + part + " (" + ctype + ")", e);
                }
            }
        }

        indexDocs.add(getPrimaryDocument(acct, attachContent.toString()));
    }

    public List<IndexDocument> getLuceneDocuments(Account acc, boolean indexAttachments) throws ServiceException {
        analyze(acc, indexAttachments);
        return indexDocs;
    }

    private void analyzeAttachment(Attachment attach, StringBuilder contentText, boolean indexAttachments)
    throws MimeHandlerException, ObjectHandlerException, ServiceException {
        String ctype = attach.getContentType();
        MimeHandler handler = MimeHandlerManager.getMimeHandler(ctype, attach.getFilename());
        assert(handler != null);

        if (handler.isIndexingEnabled()) {
            handler.init(attach);
            handler.setPartName(attach.getPartName());
            handler.setFilename(attach.getFilename());
            handler.setSize(attach.getSize());

            if (indexAttachments && !DebugConfig.disableIndexingAttachmentsTogether) {
                // add ALL TEXT from EVERY PART to the toplevel body content.
                // This is necessary for queries with multiple words -- where
                // one word is in the body and one is in a sub-attachment.
                //
                // If attachment indexing is disabled, then we only add the main body and
                // text parts...
                contentText.append(contentText.length() == 0 ? "" : " ").append(handler.getContent());
            }

            if (indexAttachments && !DebugConfig.disableIndexingAttachmentsSeparately) {
                // Each non-text MIME part is also indexed as a separate
                // Lucene document.  This is necessary so that we can tell the
                // client what parts match if a search matched a particular
                // part.
                SolrInputDocument doc = handler.getDocument();
                if (doc != null) {
                    IndexDocument idoc = new IndexDocument(doc);
                    idoc.addSortSize(attach.getSize());
                    indexDocs.add(idoc);
                }
            }
        }
    }

    private static void appendContactField(StringBuilder sb, ParsedContact contact, String fieldName) {
        String value = contact.getFields().get(fieldName);
        if (!Strings.isNullOrEmpty(value)) {
            sb.append(value).append(' ');
        }
    }

    private IndexDocument getPrimaryDocument(Account acct, String contentStrIn) throws ServiceException {

        StringBuilder contentText = new StringBuilder();

        String emailFields[] = Contact.getEmailFields(acct);

        IndexDocument doc = new IndexDocument();

        for (Map.Entry<String, String> entry : getFields().entrySet()) {
            String fieldName = entry.getKey();

            // Ignore these fields as they can either be too big or containing encoded data.
            if (Contact.isSMIMECertField(fieldName) ||
                    ContactConstants.A_member.equals(fieldName) ||
                    ContactConstants.A_groupMember.equals(fieldName)) {
                continue;
            }

            doc.addField(String.format("%s:%s", fieldName, entry.getValue()));
        }

        // fetch all the 'email' addresses for this contact into a single concatenated string
        // We don't index members in a contact group because it's only confusing when searching.
        StringBuilder emails  = new StringBuilder();
        for (String email : Contact.getEmailAddresses(emailFields, getFields(), DerefGroupMembersOption.NONE)) {
            emails.append(email).append(',');
        }

        String to = emails.toString();
        StringBuilder searchText = new StringBuilder();
        appendContactField(searchText, this, ContactConstants.A_company);
        appendContactField(searchText, this, ContactConstants.A_phoneticCompany);
        appendContactField(searchText, this, ContactConstants.A_firstName);
        appendContactField(searchText, this, ContactConstants.A_phoneticFirstName);
        appendContactField(searchText, this, ContactConstants.A_lastName);
        appendContactField(searchText, this, ContactConstants.A_phoneticLastName);
        appendContactField(searchText, this, ContactConstants.A_nickname);
        appendContactField(searchText, this, ContactConstants.A_fullName);

        contentText = new StringBuilder(contentText).append(' ').append(contentStrIn);

        /* put the email addresses in the "To" field so they can be more easily searched */
        doc.addTo(to);

        /* put the name in the "From" field since the MailItem table uses 'Sender'*/
        doc.addFrom(Contact.getFileAsString(contactFields));
        /* bug 11831 - put contact searchable data in its own field so wildcard search works better  */
        doc.addContactData(searchText.toString());
        doc.addContent(contentText.toString());
        doc.addPartName(LuceneFields.L_PARTNAME_CONTACT);

        return doc;
    }

}
