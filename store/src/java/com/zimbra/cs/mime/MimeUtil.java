/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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
package com.zimbra.cs.mime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import javax.mail.internet.MimeUtility;

import com.sun.mail.util.BEncoderStream;
import com.sun.mail.util.PropUtil;
import com.sun.mail.util.QEncoderStream;

public class MimeUtil {
    /** (non-Javadoc)
     * Encode <tt>string</tt> no matter what characters are contained in it.
     * @param string text to be encoded
     * @param charset if it's set to <tt>null</tt>, UTF-8 is chosen as default
     * @param encoding if it's set to <tt>null</tt>, base64 encoding is chosen as default
     * @param encodingWord available when encoding is "quoted-printable". If it's set to <tt>false</tt>,
     * only "=_?" characters are encoded, otherwise "=_?\"#$%&'(),.:;<>@[\\]^`{|}~" will be encoded.
     * @return encoded text
     * @throws UnsupportedEncodingException
     *
     * @see  javax.mail.internet.MimeUtility.encodeWord(String string, String charset, String encoding, boolean encodingWord)
     */
    public static String encodeWord(String string, String charset,
            String encoding, boolean encodingWord) throws UnsupportedEncodingException {
        if (!string.contains("\r") && !string.contains("\n")){
            String result = string;
            try {
                result = MimeUtility.encodeText(string);
            } catch (UnsupportedEncodingException e) {
                // ignore the exception and return the source string as is.
            }
            return result;
        }

        // Else, apply the specified charset conversion.
        String jcharset;
        if (charset == null) { // use default charset
            jcharset = MimeUtility.getDefaultJavaCharset(); // the java charset
            charset = getDefaultMIMECharset(); // the MIME equivalent
        } else { // MIME charset -> java charset
            jcharset = MimeUtility.javaCharset(charset);
        }

        // If no transfer-encoding is specified, figure one out.
        if (encoding == null) {
            encoding = "B";
        }

        boolean b64;
        if (encoding.equalsIgnoreCase("B")) {
            b64 = true;
        } else if (encoding.equalsIgnoreCase("Q")) {
            b64 = false;
        } else {
            throw new UnsupportedEncodingException(
                    "Unknown transfer encoding: " + encoding);
        }

        StringBuffer outb = new StringBuffer(); // the output buffer
        doEncode(string, b64, jcharset,
                // As per RFC 2047, size of an encoded string should not
                // exceed 75 bytes.
                // 7 = size of "=?", '?', 'B'/'Q', '?', "?="
                75 - 7 - charset.length(), // the available space
                "=?" + charset + "?" + encoding + "?", // prefix
                true, encodingWord, outb);

        return outb.toString();
    }

    /*
     * The following two properties allow disabling the fold()
     * and unfold() methods and reverting to the previous behavior.
     * They should never need to be changed and are here only because
     * of my paranoid concern with compatibility.
     */
    private static final boolean foldEncodedWords =
            PropUtil.getBooleanSystemProperty("mail.mime.foldencodedwords", false);

    /** (non-Javadoc)
     * @see javax.mail.internet.MimeUtility.doEncode(String string, boolean b64, String jcharset, int avail, String prefix, boolean first, boolean encodingWord, StringBuffer buf)
     */
    private static void doEncode(String string, boolean b64,
            String jcharset, int avail, String prefix,
            boolean first, boolean encodingWord, StringBuffer buf)
                    throws UnsupportedEncodingException {

        // First find out what the length of the encoded version of
        // 'string' would be.
        byte[] bytes = string.getBytes(jcharset);
        int len;
        if (b64) {
            // "B" encoding
            len = BEncoderStream.encodedLength(bytes);
        } else {
            // "Q"
            len = QEncoderStream.encodedLength(bytes, encodingWord);
        }

        int size;
        if ((len > avail) && ((size = string.length()) > 1)) {
            // If the length is greater than 'avail', split 'string'
            // into two and recurse.
            doEncode(string.substring(0, size/2), b64, jcharset,
                    avail, prefix, first, encodingWord, buf);
            doEncode(string.substring(size/2, size), b64, jcharset,
                    avail, prefix, false, encodingWord, buf);
        } else {
            // length <= than 'avail'. Encode the given string
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            OutputStream eos; // the encoder
            if (b64) {
                // "B" encoding
                eos = new BEncoderStream(os);
            } else {
                // "Q" encoding
                eos = new QEncoderStream(os, encodingWord);
            }

            try { // do the encoding
                eos.write(bytes);
                eos.close();
            } catch (IOException ioex) { }

            byte[] encodedBytes = os.toByteArray(); // the encoded stuff
            // Now write out the encoded (all ASCII) bytes into our
            // StringBuffer
            if (!first) {
                // not the first line of this sequence
                if (foldEncodedWords) {
                    buf.append("\r\n "); // start a continuation line
                } else {
                    buf.append(" "); // line will be folded later
                }
            }

            buf.append(prefix);
            for (int i = 0; i < encodedBytes.length; i++) {
                buf.append((char)encodedBytes[i]);
            }
            buf.append("?="); // terminate the current sequence
        }
    }

    /*
     * Get the default MIME charset for this locale.
     */
    private static String defaultMIMECharset;
    static String getDefaultMIMECharset() {
        if (defaultMIMECharset == null) {
            try {
                defaultMIMECharset = System.getProperty("mail.mime.charset");
            } catch (SecurityException ex) { }  // ignore it
        }
        if (defaultMIMECharset == null) {
            defaultMIMECharset = MimeUtility.mimeCharset(MimeUtility.getDefaultJavaCharset());
        }
        return defaultMIMECharset;
    }
}