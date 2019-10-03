/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016, 2017 Synacor, Inc.
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
package com.zimbra.common.zmime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CharsetUtil;
import com.zimbra.common.util.ZimbraLog;

public class ZInternetHeader {
    final HeaderInfo hinfo;
    protected final String name;
    protected byte[] content;
    protected int valueStart;

    /** Constructor for pre-analyzed header line read from message source.
     * @param name    Header field name.
     * @param content Complete raw header line, <b>including</b> the field name
     *                and colon and with folding and trailing CRLF and 2047-
     *                encoding intact.
     * @param start   The position within <code>content</code> where the header
     *                field value begins (after the ":"/": "). */
    protected ZInternetHeader(final String name, final byte[] content, final int start) {
        this.hinfo = HeaderInfo.of(name);
        this.name = name;
        this.content = content;
        this.valueStart = start;
    }

    /** Creates a {@code ZInternetHeader} from another {@code ZInternetHeader}. */
    protected ZInternetHeader(final ZInternetHeader header) {
        this.hinfo = header.hinfo;
        this.name = header.name;
        this.content = header.getRawHeader();
        this.valueStart = header.valueStart;
    }

    protected ZInternetHeader(final byte[] line) {
        int colon = -1;
        for (int i = 0, len = line.length; i < len; i++) {
            if (line[i] == ':') {
                colon = i;
                break;
            }
        }
        int vstart = colon == -1 ? line.length : colon + 1;
        if (colon != -1) {
            // the actual content of the header starts after an optional space and/or CRLF
            for (int headerLength = line.length; vstart < headerLength; vstart++) {
                byte b = line[vstart];
                if (b != '\n' && b != '\r' && b != ' ' && b != '\t')
                    break;
            }
        }

        this.name = new String(line, 0, colon == -1 ? line.length : colon, DEFAULT_CHARSET).trim();
        this.hinfo = HeaderInfo.of(name);
        this.content = line;
        this.valueStart = vstart;
        if (LC.mime_handle_nonprintable_subject.booleanValue() && "subject".equalsIgnoreCase(name)) {
            //if any non-printable characters it is probably natively encoded in ISO-2022-JP or similar
            for (int i = vstart; i < line.length; i++) {
                int code = content[i];
                if ((code < 0x20 || code > 0x7E) && code != 10 && code != 13 && code != 9) {
                    byte[] rawValue = Arrays.copyOfRange(content, vstart, content.length);
                    Charset charset = detectCharset(rawValue, DEFAULT_CHARSET);
                    if (charset != null && !charset.equals(DEFAULT_CHARSET)) {
                        String newValue = new String(rawValue, charset);
                        String encoded = EncodedWord.encode(newValue.trim(), charset);
                        updateContent(encoded.getBytes());
                    }
                    break;
                }
            }
        }
    }

    private Charset detectCharset(byte[] content, Charset defaultCharset) {
        if (defaultCharset == null) {
            defaultCharset = Charset.defaultCharset();
        }
        CharsetDetector detector = new CharsetDetector();
        detector.setText(content);

        Charset match = findMatch(detector);
        return (match != null ? match : defaultCharset);
    }

    private Charset findMatch(CharsetDetector detector) {
        for (CharsetMatch match : detector.detectAll()) { // matches are sorted by confidence
            if (match.getConfidence() > 50) { // only trust good match
                try {
                    return Charset.forName(match.getName());
                } catch (Exception ignore) {
                }
            } else {
                break;
            }
        }
        return null;
    }

    /** Creates a new {@code ZInternetHeader} with {@code value} as the field value.
     *  Header will be serialized as <tt>{name}: {encoded-value}CRLF</tt> after
     *  appropriate RFC 2047 encoded-word encoding and RFC 5322 line folding
     *  has been performed.  When generating encoded-words, <tt>utf-8</tt> will
     *  be used as the encoding charset.  <i>Note: No line folding is done at
     *  present.</i> */
    protected ZInternetHeader(final String name, final String value) {
        this(name, value, null);
    }

