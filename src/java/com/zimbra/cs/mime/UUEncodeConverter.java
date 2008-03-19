/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mime;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.ByteUtil.PositionInputStream;

public class UUEncodeConverter extends MimeVisitor {
    protected boolean visitMultipart(MimeMultipart mmp, VisitPhase visitKind)  { return false; }
    protected boolean visitBodyPart(MimeBodyPart bp)                           { return false; }

    protected boolean visitMessage(MimeMessage msg, VisitPhase visitKind) throws MessagingException {
        // do the decode in the exit phase
        if (visitKind != VisitPhase.VISIT_END)
            return false;

        MimeMultipart mmp = null;
        try {
            // only check "text/plain" parts for uudecodeable attachments
            if (!msg.isMimeType(Mime.CT_TEXT_PLAIN))
                return false;

            // don't check transfer-encoded parts for uudecodeable attachments
            String cte = msg.getHeader("Content-Transfer-Encoding", null);
            if (cte != null) {
                cte = cte.trim().toLowerCase();
                if (!cte.equals(Mime.ET_7BIT) && !cte.equals(Mime.ET_8BIT) && !cte.equals(Mime.ET_BINARY))
                    return false;
            }

            List<UUDecodedFile> uufiles = null;

            // go through top-level text/plain part and extract uuencoded files
            PositionInputStream is = null;
            long size;
            try {
                is = new PositionInputStream(new BufferedInputStream(msg.getInputStream()));
                for (int c = is.read(); c != -1; ) {
                    long start = is.getPosition() - 1;
                    // check for uuencode header: "begin NNN filename"
                    if (c == 'b' && (c = is.read()) == 'e' && (c = is.read()) == 'g' && (c = is.read()) == 'i' && (c = is.read()) == 'n' &&
                            ((c = is.read()) == ' ' || c == '\t') &&
                            Character.isDigit((c = is.read())) && Character.isDigit(c = is.read()) && Character.isDigit(c = is.read()) &&
                            ((c = is.read()) == ' ' || c == '\t'))
                    {
                        StringBuilder sb = new StringBuilder();
                        while ((c = is.read()) != '\r' && c != '\n' && c != -1)
                            sb.append((char) c);
                        String filename = FileUtil.trimFilename(sb.toString().trim());
                        if (c != -1 && filename.length() > 0) {
                            if (uufiles == null)
                                uufiles = new ArrayList<UUDecodedFile>(3);
                            try {
                                uufiles.add(new UUDecodedFile(is, filename, start));
                                // check to make sure that the caller's OK with altering the message
                                if (uufiles.size() == 1 && mCallback != null && !mCallback.onModification())
                                    return false;
                            } catch (IOException ioe) { }
                        }
                    }
                    // skip to the beginning of the next line
                    while (c != '\r' && c != '\n' && c != -1)
                        c = is.read();
                    while (c == '\r' || c == '\n')
                        c = is.read();
                }
                size = is.getPosition();
            } finally {
                ByteUtil.closeStream(is);
            }

            if (uufiles == null || uufiles.isEmpty())
                return false;

            // create MimeParts for the extracted files
            mmp = new MimeMultipart("mixed");
            for (UUDecodedFile uu : uufiles) {
                MimeBodyPart mbp = new MimeBodyPart();
                mbp.setHeader("Content-Type", Mime.CT_APPLICATION_OCTET_STREAM);
                mbp.setHeader("Content-Disposition", new ContentDisposition(Part.ATTACHMENT).setParameter("filename", uu.getFilename()).toString());
                mbp.setDataHandler(new DataHandler(uu.getDataSource()));
                mmp.addBodyPart(mbp);

                size -= uu.getEndOffset() - uu.getStartOffset();
            }

            // take the remaining text and put it in as the first "related" part
            InputStream isOrig = null;
            try {
                isOrig = msg.getInputStream();
                long offset = 0;
                ByteArrayOutputStream baos = new ByteArrayOutputStream((int) size);
                byte[] buffer = new byte[8192];
                for (UUDecodedFile uu : uufiles) {
                    long count = uu.getStartOffset() - offset, numRead;
                    while (count > 0 && (numRead = isOrig.read(buffer, 0, (int) Math.min(count, 8192))) >= 0) {
                        baos.write(buffer, 0, (int) numRead);  count -= numRead;
                    }
                    isOrig.skip(uu.getEndOffset() - uu.getStartOffset());
                    offset = uu.getEndOffset();
                }
                ByteUtil.copy(isOrig, true, baos, true);

                MimeBodyPart mbp = new MimeBodyPart();
                mbp.setDataHandler(new DataHandler(new ByteArrayDataSource(baos.toByteArray(), Mime.CT_TEXT_PLAIN)));
                mmp.addBodyPart(mbp, 0);
            } finally {
                ByteUtil.closeStream(isOrig);
            }
        } catch (MessagingException e) {
            ZimbraLog.extensions.warn("exception while uudecoding message part; skipping part", e);
            return false;
        } catch (IOException e) {
            ZimbraLog.extensions.warn("exception while uudecoding message part; skipping part", e);
            return false;
        }

        // replace the top-level part with a new multipart/related
        msg.setContent(mmp);
        msg.setHeader("Content-Type", mmp.getContentType() + "; generated=true");
        return true;
    }

