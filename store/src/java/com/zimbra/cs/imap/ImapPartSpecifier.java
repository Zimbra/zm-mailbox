/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.imap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.InputStreamWithSize;
import com.zimbra.common.util.StartOutOfBoundsException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mime.Mime;

class ImapPartSpecifier {
    static class BinaryDecodingException extends Exception {
        private static final long serialVersionUID = 8158363540973909369L;
    }

    /**
     * Maximum size of byte[] buffer held in memory while processing a
     * <tt>FETCH BINARY</tt> or <tt>FETCH BINARY.SIZE</tt>.
     */
    static final int MAX_PINNED_BINARY_CONTENT = 100000;

    private final String command;
    private final String part;
    private final String modifier;
    private int octetStart = -1, octetEnd = -1;
    private List<String> requestedHeaders;

    ImapPartSpecifier(String cmd, String part, String modifier) {
        this.command = cmd;
        this.part = part;
        this.modifier = modifier;
    }

    ImapPartSpecifier(String cmd, String part, String modifier, int start, int count) {
        this(cmd, part, modifier);
        setPartial(start, count);
    }

    void setPartial(int start, int count) {
        if (start >= 0 && count >= 0) {
            this.octetStart = start;
            this.octetEnd = start + count;
        }
    }

    boolean isEntireMessage() {
        return part.equals("") && modifier.equals("");
    }

    ImapPartSpecifier setHeaders(List<String> headers) {
        this.requestedHeaders = headers;
        return this;
    }

    private String[] getHeaders() {
        if (requestedHeaders == null || requestedHeaders.isEmpty()) {
            return NO_HEADERS;
        }

        String[] headers = new String[requestedHeaders.size()];
        for (int i = 0; i < requestedHeaders.size(); i++) {
            headers[i] = requestedHeaders.get(i);
        }
        return headers;
    }

    private static final String[] NO_HEADERS = new String[0];

    @Override
    public String toString() {
        StringBuilder response = new StringBuilder(command);
        if (command.equals("BODY") || command.equals("BINARY") || command.equals("BINARY.SIZE")) {
            response.append('[').append(getSectionSpec()).append(']');
            // 6.4.5: "BODY[]<0.2048> of a 1500-octet message will return
            // BODY[]<0> with a literal of size 1500, not BODY[]."
            if (octetStart != -1) {
                response.append('<').append(octetStart).append('>');
            }
        }
        return response.toString();
    }

    String getCommand() {
        return command;
    }

    String getSectionPart() {
        return part;
    }

    String getSectionSpec() {
        StringBuilder sb = new StringBuilder();
        sb.append(part).append(part.equals("") || modifier.equals("") ? "" : ".").append(modifier);
        if (requestedHeaders != null) {
            boolean first = true;
            sb.append(" (");
            for (String header : requestedHeaders) {
                sb.append(first ? "" : " ").append(header.toUpperCase());
                first = false;
            }
            sb.append(')');
        }
        return sb.toString();
    }

    void write(PrintStream ps, OutputStream os, Object obj) throws IOException, BinaryDecodingException, ServiceException {
        InputStream is = null;
        try {
            InputStreamWithSize contents = getContent(obj);
            is = contents == null ? null : contents.stream;
            long length = contents == null ? -1 : contents.size;

            ps.print(this);
            ps.write(' ');

            if (is == null) {
                ps.print("NIL");
            } else if (command.equals("BINARY.SIZE")) {
                ps.print(length >= 0 ? length : NULCheck.getLength(is));
            } else {
                boolean binary = false;
                if (command.startsWith("BINARY")) {
                    NULCheck nul = NULCheck.hasNULs(is, length);
                    if (length < 0) {
                        length = nul.length;
                    }
                    if (nul.content == null) {
                        // reload the original InputStream
                        is = getContent(obj).stream;
                    } else {
                        // use the cached copy
                        is = new ByteArrayInputStream(nul.content);
                    }
                    binary = nul.hasNULs;
                }

                ps.print(binary ? "~{" : "{");
                ps.print(length);
                ps.write('}');
                if (os != null) {
                    os.write(ImapHandler.LINE_SEPARATOR_BYTES);
                    long written = ByteUtil.copy(is, false, os, false);
                    assert written == length;
                }
            }
        } finally {
            ByteUtil.closeStream(is);
        }
    }