    /** Creates a new {@code ZInternetHeader} with {@code value} as the field value.
     *  Header will be serialized as <tt>{name}: {encoded-value}CRLF</tt> after
     *  appropriate RFC 2047 encoded-word encoding and RFC 5322 line folding
     *  has been performed.  When generating encoded-words, {@code charset}
     *  will be used as the encoding charset if possible, defaulting back to
     *  <tt>utf-8</tt>.  <i>Note: No line folding is done at present.</i> */
    protected ZInternetHeader(final String name, final String value, final String charset) {
        this.hinfo = HeaderInfo.of(name);
        this.name = hinfo.name == null ? name : hinfo.name;
        //        updateContent(escape(value, CharsetUtil.toCharset(charset), false).getBytes());
        updateContent(value == null ? null : value.getBytes());
    }

    /** Creates a new {@code ZInternetHeader} serialized as "<tt>{name}:
     *  {bvalue}CRLF</tt>".  {@code bvalue} is copied verbatim; no charset
     *  transforms, encoded-word handling, or folding is performed. */
    protected ZInternetHeader(final String name, final byte[] bvalue) {
        this.hinfo = HeaderInfo.of(name);
        this.name = hinfo.name == null ? name : hinfo.name;
        updateContent(bvalue);
    }

    @Override
    protected ZInternetHeader clone() {
        return new ZInternetHeader(this);
    }


    enum HeaderInfo {
        RETURN_PATH("Return-Path", 1, false, true),
        RECEIVED("Received", 2, false, true),
        RESENT_DATE("Resent-Date", 3, false, false, true),
        RESENT_FROM("Resent-From", 3, false, false, true),
        RESENT_SENDER("Resent-Sender", 3, false, false, true),
        RESENT_TO("Resent-To", 3, false, false, true),
        RESENT_CC("Resent-Cc", 3, false, false, true),
        RESENT_BCC("Resent-Bcc", 3, false, false, true),
        RESENT_MESSAGE_ID("Resent-Message-ID", 3, false, false, true),
        DATE("Date", 4, true),
        FROM("From", 5),
        SENDER("Sender", 6, true),
        REPLY_TO("Reply-To", 7, true),
        TO("To", 8),
        CC("Cc", 9),
        BCC("Bcc", 10),
        MESSAGE_ID("Message-ID", 11, true),
        IN_REPLY_TO("In-Reply-To", 12, true),
        REFERENCES("References", 13, true),
        SUBJECT("Subject", 14, true),
        COMMENTS("Comments", 15, true),
        KEYWORDS("Keywords", 16, true),
        ERRORS_TO("Errors-To", 17, true),
        MIME_VERSION("MIME-Version", 18, true),
        CONTENT_TYPE("Content-Type", 19, true),
        CONTENT_DISPOSITION("Content-Disposition", 20, true),
        CONTENT_TRANSFER_ENCODING("Content-Transfer-Encoding", 21, true),
        DEFAULT(null, 30),
        CONTENT_LENGTH("Content-Length", 49, true),
        STATUS("Status", 50, true);

        final String name;
        final int position;
        final boolean unique, prepend, first;

        HeaderInfo(String name, int position) {
            this(name, position, false, false, false);
        }

        HeaderInfo(String name, int position, boolean unique) {
            this(name, position, unique, false, false);
        }

        HeaderInfo(String name, int position, boolean unique, boolean prepend) {
            this(name, position, unique, prepend, false);
        }

        HeaderInfo(String name, int position, boolean unique, boolean prepend, boolean first) {
            this.name = name;  this.position = position;
            this.unique = unique;  this.prepend = prepend;  this.first = first;
        }

