/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Apr 30, 2005
 */
package com.zimbra.cs.imap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.*;

import org.apache.commons.codec.net.QCodec;

import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeCompoundHeader.ContentType;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.formatter.VCard;


public class ImapMessage implements Comparable<ImapMessage> {
    static final byte FLAG_RECENT       = 0x01;
    static final byte FLAG_SPAM         = 0x02;
    static final byte FLAG_NONSPAM      = 0x04;
    static final byte FLAG_JUNKRECORDED = 0x08;
    static final byte FLAG_IS_CONTACT   = 0x10;

    static final int IMAP_FLAGS = Flag.FLAG_UNREAD | Flag.FLAG_FLAGGED | Flag.FLAG_DELETED |
                                  Flag.FLAG_DRAFT  | Flag.FLAG_REPLIED | Flag.FLAG_FORWARDED |
                                  Flag.FLAG_NOTIFIED;
    static final byte SESSION_FLAGS = FLAG_RECENT | FLAG_SPAM | FLAG_NONSPAM | FLAG_JUNKRECORDED | FLAG_IS_CONTACT;

    int     sequence;
    int     msgId;
    int     imapUid;
    int     flags;
    long    tags;
    byte    sflags;
    boolean added    = false;
    boolean expunged = false;
    ImapFolder parent;

    public ImapMessage(int id, byte type, int imapId, int flag, long tag) {
        msgId   = id;
        imapUid = imapId;
        flags   = flag;
        tags    = tag;
        sflags  = (type == MailItem.TYPE_CONTACT ? FLAG_IS_CONTACT : 0);
    }

    ImapMessage(MailItem item) {
        this(item.getId(), item.getType(), item.getImapUid(), item.getFlagBitmask(), item.getTagBitmask());
    }

    public int compareTo(ImapMessage i4msg) {
        if (imapUid == i4msg.imapUid)  return 0;
        return (imapUid < i4msg.imapUid ? -1 : 1);
    }

    byte getType() {
        return (sflags & FLAG_IS_CONTACT) == 0 ? MailItem.TYPE_MESSAGE : MailItem.TYPE_CONTACT;
    }

    long getSize(MailItem item) throws ServiceException {
        if (item instanceof Message)
            return item.getSize();
        // FIXME: need to generate the representation of the item to do this correctly...
        return getContent(item).length;
    }

    private static final byte[] EMPTY_CONTENT = new byte[0];

    byte[] getContent(MailItem item) throws ServiceException {
        if (item instanceof Message) {
            return ((Message) item).getMessageContent();
        } else if (item instanceof Contact) {
            try {
                VCard vcard = VCard.formatContact((Contact) item);
                QCodec qcodec = new QCodec();
                StringBuilder header = new StringBuilder();
                header.append("Subject: ").append(qcodec.encode(vcard.fn, Mime.P_CHARSET_UTF8)).append(ImapHandler.LINE_SEPARATOR);
                header.append("Date: ").append(new MailDateFormat().format(new Date(item.getDate()))).append(ImapHandler.LINE_SEPARATOR);
                header.append("Content-Type: text/x-vcard; charset=\"utf-8\"").append(ImapHandler.LINE_SEPARATOR);
                header.append("Content-Transfer-Encoding: 8bit").append(ImapHandler.LINE_SEPARATOR);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write(header.toString().getBytes(Mime.P_CHARSET_ASCII));
                baos.write(ImapHandler.LINE_SEPARATOR_BYTES);
                baos.write(vcard.formatted.getBytes(Mime.P_CHARSET_UTF8));
                return baos.toByteArray();
            } catch (Exception e) {
                throw ServiceException.FAILURE("problems serializing contact " + msgId, e);
            }
        } else
            return EMPTY_CONTENT;
    }

    InputStream getContentStream(MailItem item) throws ServiceException {
        if (item instanceof Message)
            return ((Message) item).getRawMessage();
        return new ByteArrayInputStream(getContent(item));
    }

    private static final String NO_FLAGS = "FLAGS ()";