    InputStreamWithSize getContent(Object obj) throws IOException, BinaryDecodingException, ServiceException {
        InputStreamWithSize contents;
        if (obj instanceof MimeMessage) {
            contents = getContent((MimeMessage) obj);
        } else if (obj instanceof MailItem) {
            if (!isEntireMessage()) {
                throw ServiceException.FAILURE("called writeMessage on non-toplevel part", null);
            }
            contents = ImapMessage.getContent((MailItem) obj);
        } else {
            throw ServiceException.FAILURE("called write() with unexpected argument: "
                    + (obj == null ? "null" : obj.getClass().getSimpleName()), null);
        }

        if (octetStart >= 0 && contents != null) {
            // if there is a "partial" octet start/length constraint on this
            // part specifier, apply it here
            InputStream is = contents.stream;
            long statedLength = contents.size;
            long realLength = Math.max(0, Math.min(statedLength < 0 ? Integer.MAX_VALUE : statedLength, octetEnd) - octetStart);
            try {
                int start = octetStart;
                // the JavaMail implementations of the content-transfer decoders
                // don't do skip() correctly
                if (is instanceof com.sun.mail.util.BASE64DecoderStream || is instanceof com.sun.mail.util.QPDecoderStream) {
                    ByteUtil.skip(is, start);
                    start = 0;
                }
                is = ByteUtil.SegmentInputStream.create(is, start, start + realLength);
            } catch (StartOutOfBoundsException e) {
                //return empty string {0} when start is out of range
                ZimbraLog.imap.warn("IMAP part requested start out of range", e);
                is = new ByteArrayInputStream(new byte[0]);
                statedLength = realLength = 0;
            } catch (IOException ioe) {
                ByteUtil.closeStream(is);
                throw ioe;
            }
            contents = new InputStreamWithSize(is, statedLength < 0 ? -1 : realLength);
        }
        return contents;
    }

    private static class NULCheck {
        long length;
        byte[] content;
        boolean hasNULs;

        static long getLength(InputStream is) throws BinaryDecodingException {
            return scan(is, -1, false).length;
        }

        static NULCheck hasNULs(InputStream is, long length) throws BinaryDecodingException {
            return scan(is, length, true);
        }

        private static NULCheck scan(InputStream is, long length, boolean checkNULs) throws BinaryDecodingException {
            // we want to (a) avoid rereading the message from disk, but (b)
            // avoid reading a 30GB blob to memory
            // so we're going to read messages up to a certain length into
            // memory and do the rest by going back to disk
            NULCheck nul = new NULCheck();
            long totalSize = 0;
            ByteArrayOutputStream baos = null;
            if (checkNULs && length <= MAX_PINNED_BINARY_CONTENT) {
                baos = new ByteArrayOutputStream(length < 0 ? MAX_PINNED_BINARY_CONTENT / 2 : (int) length);
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            try {
                while ((bytesRead = is.read(buffer)) >= 0) {
                    // check for NUL bytes
                    if (checkNULs && !nul.hasNULs) {
                        for (int i = 0; i < bytesRead; i++) {
                            if (buffer[i] == '\0') {
                                nul.hasNULs = true;
                                break;
                            }
                        }
                    }
                    // and copy to memory buffer if we're doing that...
                    if (length < 0) {
                        totalSize += bytesRead;
                        if (totalSize > MAX_PINNED_BINARY_CONTENT && baos != null) {
                            baos = null;
                        }
                    }
                    if (baos != null) {
                        baos.write(buffer, 0, bytesRead);
                    } else if (nul.hasNULs && length > 0) {
                        break;
                    }
                }
            } catch (IOException ioe) {
                throw new BinaryDecodingException();
            } finally {
                ByteUtil.closeStream(is);
            }

            if (baos != null) {
                nul.content = baos.toByteArray();
            }
            if (length < 0) {
                nul.length = totalSize;
            }
            return nul;
        }
    }

    private boolean isMessageBody(MimeMessage base, MimePart resolved)
    throws IOException, MessagingException {
        if (!part.equals("1") && !part.endsWith(".1")) {
            return false;
        }

        String parentPart = part.substring(0, Math.max(0, part.length() - 2));
        return Mime.getMimePart(base, parentPart) == resolved;
    }

