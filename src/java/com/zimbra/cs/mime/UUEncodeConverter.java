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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.ContentDisposition;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.cs.util.FileUtil;

public class UUEncodeConverter implements MimeVisitor {
    private ModificationCallback mCallback;

    public UUEncodeConverter() { }
    public UUEncodeConverter(ModificationCallback callback)  { mCallback = callback; }

    public void visitMultipart(MimeMultipart mp, int visitKind) { }
    public void visitBodyPart(MimeBodyPart bp)  { }

    public void visitMessage(MimeMessage msg, int visitKind) throws MessagingException, IOException {
        if (visitKind != VISIT_END)
            return;
        if (!msg.isMimeType(Mime.CT_TEXT_PLAIN))
            return;
        
        Object content = msg.getContent();
        // FIXME: check for char or byte arrays, SharedByteInputStream or whatever
        if (!(content instanceof String))
            return;
        String text = (String) content;

        // go through top-level text/plain part and extract uuencoded files
        MimeMultipart mmp = null;
        boolean initial = text.startsWith("begin ");
        for (int location = 0; initial || (location = text.indexOf("\nbegin ", location)) != -1; initial = false, location++) {
            // find the end of the uuencoded block
            int end = text.indexOf("\nend");
            if (end != -1)
                try {
                    // parse the uuencoded content into a String
                    int start = initial ? location: location + 1;
                    UUDecodedFile uu = new UUDecodedFile(text.substring(start, end + 4));

                    MimeBodyPart mbp = new MimeBodyPart();
                    mbp.setHeader("Content-Type", Mime.CT_APPLICATION_OCTET_STREAM);
                    ContentDisposition cd = new ContentDisposition(Part.ATTACHMENT);
                    cd.setParameter("filename", uu.getFilename());
                    mbp.setHeader("Content-Disposition", cd.toString());
                    mbp.setDataHandler(new DataHandler(uu.getDataSource()));

                    if (mmp == null)
                        mmp = new MimeMultipart("mixed");
                    mmp.addBodyPart(mbp);

                    text = text.substring(0, start) + text.substring(end + 4);
                    location--;
                } catch (ParseException pe) { }
        }
        
        if (mmp == null)
            return;

        // take the remaining text and put it in as the first "related" part
        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setText(text, Mime.P_CHARSET_UTF8);
        mbp.setHeader("Content-Type", "text/plain; charset=utf-8");
        mmp.addBodyPart(mbp, 0);

        // check to make sure that the caller's OK with altering the message
        if (mCallback != null && !mCallback.onModification())
            return;
        // and replace the top-level part with a new multipart/related
        msg.setContent(mmp);
        msg.saveChanges();
    }

    private static class UUDecodedFile {
        private short mMode;
        private String mFilename;
        private byte[] mContent;

        private UUDecodedFile(String text) throws ParseException {
            // skip "begin *"
            if (!text.startsWith("begin "))
                throw new ParseException("missing 'begin'", 0);

            // read mode value
            int pos = 6, start;
            char c;
            while (text.charAt(pos) == ' ')
                pos++;
            try {
                mMode = Short.parseShort(text.substring(pos, pos + 3));
            } catch (NumberFormatException nfe) {
                throw new ParseException("invalid mode string", pos);
            }
            pos += 3;

            // read filename
            if (text.charAt(pos++) != ' ')
                throw new ParseException("missing space after mode", pos - 1);
            while (text.charAt(pos) == ' ')
                pos++;
            start = pos;
            while ((c = text.charAt(pos)) != '\r' && c != '\n')
                pos++;
            mFilename = FileUtil.trimFilename(text.substring(start, pos));
            while ((c = text.charAt(pos)) == '\r' || c == '\n')
                pos++;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int length = 0, end = text.length(), bits = 0, acc = 0;
            do {
                // read line of content
                length = (text.charAt(pos++) - ' ') & 0x3F;
                if (length * 4 / 3 > end - pos)
                    throw new ParseException("invalid encoded line length", pos - 1);
                for (int decoded = 0; decoded < length; ) {
                    acc = (acc << 6) | (((c = text.charAt(pos++)) - ' ') & 0x3F);
                    bits += 6;
                    if (bits < 8)
                        continue;
                    bits -= 8;
                    baos.write(acc >> bits);
                    acc &= (0xFF >> (8 - bits));
                    decoded++;
                }
                while (pos < end && (c = text.charAt(pos)) != '\r' && c != '\n')
                    pos++;
                while (pos < end && (c = text.charAt(pos)) == '\r' || c == '\n')
                    pos++;
            } while (length > 0);

            // skip "end"
            if (!text.startsWith("end", pos))
                throw new ParseException("missing 'end'", pos);
            mContent = baos.toByteArray();
        }

        short getMode()             { return mMode; }
        String getFilename()        { return mFilename; }
        byte[] getContent()         { return mContent; }
        DataSource getDataSource()  { return new ByteArrayDataSource(); }

        private class ByteArrayDataSource implements DataSource {
            public String getContentType()         { return Mime.CT_APPLICATION_OCTET_STREAM; }
            public String getName()                { return getFilename(); }
            public InputStream getInputStream()    { return new ByteArrayInputStream(getContent()); }
            public OutputStream getOutputStream()  { return null; }
        }
    }

    public static void main(String[] args) throws MessagingException, IOException, ParseException {
        UUDecodedFile uu = new UUDecodedFile("begin 644 cat.txt\n#0V%T\n`\nend\n");
        System.out.println(new String(uu.getContent()));

        uu = new UUDecodedFile("begin 644 EXAMPLE\n>5&AI<R!F:6QE(&AA<R!B965N(%55+65N8V]D960N\n`\nend\n");
        System.out.println(new String(uu.getContent()));

        uu = new UUDecodedFile("begin 664 hoge.txt\nM/CXQ,PJDO:3LI,^EQZ6SH;RER:2YI.NDR*2MI,NDP:3(S,S%W:2KI,B[UZ3O\n#I.P*\n`\nend");
        System.out.println(new String(uu.getContent(), "euc-jp"));

        MimeMessage mm = new MimeMessage(com.zimbra.cs.util.JMSession.getSession(), new java.io.FileInputStream("c:\\tmp\\uuencode-1"));
        Mime.accept(new UUEncodeConverter(), mm);
        mm.writeTo(new java.io.FileOutputStream("c:\\tmp\\decoded-1"));

        mm = new MimeMessage(com.zimbra.cs.util.JMSession.getSession(), new java.io.FileInputStream("c:\\tmp\\uuencode-2"));
        Mime.accept(new UUEncodeConverter(), mm);
        mm.writeTo(new java.io.FileOutputStream("c:\\tmp\\decoded-2"));
    }
}