    String getFlags(ImapSession session) {
        if ((flags & IMAP_FLAGS) == Flag.FLAG_UNREAD && tags == 0 && sflags == 0)
            return NO_FLAGS;
        StringBuilder result = new StringBuilder("FLAGS (");
        int empty = result.length();

        if ((flags & Flag.FLAG_DELETED) != 0)
            result.append(result.length() == empty ? "" : " ").append("\\Deleted");
        if ((flags & Flag.FLAG_DRAFT) != 0)
            result.append(result.length() == empty ? "" : " ").append("\\Draft");
        if ((flags & Flag.FLAG_FLAGGED) != 0)
            result.append(result.length() == empty ? "" : " ").append("\\Flagged");
        if ((flags & Flag.FLAG_REPLIED) != 0)
            result.append(result.length() == empty ? "" : " ").append("\\Answered");
        if ((flags & Flag.FLAG_NOTIFIED) != 0)
            result.append(result.length() == empty ? "" : " ").append("$MDNSent");
        if ((flags & Flag.FLAG_FORWARDED) != 0)
            result.append(result.length() == empty ? "" : " ").append("$Forwarded Forwarded");
        // note: \Seen is the IMAP flag, but we store "unread", so the test here is == not !=
        if ((flags & Flag.FLAG_UNREAD) == 0)
            result.append(result.length() == empty ? "" : " ").append("\\Seen");

        if ((sflags & FLAG_RECENT) != 0)
            result.append(result.length() == empty ? "" : " ").append("\\Recent");
        if ((sflags & FLAG_SPAM) != 0)
            result.append(result.length() == empty ? "" : " ").append("$Junk Junk");
        if ((sflags & FLAG_NONSPAM) != 0)
            result.append(result.length() == empty ? "" : " ").append("$NotJunk NotJunk NonJunk");
        if ((sflags & FLAG_JUNKRECORDED) != 0)
            result.append(result.length() == empty ? "" : " ").append("JunkRecorded");

        long tagBuffer = tags;
        for (int i = 0; tagBuffer != 0 && i < 64; i++) {
            long mask = 1L << i;
            if ((tagBuffer & mask) != 0) {
                ImapSession.ImapFlag i4flag = session.getTagByMask(mask);
                if (i4flag != null)
                    result.append(result.length() == empty ? "" : " ").append(i4flag);
                tagBuffer &= ~mask;
            }
        }
        return result.append(')').toString();
    }
    void setPermanentFlags(int f, long t) {
        if (t == tags && (f & IMAP_FLAGS) == (flags & IMAP_FLAGS))
            return;
        flags = (f & IMAP_FLAGS) | (flags & ~IMAP_FLAGS);
        tags  = t;
        parent.dirtyMessage(this);
    }
    void setSessionFlags(byte s) {
        if (s == sflags)
            return;
        sflags = s;
        parent.dirtyMessage(this);
    }

    private static final byte[] NIL = { 'N', 'I', 'L' };

    private void nstring(PrintStream ps, String value) { if (value == null)  ps.write(NIL, 0, 3);  else astring(ps, value); }
    private void astring(PrintStream ps, String value) { astring(ps, value, false); }
    private void aSTRING(PrintStream ps, String value) { astring(ps, value, true); }
    private void astring(PrintStream ps, String value, boolean upcase) {
        boolean literal = false;
        StringBuilder nonulls = null;
        for (int i = 0, length = value.length(), lastNull = -1; i < length; i++) {
            char c = value.charAt(i);
            if (c == '\0') {
                if (nonulls == null)  nonulls = new StringBuilder();
                nonulls.append(value.substring(lastNull + 1, i));
                lastNull = i;
            } else if (c == '"' || c == '\\' || c > 0x7f)
                literal = true;
        }
        String content = (nonulls == null ? value : nonulls.toString());
        if (upcase)  content = content.toUpperCase();
        if (!literal) {
            ps.write('"');  ps.print(content);  ps.write('"');
        } else {
            try {
                byte[] bytes = content.getBytes(Mime.P_CHARSET_UTF8);
                ps.write('{');  ps.print(bytes.length);  ps.write('}');  ps.write(ImapHandler.LINE_SEPARATOR_BYTES, 0, 2);
                ps.write(bytes, 0, bytes.length);
            } catch (UnsupportedEncodingException uee) {
                ps.write(NIL, 0, 3);
            }
        }
    }

    private void naddresses(PrintStream ps, InternetAddress[] addrs) {
        int count = 0;
        if (addrs != null && addrs.length > 0) {
            for (int i = 0; i < addrs.length; i++) {
                if (addrs[i].isGroup()) {
                    // 7.4.2: "[RFC-2822] group syntax is indicated by a special form of address
                    //         structure in which the host name field is NIL.  If the mailbox name
                    //         field is also NIL, this is an end of group marker (semi-colon in RFC
                    //         822 syntax).  If the mailbox name field is non-NIL, this is a start of
                    //         group marker, and the mailbox name field holds the group name phrase."
                    // FIXME: handle groups...
                } else if (addrs[i].getAddress() == null)
                    continue;
                else {
                    // 7.4.2: "The fields of an address structure are in the following order: personal
                    //         name, [SMTP] at-domain-list (source route), mailbox name, and host name."
                    if (count++ == 0)  ps.write('(');
                    String[] parts = addrs[i].getAddress().split("@", 2);
                    ps.write('(');  nstring(ps, addrs[i].getPersonal());
                    ps.write(' ');  ps.write(NIL, 0, 3);
                    ps.write(' ');  nstring(ps, parts[0]);
                    ps.write(' ');  nstring(ps, parts.length > 1 ? parts[1] : null);
                    ps.write(')');
                }
            }
        }
        if (count == 0)  ps.write(NIL, 0, 3);
        else             ps.write(')');
    }