    private InputStreamWithSize getContent(MimeMessage msg) throws BinaryDecodingException {
        long length = -1;
        InputStream is = null;

        try {
            MimePart mp = Mime.getMimePart(msg, part);
            if (mp == null) {
                return null;
            }

            // TEXT and HEADER* modifiers operate on rfc822 messages
            if ((modifier.equals("TEXT") || modifier.startsWith("HEADER")) && !(mp instanceof MimeMessage)) {
                // FIXME: hackaround for JavaMail's failure to handle multipart/digest properly
                Object content = Mime.getMessageContent(mp);
                if (!(content instanceof MimeMessage)) {
                    return null;
                }
                mp = (MimeMessage) content;
            }

            // get the content of the requested part
            if (modifier.equals("")) {
                if (mp instanceof MimeBodyPart) {
                    if (command.startsWith("BINARY")) {
                        try {
                            is = ((MimeBodyPart) mp).getInputStream();
                        } catch (IOException ioe) {
                            throw new BinaryDecodingException();
                        }
                    } else {
                        is = ((MimeBodyPart) mp).getRawInputStream();
                        length = Math.max(0, mp.getSize());
                    }
                } else if (mp instanceof MimeMessage) {
                    if (!isMessageBody(msg, mp)) {
                        String parentPart = part.substring(0, Math.max(0, part.length() - 2));
                        return new ImapPartSpecifier(command, parentPart, "TEXT").getContent(msg);
                    } else if (command.startsWith("BINARY")) {
                        try {
                            is = ((MimeMessage) mp).getInputStream();
                        } catch (IOException ioe) {
                            throw new BinaryDecodingException();
                        }
                    } else {
                        is = ((MimeMessage) mp).getRawInputStream();
                        length = Math.max(0, mp.getSize());
                    }
                } else {
                    ZimbraLog.imap.debug("getting content of part; not MimeBodyPart: " + this);
                    return ImapMessage.EMPTY_CONTENT;
                }
            } else if (modifier.startsWith("HEADER")) {
                MimeMessage mm = (MimeMessage) mp;
                Enumeration<?> headers;
                if (modifier.equals("HEADER")) {
                    headers = mm.getAllHeaderLines();
                } else if (modifier.equals("HEADER.FIELDS")) {
                    headers = mm.getMatchingHeaderLines(getHeaders());
                } else {
                    headers = mm.getNonMatchingHeaderLines(getHeaders());
                }
                StringBuilder result = new StringBuilder();
                while (headers.hasMoreElements()) {
                    result.append(headers.nextElement()).append(ImapHandler.LINE_SEPARATOR);
                }

                byte[] content = result.append(ImapHandler.LINE_SEPARATOR).toString().getBytes();
                is = new ByteArrayInputStream(content);
                length = content.length;
            } else if (modifier.equals("MIME")) {
                if (mp instanceof MimeMessage) {
                    String parentPart = part.substring(0, Math.max(0, part.length() - 2));
                    return new ImapPartSpecifier(command, parentPart, "HEADER").getContent(msg);
                }

                Enumeration<?> mime = mp.getAllHeaderLines();
                StringBuilder result = new StringBuilder();
                while (mime.hasMoreElements()) {
                    result.append(mime.nextElement()).append(ImapHandler.LINE_SEPARATOR);
                }

                byte[] content = result.append(ImapHandler.LINE_SEPARATOR).toString().getBytes();
                is = new ByteArrayInputStream(content);
                length = content.length;
            } else if (modifier.equals("TEXT")) {
                is = ((MimeMessage) mp).getRawInputStream();
                length = Math.max(0, mp.getSize());
            } else {
                return null;
            }

            return new InputStreamWithSize(is, length);
        } catch (IOException e) {
            ByteUtil.closeStream(is);
            return null;
        } catch (MessagingException e) {
            ByteUtil.closeStream(is);
            return null;
        }
    }

    public boolean isIgnoredExchangeHeader() {
        return requestedHeaders != null && requestedHeaders.size() == 1 && StringUtil.equalIgnoreCase(requestedHeaders.get(0), "CONTENT-CLASS")
                && StringUtil.equalIgnoreCase("BODY", command) && StringUtil.equalIgnoreCase("HEADER.FIELDS", modifier);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (command == null ? 0 : command.hashCode());
        result = prime * result + (requestedHeaders == null ? 0 : requestedHeaders.hashCode());
        result = prime * result + (modifier == null ? 0 : modifier.hashCode());
        result = prime * result + octetEnd;
        result = prime * result + octetStart;
        result = prime * result + (part == null ? 0 : part.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ImapPartSpecifier other = (ImapPartSpecifier) obj;
        if (command == null) {
            if (other.command != null) {
                return false;
            }
        } else if (!command.equals(other.command)) {
            return false;
        }
        if (requestedHeaders == null) {
            if (other.requestedHeaders != null) {
                return false;
            }
        } else if (!requestedHeaders.equals(other.requestedHeaders)) {
            if (other.requestedHeaders == null || requestedHeaders.size() != other.requestedHeaders.size()) {
                return false;
            } else {
                // special case, effectively equal if same headers but different
                // order
                return requestedHeaders.containsAll(other.requestedHeaders);
            }
        }
        if (modifier == null) {
            if (other.modifier != null) {
                return false;
            }
        } else if (!modifier.equals(other.modifier)) {
            return false;
        }
        if (octetEnd != other.octetEnd) {
            return false;
        }
        if (octetStart != other.octetStart) {
            return false;
        }
        if (part == null) {
            if (other.part != null) {
                return false;
            }
        } else if (!part.equals(other.part)) {
            return false;
        }
        return true;
    }
}
