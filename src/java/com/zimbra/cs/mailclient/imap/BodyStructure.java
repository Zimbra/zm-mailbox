/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
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
package com.zimbra.cs.mailclient.imap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * IMAP message BODYSTRUCTURE response.
 */
public class BodyStructure {
    private String type;            // Content type
    private String subtype;         // Content subtype
    private Map<String, String> params; // Content parameters
    private String id;              // Content-ID
    private String description;     // Content-Description
    private String encoding;        // Content-Transfer-Encoding
    private long size = -1;         // Content size in bytes
    private long lines = -1;        // Number of content lines
    private Envelope envelope;      // Optional envelope for MESSAGE/RFC822
    private BodyStructure[] parts;  // Multipart message body parts
    private String md5;             // Content MD5 checksum
    private String disposition;     // Body disposition type
    private Map<String, String> dispositionParams; // Disposition parameters
    private String[] language;      // Body language
    private String location;        // Body content URI

    /**
     * Reads BODYSTRUCTURE IMAP response data.
     * @param is the IMAP stream from which to read the response
     * @param ext if true, then allow extension data (not for BODY)
     * @return the BodyStructure representing the data
     * @throws IOException if an I/O error occurred
     */
    public static BodyStructure read(ImapInputStream is, boolean ext)
            throws IOException {
        // body            = "(" (body-type-1part / body-type-mpart) ")"
        BodyStructure bs = new BodyStructure();
        is.skipChar('(');
        if (is.peek() == '(') {
            bs.readMPart(is, ext);
        } else {
            bs.read1Part(is, ext);
        }
        is.skipChar(')');
        return bs;
    }

    // body-type-1part = (body-type-basic / body-type-msg / body-type-text)
    //                   [SP body-ext-1part]
    // body-type-basic = media-basic SP body-fields
    // body-type-text  = media-text SP body-fields SP body-fld-lines
    // body-type-msg   = media-message SP body-fields SP envelope
    //                   SP body SP body-fld-lines
    // media-basic     = ((DQUOTE ("APPLICATION" / "AUDIO" / "IMAGE" /
    //                   "MESSAGE" / "VIDEO") DQUOTE) / string) SP
    //                   media-subtype
    // media-message   = DQUOTE "MESSAGE" DQUOTE SP DQUOTE "RFC822" DQUOTE
    // media-text      = DQUOTE "TEXT" DQUOTE SP media-subtype
    // media-subtype   = string
    // body-fld-lines  = number
    // body-ext-1part  = body-fld-md5 [SP body-fld-dsp [SP body-fld-lang
    //                   [SP body-fld-loc *(SP body-extension)]]]

    private void read1Part(ImapInputStream is, boolean ext) throws IOException {
        type = is.readString().toLowerCase();
        is.skipChar(' ');
        subtype = is.readString().toLowerCase();
        is.skipChar(' ');
        readFields(is);
        if (type.equals("text")) {
            is.skipChar(' ');
            lines = is.readNumber();
        } else if (type.equals("message") && subtype.equals("rfc822")) {
            is.skipChar(' ');
            envelope = Envelope.read(is);
            is.skipChar(' ');
            parts = new BodyStructure[] { BodyStructure.read(is, ext) };
            is.skipChar(' ');
            lines = is.readNumber();
        }
        if (ext && is.match(' ')) {
            md5 = is.readNString();
            if (is.match(' ')) readExt(is);
        }
    }

    // body-type-mpart = 1*body SP media-subtype
    //                   [SP body-ext-mpart]
    // body-ext-mpart  = body-fld-param [SP body-fld-dsp [SP body-fld-lang
    //                   [SP body-fld-loc *(SP body-extension)]]]
    
    private void readMPart(ImapInputStream is, boolean ext) throws IOException {
        is.skipChar('(');
        type = "multipart";
        List<BodyStructure> parts = new ArrayList<BodyStructure>();
        do {
            parts.add(read(is, ext));
        } while (!is.match(' '));
        subtype = is.readString().toLowerCase();
        if (ext && is.match(' ')) {
            params = readParams(is);
            if (is.match(' ')) readExt(is);
        }
        this.parts = parts.toArray(new BodyStructure[parts.size()]);
        is.skipChar(')');
    }

    // body-fld-dsp    = "(" string SP body-fld-param ")" / nil
    // body-fld-loc    = nstring
    
    private void readExt(ImapInputStream is) throws IOException {
        is.skipChar('(');
        disposition = is.readString();
        is.skipChar(' ');
        dispositionParams = readParams(is);
        is.skipChar(')');
        if (is.match(' ')) {
            language = readLang(is);
            if (is.match(' ')) {
                location = is.readNString();
                while (is.match(' ')) {
                    skipExtData(is);
                }
            }
        }
    }
    
    // body-fields     = body-fld-param SP body-fld-id SP body-fld-desc SP
    //                   body-fld-enc SP body-fld-octets
    // body-fld-id     = nstring
    // body-fld-desc   = nstring
    // body-fld-enc    = (DQUOTE ("7BIT" / "8BIT" / "BINARY" / "BASE64"/
    //                   "QUOTED-PRINTABLE") DQUOTE) / string
    // body-fld-octets = number

    private void readFields(ImapInputStream is) throws IOException {
        params = readParams(is);
        is.skipChar(' ');
        id = is.readNString();
        is.skipChar(' ');
        description = is.readNString();
        is.skipChar(' ');
        encoding = is.readString();
        is.skipChar(' ');
        size = is.readNumber();
    }

    // body-fld-param  = "(" string SP string *(SP string SP string) ")" / nil
    private static Map<String, String> readParams(ImapInputStream is)
            throws IOException {
        if (!is.match('(')) {
            is.skipNil();
            return null;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        do {
            String name = is.readString().toLowerCase();
            is.skipChar(' ');
            String value = is.readString();
            params.put(name, value);
        } while (is.match(' '));
        is.skipChar(')');
        return params;
    }

    // body-extension  = nstring / number /
    //                "(" body-extension *(SP body-extension) ")"
    
    private static void skipExtData(ImapInputStream is) throws IOException {
        if (is.match('(')) {
            do {
                skipExtData(is);
            } while (is.match(' '));
            is.skipChar(')');
        } else {
            is.readAStringData();
        }
    }

    // body-fld-lang   = nstring / "(" string *(SP string) ")"
    
    private static String[] readLang(ImapInputStream is) throws IOException {
        if (is.peek() != '(') {
            String lang = is.readNString();
            return lang != null ? new String[] { lang } : null;
        }
        is.skipChar('(');
        ArrayList<String> lang = new ArrayList<String>();
        do {
            lang.add(is.readString());
        } while (is.match(' '));
        is.skipChar(')');
        return lang.toArray(new String[lang.size()]);
    }
    
    public String getType() { return type; }
    public String getSubtype() { return subtype; }
    public Map<String, String> getParameters() { return params; }
    public String getId() { return id; }
    public String getDescription() { return description; }
    public String getEncoding() { return encoding; }
    public long getSize() { return size; }
    public long getLines() { return lines; }
    public Envelope getEnvelope() { return envelope; }
    public BodyStructure[] getParts() { return parts; }
    public String getMd5() { return md5; }
    public String[] getLanguage() { return language; }
    public String getLocation() { return location; }
    public String getDisposition() { return disposition; }
    public Map<String, String> getDispositionParameters() {
        return dispositionParams;
    }
}