        private static final Map<String, HeaderInfo> lookup = new HashMap<String, HeaderInfo>(40);
        static {
            for (HeaderInfo hinfo : EnumSet.allOf(HeaderInfo.class)) {
                if (hinfo.name != null) {
                    lookup.put(hinfo.name.toLowerCase(), hinfo);
                }
            }
        }

        static HeaderInfo of(String name) {
            HeaderInfo hinfo = name == null ? null : lookup.get(name.toLowerCase());
            return hinfo == null ? DEFAULT : hinfo;
        }

        @Override
        public String toString() {
            return name;
        }
    }


    /** Reserializes the {@code ZInternetHeader}, using {@code bvalue} as the
     *  field value (the bit after the '<tt>:</tt>').  {@code bvalue} is
     *  copied verbatim; no charset transforms, encoded-word handling, or
     *  folding is performed.*/
    ZInternetHeader updateContent(final byte[] bvalue) {
        byte[] bname = name.getBytes();
        int nlen = bname.length, vlen = bvalue == null ? 0 : bvalue.length;
        int csize = nlen + vlen + 4;

        byte[] buf = new byte[csize];
        System.arraycopy(bname, 0, buf, 0, nlen);
        buf[nlen] = ':';  buf[nlen + 1] = ' ';
        if (bvalue != null) {
            System.arraycopy(bvalue, 0, buf, nlen + 2, vlen);
        }
        buf[csize - 2] = '\r';  buf[csize - 1] = '\n';

        this.content = buf;  this.valueStart = nlen + 2;
        return this;
    }

    /** Returns this header's field name (the bit before the '<tt>:</tt>' in
     *  the header line). */
    public String getName() {
        return name;
    }

    /** Returns the entire header line (including the field name and the
     *  '<tt>:</tt>') as a raw byte array. */
    public byte[] getRawHeader() {
        return content;
    }

    /** Returns the header's value (the bit after the '<tt>:</tt>') after all
     *  unfolding and decoding of RFC 2047 encoded-words has been performed. */
    public String getValue(final String charset) {
        int end = content.length, c;
        while (end > valueStart && ((c = content[end-1]) == '\n' || c == '\r')) {
            end--;
        }
        return decode(content, valueStart, end - valueStart, CharsetUtil.toCharset(charset));
    }

    /** Returns the header's value (the bit after the '<tt>:</tt>') as a
     *  {@code String}.  No decoding is performed other than removing the
     *  trailing CRLF. */
    @Override
    public String toString() {
        return content == null ? "" : new String(content, valueStart, content.length - valueStart);
    }

    /** Returns the header's value (the bit after the '<tt>:</tt>') as a
     *  {@code String}.  No decoding is performed other than removing the
     *  trailing CRLF. */
    public String getEncodedValue() {
        return getEncodedValue((Charset) null);
    }

    /** Returns the header's value (the bit after the '<tt>:</tt>') as a
     *  {@code String}.  If non-{@code null}, the {@code charset} is used when
     *  converting the header bytes to a {@code String}.  No decoding is
     *  performed other than removing the trailing CRLF. */
    public String getEncodedValue(final String charset) {
        return getEncodedValue(CharsetUtil.toCharset(charset));
    }

    public String getEncodedValue(final Charset charset) {
        int end = content.length, c;
        while (end > valueStart && ((c = content[end-1]) == '\n' || c == '\r')) {
            end--;
        }
        return createString(content, valueStart, end - valueStart, charset);
    }

    private static String createString(final byte[] bytes, final int offset, final int length, final Charset charset) {
        return new String(bytes, offset, length, decodingCharset(charset));
    }

    static final Charset DEFAULT_CHARSET = CharsetUtil.normalizeCharset(CharsetUtil.ISO_8859_1);

    static Charset decodingCharset(Charset charset) {
        return charset != null ? CharsetUtil.normalizeCharset(charset) : DEFAULT_CHARSET;
    }

    public static String decode(final String content) {
        return decode(content.getBytes(CharsetUtil.UTF_8), CharsetUtil.UTF_8);
    }

