/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.mime;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.SharedInputStream;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.lucene.document.Field;
import org.json.JSONException;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CalculatorStream;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.ZimbraAnalyzer;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Contact.Attachment;
import com.zimbra.cs.object.ObjectHandlerException;
import com.zimbra.cs.util.JMSession;

public class ParsedContact {
    private Map<String, String> mFields;
    private List<Attachment> mAttachments;

    // Used when parsing existing blobs.
    private InputStream mSharedStream;

    // Used when assembling a contact from attachments.
    private MimeMessage mMimeMessage;

    private String mDigest;
    private long mSize;

    private List<IndexDocument> mZDocuments;

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
    public ParsedContact(Map<String, String> fields, InputStream in)
    throws ServiceException {
        init(fields, in);
    }

    /**
     * @param fields maps field names to either a <tt>String</tt> or <tt>String[]</tt> value.
     */
    public ParsedContact(Map<String, ? extends Object> fields, List<Attachment> attachments) throws ServiceException {
        init(fields, null);

        if (attachments != null && !attachments.isEmpty()) {
            try {
                mAttachments = attachments;
                mMimeMessage = generateMimeMessage(attachments);
                mDigest = ByteUtil.getSHA1Digest(Mime.getInputStream(mMimeMessage), true);

                for (Attachment attach : mAttachments)
                    mFields.remove(attach.getName());
                if (fields.isEmpty())
                    throw ServiceException.INVALID_REQUEST("contact must have fields", null);
                initializeSizeAndDigest(); // This didn't happen in init() because there was no stream.
            } catch (MessagingException me) {
                throw MailServiceException.MESSAGE_PARSE_ERROR(me);
            } catch (IOException ioe) {
                throw MailServiceException.MESSAGE_PARSE_ERROR(ioe);
            }
        }
    }

    /**
     *
     * @param con
     * @param getAllFields
     *          if true, all fields are passed to the ParsedContact
     *          if false, only non-hidden fields are passed to the ParsedContact
     * @throws ServiceException
     */
    public ParsedContact(Contact con) throws ServiceException {
        init(con.getAllFields(), con.getContentStream());
    }

    private void init(Map<String, ? extends Object> fields, InputStream in)
    throws ServiceException {
        // Initialized shared stream.
        try {
            if (in instanceof SharedInputStream) {
                mSharedStream = in;
            } else if (in != null) {
                byte[] content = ByteUtil.getContent(in, 1024);
                mSharedStream = new SharedByteArrayInputStream(content);
            }
        } catch (IOException e) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(e);
        }

        // Initialize fields.
        Map<String,String> strMap = new HashMap<String,String>();
        if (fields == null)
            throw ServiceException.INVALID_REQUEST("contact must have fields", null);

        for (String key : fields.keySet()) {
            Object value = fields.get(key);
            String strValue = null;
            key = StringUtil.stripControlCharacters(key);
            // encode multi value attributes as JSONObject
            if (value instanceof String[]) {
                try {
                    strValue = Contact.encodeMultiValueAttr((String[])value);
                } catch (JSONException e) {
                    ZimbraLog.index.warn("Error encoding multi valued attribute " + key, e);
                }
            } else if (value instanceof String) {
                strValue = StringUtil.stripControlCharacters((String)value);
            }

            if (key != null && !key.trim().equals("") && strValue != null && !strValue.equals(""))
                strMap.put(key, strValue);
        }

        if (strMap.isEmpty())
            throw ServiceException.INVALID_REQUEST("contact must have fields", null);

        mFields = strMap;