    void serializeEnvelope(PrintStream ps, MimeMessage mm) throws MessagingException {
        // 7.4.2: "The fields of the envelope structure are in the following order: date, subject,
        //         from, sender, reply-to, to, cc, bcc, in-reply-to, and message-id.  The date,
        //         subject, in-reply-to, and message-id fields are strings.  The from, sender,
        //         reply-to, to, cc, and bcc fields are parenthesized lists of address structures."
        InternetAddress[] from = Mime.parseAddressHeader(mm, "From");
        InternetAddress[] sender = Mime.parseAddressHeader(mm, "Sender"), replyTo = Mime.parseAddressHeader(mm, "Reply-To");
        ps.write('(');  nstring(ps, mm.getHeader("Date", ","));
        ps.write(' ');  nstring(ps, mm.getSubject());
        ps.write(' ');  naddresses(ps, from);
        ps.write(' ');  naddresses(ps, sender.length == 0 ? from : sender);
        ps.write(' ');  naddresses(ps, replyTo.length == 0 ? from : replyTo);
        ps.write(' ');  naddresses(ps, Mime.parseAddressHeader(mm, "To"));
        ps.write(' ');  naddresses(ps, Mime.parseAddressHeader(mm, "CC"));
        ps.write(' ');  naddresses(ps, Mime.parseAddressHeader(mm, "BCC"));
        ps.write(' ');  nstring(ps, mm.getHeader("In-Reply-To", " "));
        ps.write(' ');  nstring(ps, mm.getMessageID());
        ps.write(')');
    }

    private String nATOM(String value) { return value == null ? "NIL" : '"' + value.toUpperCase() + '"'; }

    void serializeStructure(PrintStream ps, MimePart mp, boolean extensions) throws IOException, MessagingException {
        ps.write('(');
        ContentType ct = new ContentType(mp.getContentType());
        String primary = nATOM(ct.getPrimaryType()), subtype = nATOM(ct.getSubType());
        if (primary.equals("\"MULTIPART\"")) {
            // 7.4.2: "Multiple parts are indicated by parenthesis nesting.  Instead of a body type
            //         as the first element of the parenthesized list, there is a sequence of one
            //         or more nested body structures.  The second element of the parenthesized
            //         list is the multipart subtype (mixed, digest, parallel, alternative, etc.)."
            MimeMultipart multi = (MimeMultipart) Mime.getMultipartContent(mp, mp.getContentType());
            for (int i = 0; i < multi.getCount(); i++)
                serializeStructure(ps, (MimePart) multi.getBodyPart(i), extensions);
            if (multi.getCount() <= 0)
                ps.print("NIL");
            ps.write(' ');  ps.print(subtype);
            if (extensions)
                ;   // FIXME: not implementing BODYSTRUCTURE extensions yet
        } else {
            // 7.4.2: "The basic fields of a non-multipart body part are in the following order:
            //         body type, body subtype, body parameter parenthesized list, body id, body
            //         description, body encoding, body size."
            String cte = mp.getEncoding();
            cte = (cte == null || cte.trim().equals("") ? "7bit" : cte);
            boolean rfc822 = primary.equals("\"MESSAGE\"") && subtype.equals("\"RFC822\"");
            aSTRING(ps, ct.getPrimaryType());  ps.write(' ');  aSTRING(ps, ct.getSubType());  ps.write(' ');
            boolean first = true;
            for (Iterator<Map.Entry<String, String>> it = ct.getParameterIterator(); it.hasNext(); first = false) {
                Map.Entry<String, String> param = it.next();
                ps.print(first ? '(' : ' ');  aSTRING(ps, param.getKey());  ps.write(' ');  astring(ps, param.getValue());
            }
            ps.print(first ? "NIL" : ")");
            ps.write(' ');  nstring(ps, mp.getContentID());
            ps.write(' ');  nstring(ps, mp.getDescription());
            ps.write(' ');  aSTRING(ps, cte);
            ps.write(' ');  ps.print(Math.max(mp.getSize(), 0));
            if (rfc822) {
                // 7.4.2: "A body type of type MESSAGE and subtype RFC822 contains, immediately
                //         after the basic fields, the envelope structure, body structure, and
                //         size in text lines of the encapsulated message."
                Object content = Mime.getMessageContent(mp);
                ps.write(' ');  serializeEnvelope(ps, (MimeMessage) content);
                ps.write(' ');  serializeStructure(ps, (MimePart) content, extensions);
            }
            if (rfc822 || primary.equals("\"TEXT\"")) {
                // 7.4.2: "A body type of type TEXT contains, immediately after the basic fields, the
                //         size of the body in text lines.  Note that this size is the size in its
                //         content transfer encoding and not the resulting size after any decoding."
                ps.write(' ');  ps.print(getLineCount(mp));
            }
            if (extensions && !rfc822)
                ;   // FIXME: not implementing BODYSTRUCTURE extensions yet
        }
        ps.write(')');
    }

    private static int getLineCount(MimePart mp) {
        InputStream is = null;
        try {
            if (mp instanceof MimeBodyPart)      is = ((MimeBodyPart) mp).getRawInputStream();
            else if (mp instanceof MimeMessage)  is = ((MimeMessage) mp).getRawInputStream();
            else                                 return 0;

            int lines = 0, c;
            boolean complete = false;
            while ((c = is.read()) != -1)
                if ((complete = (c == '\n')) == true)
                    lines++;
            return complete ? lines : lines + 1;
        } catch (MessagingException e) {
            return 0;
        } catch (IOException e) {
            return 0;
        }
    }
}
