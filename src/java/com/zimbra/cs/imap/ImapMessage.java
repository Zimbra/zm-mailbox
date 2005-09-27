/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
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

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;

import javax.mail.MessagingException;
import javax.mail.internet.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.imap.ImapHandler.PartSpecifier;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.util.ByteUtil;


class ImapMessage {
    static final byte FLAG_RECENT       = 0x01;
    static final byte FLAG_SPAM         = 0x02;
    static final byte FLAG_NONSPAM      = 0x04;
    static final byte FLAG_JUNKRECORDED = 0x08;

    static final int IMAP_FLAGS = Flag.FLAG_UNREAD | Flag.FLAG_FLAGGED | Flag.FLAG_DELETED |
                                  Flag.FLAG_DRAFT  | Flag.FLAG_REPLIED | Flag.FLAG_FORWARDED |
                                  Flag.FLAG_NOTIFIED;
    static final byte SESSION_FLAGS = FLAG_RECENT | FLAG_SPAM | FLAG_NONSPAM | FLAG_JUNKRECORDED;

    static Log sLog = LogFactory.getLog(ImapMessage.class);


    int     seq;
    int     id;
    int     uid;
    byte    sflags;
    int     flags;
    long    tags;
    boolean added    = false;
    boolean expunged = false;
    private int    size;
    private long   date;
    private int    revision;
    private short  volumeId = -1;
    private String dateString;
    private ImapFolder parent;

    ImapMessage(Message msg, ImapFolder i4folder) {
        id       = msg.getId();
        uid      = msg.getImapUID();
        sflags   = i4folder.getId() == Mailbox.ID_FOLDER_SPAM ? (byte) (FLAG_SPAM | FLAG_JUNKRECORDED) : 0;
        flags    = msg.getFlagBitmask();
        tags     = msg.getTagBitmask();
        size     = (int) msg.getSize();
        date     = msg.getDate();
        revision = msg.getSavedSequence();
        volumeId   = msg.getVolumeId();
        parent   = i4folder;
    }

    int getSize()           { return size; }
    int getRevision()       { return revision; }
    short getVolumeId()     { return volumeId; }
    ImapFolder getParent()  { return parent; }

    private static final String NO_FLAGS = "FLAGS ()";

    String getFlags(ImapSession session) {
        if ((flags & IMAP_FLAGS) == Flag.FLAG_UNREAD && tags == 0 && sflags == 0)
            return NO_FLAGS;
        StringBuffer result = new StringBuffer("FLAGS (");
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
        if (parent.notificationsEnabled() && (t != tags || (f & IMAP_FLAGS) != (flags & IMAP_FLAGS)))
            parent.dirtyMessage(this);
        flags = (f & IMAP_FLAGS) | (flags & ~IMAP_FLAGS);
        tags  = t;
    }
    void setSessionFlags(byte s) {
        if (parent.notificationsEnabled() && s != sflags)
            parent.dirtyMessage(this);
        sflags = s;
    }

    String getDate(DateFormat dateFormat) {
        if (dateString == null)
            dateString = "INTERNALDATE \"" + dateFormat.format(new Date(date)) + '"';
        return dateString;
    }

    private static final byte[] NIL = { 'N', 'I', 'L' };

    private void astring(PrintStream ps, String value) { astring(ps, value, false); }
    private void astring(PrintStream ps, String value, boolean upcase) {
        boolean literal = false;
        StringBuffer nonulls = null;
        for (int i = 0, length = value.length(), lastNull = -1; i < length; i++) {
            char c = value.charAt(i);
            if (c == '\0') {
                if (nonulls == null)  nonulls = new StringBuffer();
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
                byte[] bytes = content.getBytes("utf-8");
                ps.write('{');  ps.print(bytes.length);  ps.write('}');  ps.write(ImapHandler.LINE_SEPARATOR_BYTES, 0, 2);
                ps.write(bytes, 0, bytes.length);
            } catch (UnsupportedEncodingException uee) {
                ps.write(NIL, 0, 3);
            }
        }
    }
    private void aSTRING(PrintStream ps, String value) { astring(ps, value, true); }
    private void nstring(PrintStream ps, String value) { if (value == null)  ps.write(NIL, 0, 3);  else astring(ps, value); }
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