        // Initialize attachments.
        InputStream contentStream = null;
        if (mSharedStream != null) {
            try {
                // Parse attachments.
                contentStream = getContentStream();
                mAttachments = parseBlob(contentStream);
                for (Attachment attach : mAttachments)
                    mFields.remove(attach.getName());
                if (fields.isEmpty())
                    throw ServiceException.INVALID_REQUEST("contact must have fields", null);

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

    private void initializeSizeAndDigest()
    throws IOException {
        CalculatorStream calc = new CalculatorStream(getContentStream());
        mSize = ByteUtil.getDataLength(calc);
        mDigest = calc.getDigest();
    }

    private static MimeMessage generateMimeMessage(List<Attachment> attachments)
    throws MessagingException {
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
        MimeMultipart multi = new MimeMultipart("mixed");
        int part = 1;
        for (Attachment attach : attachments) {
            ContentDisposition cdisp = new ContentDisposition(Part.ATTACHMENT);
            cdisp.setParameter("filename", attach.getFilename()).setParameter("field", attach.getName());

            MimeBodyPart bp = new MimeBodyPart();
            bp.addHeader("Content-Disposition", cdisp.toString());
            bp.addHeader("Content-Type", attach.getContentType());
            bp.addHeader("Content-Transfer-Encoding", MimeConstants.ET_8BIT);
            bp.setDataHandler(attach.getDataHandler());
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
        return mFields;
    }

    public boolean hasAttachment() {
        return mAttachments != null && !mAttachments.isEmpty();
    }

    public List<Attachment> getAttachments() {
        return mAttachments;
    }

    /**
     * Returns the stream to this contact's blob, or <tt>null</tt> if
     * it has no attachments.
     */
    public InputStream getContentStream()
    throws IOException {
        if (mSharedStream != null) {
            return ((SharedInputStream) mSharedStream).newStream(0, -1);
        }
        if (mMimeMessage != null) {
            return Mime.getInputStream(mMimeMessage);
        }
        return null;
    }

    public long getSize() {
        return mSize;
    }

    public String getDigest() {
        return mDigest;
    }


    public ParsedContact modify(Map<String, String> fieldDelta, List<Attachment> attachDelta) throws ServiceException {
        if (attachDelta != null && !attachDelta.isEmpty()) {
            for (Attachment attach : attachDelta) {
                // make sure we don't have anything referenced in both fieldDelta and attachDelta
                fieldDelta.remove(attach.getName());

                // add the new attachments to the contact
                removeAttachment(attach.getName());
                if (mAttachments == null)
                    mAttachments = new ArrayList<Attachment>(attachDelta.size());
                mAttachments.add(attach);
            }
        }

        for (Map.Entry<String, String> entry : fieldDelta.entrySet()) {
            String name  = StringUtil.stripControlCharacters(entry.getKey());
            String value = StringUtil.stripControlCharacters(entry.getValue());
            if (name != null && !name.trim().equals("")) {
                // kill any attachment with that field name
                removeAttachment(name);

                // and replace the field appropriately
                if (value == null || value.equals(""))
                    mFields.remove(name);
                else
                    mFields.put(name, value);
            }
        }

        if (mFields.isEmpty())
            throw ServiceException.INVALID_REQUEST("contact must have fields", null);

        mDigest = null;
        mZDocuments = null;

        if (mAttachments != null) {
            try {
                mMimeMessage = generateMimeMessage(mAttachments);

                // Original stream is now stale.
                ByteUtil.closeStream(mSharedStream);
                mSharedStream = null;

                initializeSizeAndDigest();
            } catch (MessagingException me) {
                throw MailServiceException.MESSAGE_PARSE_ERROR(me);
            } catch (IOException e) {
                throw MailServiceException.MESSAGE_PARSE_ERROR(e);
            }
        } else {
            // No attachments.  Wipe out any previous reference to a blob.
            ByteUtil.closeStream(mSharedStream);
            mSharedStream = null;
            mMimeMessage = null;
            mSize = 0;
            mDigest = null;
        }

        return this;
    }

    private void removeAttachment(String name) {
        if (mAttachments == null)
            return;

        for (Iterator<Attachment> it = mAttachments.iterator(); it.hasNext(); ) {
            if (it.next().getName().equals(name))
                it.remove();
        }

        if (mAttachments.isEmpty())
            mAttachments = null;
    }


    public ParsedContact analyze(Mailbox mbox) throws ServiceException {
        try {
            analyzeContact(mbox.getAccount(), mbox.attachmentsIndexingEnabled());
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
        if (mZDocuments != null)
            return;

        mZDocuments = new ArrayList<IndexDocument>();
        StringBuilder attachContent = new StringBuilder();

        int numParseErrors = 0;
        ServiceException conversionError = null;
        if (mAttachments != null) {
            for (Attachment attach: mAttachments) {
                try {
                    analyzeAttachment(attach, attachContent, indexAttachments);
                } catch (MimeHandlerException e) {
                    numParseErrors++;
                    String part = attach.getPartName();
                    String ctype = attach.getContentType();
                    ZimbraLog.index.warn("Parse error on attachment " + part + " (" + ctype + ")", e);
                    if (conversionError == null && ConversionException.isTemporaryCauseOf(e)) {
                        conversionError = ServiceException.FAILURE("failed to analyze part", e.getCause());
                        mHasTemporaryAnalysisFailure = true;
                    }
                } catch (ObjectHandlerException e) {
                    numParseErrors++;
                    String part = attach.getPartName();
                    String ctype = attach.getContentType();
                    ZimbraLog.index.warn("Parse error on attachment " + part + " (" + ctype + ")", e);
                }
            }
        }

        mZDocuments.add(new IndexDocument(getPrimaryDocument(acct, attachContent.toString())));

        // dumpLuceneDocuments();
    }

    public List<IndexDocument> getLuceneDocuments(Mailbox mbox) throws ServiceException {
        analyze(mbox);
        // dumpLuceneDocuments();
        return mZDocuments;
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
                org.apache.lucene.document.Document doc = handler.getDocument();
                if (doc != null) {
                    doc.add(new Field(LuceneFields.L_SORT_SIZE,
                            Integer.toString(attach.getSize()),
                            Field.Store.YES, Field.Index.NO));
                    mZDocuments.add(new IndexDocument(doc));
                }
            }
        }
    }

    private static void appendContactField(StringBuilder sb, ParsedContact contact, String fieldName) {
        String s = contact.getFields().get(fieldName);
        if (s!= null) {
            sb.append(s).append(' ');
        }
    }

    private org.apache.lucene.document.Document getPrimaryDocument(Account acct, String contentStrIn) throws ServiceException {
        org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();

        StringBuilder fieldText = new StringBuilder();
        StringBuilder contentText = new StringBuilder();

        String emailFields[] = Contact.getEmailFields(acct);

        Map<String, String> m = getFields();
        for (Map.Entry<String, String> entry : m.entrySet()) {
            if (!Contact.isEmailField(emailFields, entry.getKey())) { // skip email addrs, they're added to CONTENT below
                if (!ContactConstants.A_fileAs.equalsIgnoreCase(entry.getKey()))
                    contentText.append(entry.getValue()).append(' ');
            }

            String fieldTextToAdd = entry.getKey() + ":" + entry.getValue() + "\n";
            fieldText.append(fieldTextToAdd);
        }

        // fetch all the 'email' addresses for this contact into a single concatenated string
        String emailStr;
        {
            StringBuilder emailSb  = new StringBuilder();
            for (String email : Contact.getEmailAddresses(emailFields, getFields())) {
                emailSb.append(email).append(' ');
            }
            emailStr = emailSb.toString();
        }
        String emailStrTokens = ZimbraAnalyzer.getAllTokensConcatenated(LuceneFields.L_H_TO, emailStr);

        StringBuilder searchText = new StringBuilder(emailStrTokens).append(' ');
        appendContactField(searchText, this, ContactConstants.A_company);
        appendContactField(searchText, this, ContactConstants.A_firstName);
        appendContactField(searchText, this, ContactConstants.A_lastName);
        appendContactField(searchText, this, ContactConstants.A_nickname);
        appendContactField(searchText, this, ContactConstants.A_fullName);

        // rebuild contentText here with the emailStr FIRST, then the other text.
        // The email addresses should be first so that they have a higher search score than the other
        // text
        contentText = new StringBuilder(emailStrTokens).append(' ').append(contentText).append(' ').append(contentStrIn);

        /* put the email addresses in the "To" field so they can be more easily searched */
        doc.add(new Field(LuceneFields.L_H_TO, emailStr,
                Field.Store.NO, Field.Index.ANALYZED));

        /* put the name in the "From" field since the MailItem table uses 'Sender'*/
        doc.add(new Field(LuceneFields.L_H_FROM, Contact.getFileAsString(mFields),
                Field.Store.NO, Field.Index.ANALYZED));
        /* bug 11831 - put contact searchable data in its own field so wildcard search works better  */
        doc.add(new Field(LuceneFields.L_CONTACT_DATA, searchText.toString(),
                Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field(LuceneFields.L_CONTENT, contentText.toString(),
                Field.Store.NO, Field.Index.ANALYZED));
        doc.add(new Field(LuceneFields.L_PARTNAME, LuceneFields.L_PARTNAME_CONTACT,
                Field.Store.YES, Field.Index.NOT_ANALYZED));

        /* add key:value pairs to the structured FIELD lucene field */
        doc.add(new Field(LuceneFields.L_FIELD, fieldText.toString(),
                Field.Store.NO, Field.Index.ANALYZED));

        return doc;
    }

    private void dumpLuceneDocuments() {

        StringBuilder sb = new StringBuilder();

        for (IndexDocument id : mZDocuments) {
            id.dump(sb);
        }

        ZimbraLog.index_add.debug("Contact lucene doc: \n" + sb.toString());
    }
}