    static String decode(final byte[] content, final Charset charset) {
        return decode(content, 0, content.length, charset);
    }

    @SuppressWarnings("null")
    static String decode(final byte[] content, final int start, final int length, final Charset charset) {
        // short-circuit if there are only ASCII characters and no "=?"
        final int end = start + length;
        boolean complicated = false;
        for (int pos = start; pos < end && !complicated; pos++) {
            byte c = content[pos];
            if (c <= 0 || c >= 0x7F || (c == '=' && pos < end - 1 && content[pos + 1] == '?')) {
                complicated = true;
            }
        }
        if (!complicated) {
            return unfold(createString(content, start, length, charset));
        }

        List<FieldElement> results = parse(content, start, length);
        if (null == results) {
            // Failed to parse the content.  Just unfold the content and return.
            return unfold(createString(content, start, length, charset));
        }

        FieldElement prev    = null;
        FieldElement current = null;

        int last = results.size();
        StringBuilder text = new StringBuilder();
        StringBuilder decodeMe = new StringBuilder();
        for (int i = 0; i < last; i++) {
            prev = current;
            current = results.get(i);

            if (current.getSeqType() == SequenceType.LWS) {
                if (prev != null && prev.getSeqType() == SequenceType.EW) {
                    decodeAndAppend(decodeMe, text);
                    decodeMe.setLength(0);
                }
                text.append(current.getText());
            } else if (current.getSeqType() == SequenceType.EW) {
                if (prev != null && prev.getSeqType() == SequenceType.EW &&
                        prev.getCharset().equalsIgnoreCase(current.getCharset()) &&
                        prev.getDecodeType().equalsIgnoreCase(current.getDecodeType())) {
                    int endIdx = decodeMe.length() - 1;
                    if ("B".equalsIgnoreCase(current.getDecodeType()) && endIdx > 1 &&
                            decodeMe.charAt(endIdx) == '=') {
                        // This chunk is correctly padded
                        decodeAndAppend(decodeMe, text);
                        setupNewNeedDecode(decodeMe, current);
                    } else if ("Q".equalsIgnoreCase(current.getDecodeType()) && endIdx > 1 &&
                            decodeMe.charAt(endIdx) == '=') {
                        /* RFC 2047 Section 5. (3)
                         * ```
                         * The 'encoded-text' in each 'encoded-word' must be well-formed according
                         * to the encoding specified; the 'encoded-text' may not be continued in
                         * the next 'encoded-word'.  (For example, "=?charset?Q?=?=
                         * =?charset?Q?AB?=" would be illegal, because the two hex digits "AB"
                         * must follow the "=" in the same 'encoded-word'.)
                         * ```
                        */
                        text.append(decodeMe);
                        setupNewNeedDecode(decodeMe, current);
                    } else {
                        decodeMe.append(current.getText());
                    }
                } else if (prev != null && prev.getSeqType() == SequenceType.EW) {
                    decodeAndAppend(decodeMe, text);
                    setupNewNeedDecode(decodeMe, current);
                } else {
                    setupNewNeedDecode(decodeMe, current);
                }
            } else if (current.getSeqType() == SequenceType.COMMENT) {
                if (prev != null && prev.getSeqType() == SequenceType.EW) {
                    decodeAndAppend(decodeMe, text);
                    decodeMe.setLength(0);
                }
                text.append(current.getText());
            }
        }
        if (decodeMe.length() > 0) {
            decodeAndAppend(decodeMe, text);
        }
        return text.toString();
    }

    static private void setupNewNeedDecode(StringBuilder needDecode, FieldElement current) {
        needDecode.setLength(0);
        needDecode.append("=?")
                  .append(current.getCharset())
                  .append("?")
                  .append(current.getDecodeType())
                  .append("?")
                  .append(current.getText());
    }