    private static class UUDecodedFile {
        private String mFilename;
        private long mStartOffset, mEndOffset;
        private byte[] mContent;

        private UUDecodedFile(PositionInputStream is, String filename, long start) throws IOException {
            mFilename = filename;
            mStartOffset = start;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int length = -1, bits = 0, acc = 0, c = is.read();
            while (c == 'r' || c == '\n')
                c = is.read();
            while (c != -1) {
                // handle already-read length byte, or "end" if it's after a 0-length line
                if (length == 0 && c == 'e') {
                    is.mark(3);
                    if (is.read() == 'n' && is.read() == 'd')
                        break;
                    is.reset();
                }
                length = (c - ' ') & 0x3F;

                // read line of content
                for (int decoded = 0; decoded < length; ) {
                    if ((c = is.read()) == -1)
                        throw new IOException("unexpected eof during uuencoded attachment");
                    acc = (acc << 6) | ((c - ' ') & 0x3F);
                    bits += 6;
                    if (bits < 8)
                        continue;
                    bits -= 8;
                    baos.write(acc >> bits);
                    acc &= (0xFF >> (8 - bits));
                    decoded++;
                }

                // skip to EOL
                while ((c = is.read()) != '\r' && c != '\n' && c != -1)
                    ;
                while (c == '\r' || c == '\n')
                    c = is.read();
            }

            mContent = baos.toByteArray();
            mEndOffset = is.getPosition();
        }

        long getStartOffset()  { return mStartOffset; }
        long getEndOffset()    { return mEndOffset; }
        byte[] getContent()    { return mContent; }
        String getFilename()   { return mFilename; }

        DataSource getDataSource() {
            ByteArrayDataSource bads = new ByteArrayDataSource(mContent, Mime.CT_APPLICATION_OCTET_STREAM);
            bads.setName(mFilename);
            return bads;
        }
    }

    public static void main(String[] args) throws MessagingException, IOException {
        MimeMessage mm = new MimeMessage(com.zimbra.cs.util.JMSession.getSession(), new java.io.FileInputStream("c:\\tmp\\uuencode-1"));
        new UUEncodeConverter().accept(mm);
        mm.saveChanges();
        mm.writeTo(new java.io.FileOutputStream("c:\\tmp\\decoded-1"));

        mm = new MimeMessage(com.zimbra.cs.util.JMSession.getSession(), new java.io.FileInputStream("c:\\tmp\\uuencode-2"));
        new UUEncodeConverter().accept(mm);
        mm.saveChanges();
        mm.writeTo(new java.io.FileOutputStream("c:\\tmp\\decoded-2"));
    }
}