    void getEnvelope(PrintStream ps, MimeMessage mm) throws MessagingException {
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

    void getStructure(PrintStream ps, MimePart mp, boolean extensions) throws IOException, MessagingException {
        ps.write('(');
        ContentType ct = new ContentType(mp.getContentType());
        String primary = nATOM(ct.getPrimaryType()), subtype = nATOM(ct.getSubType());
        if (primary.equals("\"MULTIPART\"")) {
            // 7.4.2: "Multiple parts are indicated by parenthesis nesting.  Instead of a body type
            //         as the first element of the parenthesized list, there is a sequence of one
            //         or more nested body structures.  The second element of the parenthesized
            //         list is the multipart subtype (mixed, digest, parallel, alternative, etc.)."
            MimeMultipart multi = (MimeMultipart) mp.getContent();
            for (int i = 0; i < multi.getCount(); i++)
                getStructure(ps, (MimePart) multi.getBodyPart(i), extensions);
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
            ParameterList pl = ct.getParameterList();
            boolean first = true;
            if (pl != null) {
                for (Enumeration en = pl.getNames(); en.hasMoreElements(); first = false) {
                    String name = (String) en.nextElement();
                    ps.print(first ? '(' : ' ');  aSTRING(ps, name);  ps.write(' ');  astring(ps, pl.get(name));
                }
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
                Object content = mp.getContent();
                ps.write(' ');  getEnvelope(ps, (MimeMessage) content);
                ps.write(' ');  getStructure(ps, (MimePart) content, extensions);
            }
            if (rfc822 || primary.equals("\"TEXT\"")) {
                // 7.4.2: "A body type of type TEXT contains, immediately after the basic fields, the
                //         size of the body in text lines.  Note that this size is the size in its
                //         content transfer encoding and not the resulting size after any decoding."
                // FIXME: JavaMail's implementation of this always returns -1
                ps.write(' ');  ps.print(Math.max(mp.getLineCount(), 0));
            }
            if (extensions && !rfc822)
                ;   // FIXME: not implementing BODYSTRUCTURE extensions yet
        }
        ps.write(')');
    }

    private static final byte[] NO_CONTENT = new byte[0];

    byte[] getPart(MimePart mp, PartSpecifier pspec) {
        try {
            mp = Mime.getMimePart(mp, pspec.mPart);
            if (mp == null)
                return null;
            String modifier = pspec.mModifier;
            // TEXT and HEADER* modifiers operate on rfc822 messages
            if ((modifier.equals("TEXT") || modifier.startsWith("HEADER")) && !(mp instanceof MimeMessage)) {
                Object content = mp.getContent();
                if (!(content instanceof MimeMessage))
                    return null;
                mp = (MimeMessage) content;
            }
            // get the content of the requested part
            if (modifier.equals("")) {
                if (mp instanceof MimeBodyPart)
                    return ByteUtil.getRawContent((MimeBodyPart) mp);
                else if (mp instanceof MimeMessage)
                    return ByteUtil.getContent(((MimeMessage) mp).getRawInputStream(), mp.getSize());
                sLog.warn("getting content of part; not MimeBodyPart: " + pspec);
                return NO_CONTENT;
            } else if (modifier.startsWith("HEADER")) {
                MimeMessage mm = (MimeMessage) mp;
                Enumeration headers;
                if (pspec.mModifier.equals("HEADER"))              headers = mm.getAllHeaderLines();
                else if (pspec.mModifier.equals("HEADER.FIELDS"))  headers = mm.getMatchingHeaderLines(pspec.getHeaders());
                else                                               headers = mm.getNonMatchingHeaderLines(pspec.getHeaders());
                StringBuffer result = new StringBuffer();
                while (headers.hasMoreElements())
                    result.append(headers.nextElement()).append(ImapHandler.LINE_SEPARATOR);
                return result.append(ImapHandler.LINE_SEPARATOR).toString().getBytes();
            } else if (modifier.equals("MIME")) {
                Enumeration mime = mp.getAllHeaderLines();
                StringBuffer result = new StringBuffer();
                while (mime.hasMoreElements())
                    result.append(mime.nextElement()).append(ImapHandler.LINE_SEPARATOR);
                return result.append(ImapHandler.LINE_SEPARATOR).toString().getBytes();
            } else if (modifier.equals("TEXT")) {
                MimeMessage mm = (MimeMessage) mp;
                return ByteUtil.getContent(mm.getRawInputStream(), mp.getSize());
            }
            return null;
        } catch (IOException e) {
            return null;
        } catch (MessagingException e) {
            return null;
        }
    }
}