    static private void decodeAndAppend(StringBuilder needDecode, StringBuilder text) {
        needDecode.append("?=");
        ZByteString decoded = ZMimeUtility.decodeWordBytes(needDecode.toString().getBytes());
        if (null != decoded) {
            text.append(decoded);
        } else {
            text.append(needDecode.toString());
        }
    }

    public enum SequenceType {UNDEFINED, ERROR, COMMENT /* comment */, EW /* encoded-word */, LWS /* linear-white-space */};
    public enum EncodeSequenceState {CHARSET, ENCODEMETHOD, TEXT, UNDEFINED};
    static public class FieldElement {
        private SequenceType seqType;
        private ByteArrayOutputStream bytes;
        private ByteArrayOutputStream charset;
        private ByteArrayOutputStream decodeType;

        public FieldElement(SequenceType type) {
            seqType = type;
            bytes = new ByteArrayOutputStream();
            charset = new ByteArrayOutputStream();
            decodeType = new ByteArrayOutputStream();
        }
        public SequenceType getSeqType()             { return seqType; }
        public String getText()                      { return bytes.toString(); }
        public String getCharset()                   { return charset.toString(); }
        public String getDecodeType()                { return decodeType.toString(); }
        public void setSeqType(SequenceType seqType) { this.seqType = seqType;}
        public void appendBody(byte b)               { this.bytes.write(b); }
        public void appendCharset(byte b)            { this.charset.write(b); }
        public void appendDecodeType(byte b)         { this.decodeType.write(b); }
        public boolean isPadding(int index)          { return (bytes.toByteArray()[index] == '='); }
        public void reset() {
            seqType = SequenceType.UNDEFINED;
            bytes.reset();
            charset.reset();
            decodeType.reset();
        }
    }

    /**
     * Wrapper for the List of FieldElement
     *
     */
    static class Fields {
        private List<FieldElement> fields = new ArrayList<FieldElement>();
        public void add(FieldElement element) {
            int end = fields.size() - 1;
            if (element.getSeqType() != SequenceType.EW && element.getText().isEmpty()) {
                return;
            }

            if (end > 0 && element.getSeqType() == SequenceType.EW &&
                       fields.get(end).getSeqType() == SequenceType.LWS &&
                       fields.get(end - 1).getSeqType() == SequenceType.EW) {
                // LWS between the EWs will be ignored
                fields.remove(end);
                fields.add(element);
            } else {
                fields.add(element);
            }
        }
        public List<FieldElement> getAll() {
            return fields;
        }
    }

    /**
     * Parse the text into tokens with the state and content
     * @param content target text
     * @param start index of start point
     * @param length length
     * @return a list of parsed FieldElement.  If the content text is mal-formatted, return null.
     */
    static private List<FieldElement> parse(final byte[] content, final int start, final int length) {
        Fields fields = new Fields();
        final int end = start + length;

        SequenceType currStat = SequenceType.COMMENT;
        EncodeSequenceState encodeStat = EncodeSequenceState.UNDEFINED;
        FieldElement currElement = new FieldElement(SequenceType.COMMENT);
        for (int pos = start; pos < end; pos++) {
            if (content[pos] == '\r' || content[pos] == '\n') {
                continue;
            }
            if (currStat == SequenceType.COMMENT) {
                if (content[pos] == ' ' || content[pos] == '\t') {
                    fields.add(currElement);
                    currStat = SequenceType.LWS;
                    currElement = new FieldElement(currStat);
                    currElement.appendBody(content[pos]);
                } else if ((pos < (end - 1)) && content[pos] == '=' && content[pos + 1] == '?' ) {
                    int hasCharset = 0;
                    for (int subpos = pos + 2; subpos < end && hasCharset < 3; subpos++) {
                        if (content[subpos] == '?') {
                            hasCharset++;
                        }
                    }
                    if (hasCharset == 3) {
                        fields.add(currElement);
                        currStat = SequenceType.EW;
                        currElement = new FieldElement(currStat);
                    } else {
                        currStat = SequenceType.ERROR;
                    }
                } else {
                    currElement.appendBody(content[pos]);
                }
            } else if (currStat == SequenceType.EW) {
                if ((content[pos] == ' ' || content[pos] == '\t') && !allowInvalidEncoding(currElement.getCharset())) {
                    return null;
                } else if (encodeStat == EncodeSequenceState.TEXT && (pos < (end - 1)) && content[pos] == '?' && content[pos + 1] == '=' ) {
                    pos++;
                    fields.add(currElement);
                    encodeStat = EncodeSequenceState.UNDEFINED;
                    currStat = SequenceType.COMMENT;
                    currElement = new FieldElement(currStat);
                } else if (content[pos] == '?') {
                    if (encodeStat == EncodeSequenceState.UNDEFINED) {
                        encodeStat = EncodeSequenceState.CHARSET;
                    } else if (encodeStat == EncodeSequenceState.CHARSET) {
                        encodeStat = EncodeSequenceState.ENCODEMETHOD;
                    } else if (encodeStat == EncodeSequenceState.ENCODEMETHOD) {
                        encodeStat = EncodeSequenceState.TEXT;
                    } else {
                        encodeStat = EncodeSequenceState.UNDEFINED;
                    }
                } else {
                    if (encodeStat == EncodeSequenceState.CHARSET) {
                        currElement.appendCharset(content[pos]);
                    } else if (encodeStat == EncodeSequenceState.ENCODEMETHOD) {
                        currElement.appendDecodeType(content[pos]);
                    } else {
                        currElement.appendBody(content[pos]);
                    }
                }
            } else if (currStat == SequenceType.LWS) {
                if (content[pos] == ' ' || content[pos] == '\t') {
                    currElement.appendBody(content[pos]);
                } else if ((pos < (end - 1)) && content[pos] == '=' && content[pos + 1] == '?' ) {
                    fields.add(currElement);
                    currStat = SequenceType.EW;
                    currElement = new FieldElement(currStat);
                } else {
                    fields.add(currElement);
                    currStat = SequenceType.COMMENT;
                    currElement = new FieldElement(currStat);
                    currElement.appendBody(content[pos]);
                }
            } else {
                return null;
            }
        }
        fields.add(currElement);
        return fields.getAll();
    }

    public static boolean allowInvalidEncoding(String charset) {

        return ("iso-8859-1".equalsIgnoreCase(charset) || "us-ascii".equalsIgnoreCase(charset)
            || ("utf-8").equalsIgnoreCase(charset)) ;
    }

    static String unfold(final String folded) {
        int length = folded.length(), i;
        for (i = 0; i < length; i++) {
            char c = folded.charAt(i);
            if (c == '\r' || c == '\n') {
                break;
            }
        }
        if (i == length) {
            return folded;
        }

        StringBuilder unfolded = new StringBuilder(length);
        if (i > 0) {
            unfolded.append(folded, 0, i);
        }
        while (++i < length) {
            char c = folded.charAt(i);
            if (c != '\r' && c != '\n') {
                unfolded.append(c);
            }
        }
        return unfolded.toString();
    }

    static class EncodedWord {
        static String encode(final String value, final Charset requestedCharset) {
            StringBuilder sb = new StringBuilder();

            // FIXME: need to limit encoded-words to 75 bytes
            Charset charset = CharsetUtil.checkCharset(value, requestedCharset);
            byte[] content = null;
            try {
                content = value.getBytes(charset);
            } catch (OutOfMemoryError e) {
                try {
                    ZimbraLog.system.fatal("out of memory", e);
                } finally {
                    Runtime.getRuntime().halt(1);
                    content = new byte[0];  // never reachable, but averts compiler warnings
                }
            } catch (Throwable t) {
                content = value.getBytes(CharsetUtil.ISO_8859_1);
                charset = CharsetUtil.ISO_8859_1;
            }
            sb.append("=?").append(charset.name().toLowerCase());

            int invalidQ = 0;
            for (byte b : content) {
                if (b < 0 || Q2047Encoder.FORCE_ENCODE[b]) {
                    invalidQ++;
                }
            }

            InputStream encoder;
            if (invalidQ > content.length / 3) {
                sb.append("?B?");  encoder = new B2047Encoder(content);
            } else {
                sb.append("?Q?");  encoder = new Q2047Encoder(content);
            }

            try {
                sb.append(new String(ByteUtil.readInput(encoder, 0, Integer.MAX_VALUE)));
            } catch (IOException ioe) {
            }
            sb.append("?=");

            return sb.toString();
        }

        private static class Q2047Encoder extends ZTransferEncoding.QuotedPrintableEncoderStream {
            static final boolean[] FORCE_ENCODE = new boolean[128];
            static {
                for (int i = 0; i < FORCE_ENCODE.length; i++) {
                    FORCE_ENCODE[i] = true;
                }
                for (int c : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!*+-/ ".getBytes()) {
                    FORCE_ENCODE[c] = false;
                }
            }

            Q2047Encoder(final byte[] content) {
                super(new ByteArrayInputStream(content), null);
                disableFolding();
                setForceEncode(FORCE_ENCODE);
            }

            @Override
            public int read() throws IOException {
                int c = super.read();
                return c == ' ' ? '_' : c;
            }
        }

        private static class B2047Encoder extends ZTransferEncoding.Base64EncoderStream {
            B2047Encoder(byte[] content) {
                super(new ByteArrayInputStream(content));
                disableFolding();
            }
        }
    }

    /** Characters that can form part of an RFC 5322 atom. */
    static final boolean[] ATEXT_VALID = new boolean[128];
    static {
        for (int c : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!#$%&'*+-/=?^_`{|}~".getBytes()) {
            ATEXT_VALID[c] = true;
        }
    }

    public static String escape(final String value, final Charset charset, final boolean phrase) {
        boolean needsQuote = false, wsp = true;
        int needs2047 = 0, needsEscape = 0, cleanTo = 0, cleanFrom = value.length();
        for (int i = 0, len = value.length(); i < len; i++) {
            char c = value.charAt(i);
            if (c > 0x7F || c == '\0' || c == '\r' || c == '\n') {
                needs2047++;  cleanFrom = len;
            } else if (!phrase) {
                // if we're not in an RFC 5322 phrase, there is no such thing as "quoting"
            } else if (c == '"' || c == '\\') {
                needsQuote = true;  needsEscape++;  cleanFrom = len;
            } else if ((c != ' ' && !ATEXT_VALID[c]) || (c == ' ' && wsp)) {
                needsQuote = true;  cleanFrom = len;
            }
            wsp = c == ' ';
            if (wsp) {
                if (!needsQuote && needs2047 == 0 && i != len - 1) {
                    cleanTo = i + 1;
                } else if (cleanFrom == len && i > cleanTo + 1) {
                    cleanFrom = i;
                }
            }
        }
        if (phrase) {
            needsQuote |= wsp;
        }
        if (wsp) {
            cleanFrom = value.length();
        }

        if (needs2047 > 0) {
            String prefix = value.substring(0, cleanTo), suffix = value.substring(cleanFrom);
            return prefix + EncodedWord.encode(value.substring(cleanTo, cleanFrom), charset) + suffix;
        } else if (needsQuote && needsEscape > 0) {
            return quote(value, needsEscape);
        } else if (needsQuote) {
            return new StringBuilder(value.length() + 2).append('"').append(value).append('"').toString();
        } else {
            return value;
        }
    }

    static String quote(final String value) {
        return quote(value, 0);
    }

    private static String quote(final String value, final int escapeHint) {
        StringBuilder sb = new StringBuilder(value.length() + escapeHint + 2).append('"');
        for (int i = 0, len = value.length(); i < len; i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.append('"').toString();
    }
}
