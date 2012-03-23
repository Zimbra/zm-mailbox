/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.common.zmime;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimePartDataSource;
import javax.mail.internet.SharedInputStream;
import javax.mail.util.SharedByteArrayInputStream;

import com.sun.mail.util.ASCIIUtility;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CharsetUtil;

class ZMimeParser {
    private static final Charset DEFAULT_CHARSET = CharsetUtil.normalizeCharset(CharsetUtil.ISO_8859_1);

    ZMimeParser(ZMimePart part, Session s, SharedInputStream shared) {
        Charset sessionCharset = s == null ? null : CharsetUtil.toCharset(s.getProperty("mail.mime.charset"));

        parts.add(new PartInfo(part));
        this.sis = shared;
        this.toplevel = part;
        this.session = s;
        this.charset = CharsetUtil.normalizeCharset(sessionCharset == null ? DEFAULT_CHARSET : sessionCharset);
        this.header = new ZMimeUtility.ByteBuilder(80, charset);
    }

    ZMimeParser(ZMimePart part, Session s, InputStream is) throws MessagingException {
        this(part, s, forceSharedStream(is));
    }

    private static SharedInputStream forceSharedStream(InputStream is) throws MessagingException {
        if (is instanceof SharedInputStream) {
            return (SharedInputStream) is;
        } else {
            InputStream source = is;
            if (!(source instanceof ByteArrayInputStream) && !(source instanceof BufferedInputStream)) {
                source = new BufferedInputStream(source);
            }

            try {
                return new SharedByteArrayInputStream(ASCIIUtility.getBytes(source));
            } catch (IOException ioex) {
                throw new MessagingException("IOException", ioex);
            }
        }
    }

    public static ZMimeMessage parse(Session session, SharedInputStream sis) throws IOException {
        return (ZMimeMessage) new ZMimeParser(ZMimeMessage.newMessage(session, null), session, sis).parse().getPart();
    }

    public static ZMimeMultipart parseMultipart(ZMimeMultipart multi, InputStream is) throws MessagingException, IOException {
        ZMimeParser parser = new ZMimeParser(new ZMimeBodyPart(), Session.getDefaultInstance(new Properties()), is);

        PartInfo proot = parser.parts.get(0);
        proot.setContentType(new ZContentType(multi.getContentType()));
        proot.bodyStart = 0;
        proot.firstLine = 0;
        proot.multi = multi;

        parser.parts.add(parser.new PartInfo(ZMimeBodyPart.newBodyPart(null), PartLocation.PREAMBLE));
        parser.state = ParserState.BODY_LINESTART;
        parser.bodyStart(0);
        parser.parse();

        return multi;
    }


    private enum ParserState {
        HEADER_LINESTART, HEADER, HEADER_CR,
        BODY_LINESTART, BODY, BODY_CR,
        TERMINATED
    }

    private enum LineEnding { CR, LF, CRLF }

    private static class BoundaryChecker {
        /** A {@code Map} mapping active boundary strings which have matched
         *  all checked bytes in the line thus far to the number of trailing
         *  dashes after the end of the boundary.  {@code null} value means
         *  that we have not yet checked the last byte in the boundary;
         *  <tt>0</tt> means that the boundary matched completely without
         *  trailing dashes, <tt>2</tt> means that the boundary has matched
         *  and there are 2 trailing dashes (i.e. an end boundary). */
        private final Map<String, Integer> boundaryCandidates;

        /** The byte position of the end of the current part if this line
         *  matches an active boundary. */
        private final long partEnd;

        /** If there is a blank boundary in the list, we keep a copy of the
         *  content of the line for use in matching subsequent boundaries. */
        private StringBuilder boundary;

        BoundaryChecker(List<String> boundaries, long lineStart, LineEnding lastEnding) {
            boundaryCandidates = new LinkedHashMap<String, Integer>(boundaries.size());
            for (String bnd : boundaries) {
                if (bnd.isEmpty()) {
                    // "" means no "boundary" param on the Content-Type
                    //   (to handle this case, match the first dash-dash-anything line)
                    boundary = new StringBuilder(80);
                    boundaryCandidates.put(bnd, 0);
                } else {
                    boundaryCandidates.put(bnd, null);
                }
            }

            // RFC 2046 5.1.1: "The boundary delimiter MUST occur at the beginning of
            //   a line, i.e., following a CRLF, and the initial CRLF is considered to
            //   be attached to the boundary delimiter line rather than part of the
            //   preceding part.
            partEnd = lineStart - (lastEnding == LineEnding.CRLF ? 2 : 1);
        }

        /** Checks a byte against all of the currently active boundaries that
         *  haven't failed a byte check yet this line.  If a boundary doesn't
         *  match the appropriate character, it is removed from the set.<p>
         *
         *  <i>Should really switch to having {@code index} be an
         *  auto-incremented counter managed by the BoundaryChecker rather than
         *  a parameter to this method.</i>
         * @param b      The byte being checked.
         * @param index  The position in the boundary strings to check against
         *               <tt>b</tt>.
         * @return whether any potential matches remain for this line. */
        boolean checkByte(byte b, int index) {
            char c = (char) (b & 0xFF);
            for (Iterator<Map.Entry<String, Integer>> it = boundaryCandidates.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Integer> bndData = it.next();
                String bnd = bndData.getKey();

                if (bnd.isEmpty()) {
                    // "" means unspecified boundary, which matches anything starting with 2 dashes
                    continue;
                } else if (index >= bnd.length()) {
                    // end boundaries are represented by exactly 2 dashes after a boundary string
                    int trailers = bndData.getValue();
                    if (c == '-' && trailers < 2) {
                        bndData.setValue(trailers + 1);
                    } else if (c != ' ' && c != '\t') {
                        it.remove();
                    }
                } else {
                    // FIXME: comparison probably wrong for high-bit-set bytes
                    if (bnd.charAt(index) != c) {
                        it.remove();
                    } else if (index == bnd.length() - 1) {
                        bndData.setValue(0);
                    }
                }
            }

            if (boundary != null) {
                boundary.append(c);
            }

            return !boundaryCandidates.isEmpty();
        }

        /** Returns info on the boundary that matched the current line, or
         *  <tt>null</tt> if there was no match.
         * @return a {@code Map.Entry} whose <tt>key</tt> is the boundary
         *         that was matched and whose <tt>value</tt> is the number of
         *         trailing dashes (either 0 or 2). */
        Map.Entry<String, Integer> getMatch() {
            Map.Entry<String, Integer> match = null;
            // want the *last* match, since we add to the end of the hash
            for (Map.Entry<String, Integer> bndData : boundaryCandidates.entrySet()) {
                int trailers = bndData.getValue() == null ? -1 : bndData.getValue();
                if (trailers == 0 || trailers == 2) {
                    // valid number of trailing dashes are 0 and 2
                    if (match == null || !match.getKey().isEmpty()) {
                        // tweak to try to not let blank boundaries trump explicit ones
                        match = bndData;
                    }
                }
            }
            return match;
        }

        String getSavedBoundary() {
            return boundary == null ? "" : boundary.toString();
        }

        long getPartEnd() {
            return partEnd;
        }

        static String normalizeBoundary(String bnd) {
            String boundary = bnd == null ? "" : bnd;
            // RFC 2046 5.1.1: "The only mandatory global parameter for the "multipart" media type is
            //                  the boundary parameter, which consists of 1 to 70 characters from a
            //                  set of characters known to be very robust through mail gateways, and
            //                  NOT ending with white space."
            while (boundary.length() > 0 && Character.isWhitespace(boundary.charAt(boundary.length() - 1))) {
                boundary = boundary.substring(0, boundary.length() - 1);
            }
            return boundary;
        }
    }

    enum PartLocation { PREAMBLE, CONTENT, EPILOGUE }

    private class PartInfo {
        ZMimePart part;
        ZMimeMultipart multi;
        int firstLine;
        long bodyStart = -1;
        String boundary;  // value: part boundary | "": unspecified boundary | null: not a multipart
        PartLocation location;
        ZContentType ctype;

        PartInfo(ZMimePart mp) {
            this(mp, PartLocation.CONTENT);
        }

        PartInfo(ZMimePart mp, PartLocation loc) {
            this.part = mp;
            this.location = loc;
        }

        void setContentType(ZContentType ct) {
            // in JavaMail land, first content-type always wins
            if (ctype != null)
                return;

            this.ctype = ct;
            if (ctype.getPrimaryType().equals("multipart")) {
                String bnd = BoundaryChecker.normalizeBoundary(ctype.getParameter("boundary"));
                this.boundary = bnd == null ? "" : bnd;
                recalculateBoundaries();
            } else if (boundary != null) {
                this.boundary = null;
                recalculateBoundaries();
            }
        }

        @Override
        public String toString() {
            return ctype == null ? null : ctype.getBaseType();
        }
    }

    private final ZMimePart toplevel;
    private final Session session;
    private final SharedInputStream sis;
    private final Charset charset;

    /** The current state of the parser.  Generally, a combination of which
     *  type of parsing is going on (HEADER vs. BODY) and where in the line
     *  we are (LINESTART, after CR, etc.). */
    protected ParserState state = ParserState.HEADER_LINESTART;

    /** The stack of active message parts, outermost to innermost. */
    private final List<PartInfo> parts = new ArrayList<PartInfo>(5);

    /** The parser's current position in the message (in bytes). */
    private long position;

    /** The position in the message of the first character of the current
     *  line.
     * @see #position */
    private long lineStart;

    /** The current (0-based) line number in the message. */
    private int lineNumber;

    /** The set of characters (CR, LF, or CRLF) that terminated the previous
     *  line. */
    protected LineEnding lastEnding;

    /** The set of active MIME boundaries.  Any line consisting of two
     *  '<tt>-</tt>' characters, a member of this list, and a newline is
     *  considered a boundary line which terminates the current part. */
    private List<String> boundaries;

    /** The number of leading '<tt>-</tt>' characters on a line, used only
     *  when there are MIME boundaries active.  When this counter reaches
     *  <tt>2</tt>, we start matching input bytes against active
     *  boundaries.*/
    private int dashes;

    /** The set of active MIME boundaries that match the current line thus
     *  far.  This object is instantiated to contain all the strings in {@link
     *  #boundaries} when {@link #dashes} reaches <tt>2</tt>.  Candidates are
     *  removed from the set when subsequent bytes in the line fail to match.
     *  When the set is empty, {@link #boundaryChecker} is reset back to
     *  <tt>null</tt>. */
    private BoundaryChecker boundaryChecker;

    private static final int MAXIMUM_HEADER_LENGTH = 65536;

    /** The entire content of the current header.  This includes the name,
     *  the colon, the raw header value, any folding, and the trailing CRLF. */
    private final ZMimeUtility.ByteBuilder header;


    /** Terminates message parsing and returns the {@code ZMimeMessage}
     *  resulting from the parse.  <b>Do not call this method until the entire
     *  message has been passed through the parser</b>, otherwise incorrect
     *  lengths may be recorded. */
    ZMimePart getPart() {
        endParse();
        return toplevel;
    }

    /** Returns the structure representing the "currently active" part being
     *  handled by the parser. */
    protected PartInfo currentPart() {
        return parts.isEmpty() ? null : parts.get(parts.size() - 1);
    }

    long getPosition() {
        return position;
    }

    int getLineNumber() {
        return lineNumber;
    }


    ZMimeParser parse() throws IOException {
        InputStream is = (InputStream) sis;
        final byte[] buffer = new byte[8192];
        int read;
        do {
            read = is.read(buffer, 0, buffer.length);
            if (read > 0) {
                handleBytes(buffer, 0, read);
            }
        } while (read >= 0);

        return endParse();
    }

    /** Hands the appropriate range of bytes to the parser, one at a time.
     * @see #handleByte(byte) */
    void handleBytes(byte[] b, int off, int len) {
        if (len > 0) {
            for (int pos = off, max = Math.min(b.length, off + len); pos < max; pos++) {
                handleByte(b[pos]);
            }
        }
    }

    /** Handles a single byte of the message.  This small state machine tracks
     *  line starts and the transitions between message/MIME headers and part
     *  bodies.<p>
     *
     *  Recursive calls to this function will get you in trouble.  Yes, I know,
     *  we call it recursively.  <i>sigh</i> */
    boolean handleByte(byte b) {
        switch (state) {
            // after a CR character at the end of a header line, expecting an LF
            case HEADER_CR:
                state = ParserState.HEADER_LINESTART;
                if (b == '\n') {
                    addHeaderByte(b);
                    lastEnding = LineEnding.CRLF;
                    break;
                }
                // \r without \n -- treat this as the first character of the next line
                lastEnding = LineEnding.CR;
                addHeaderByte((byte) '\n');
                //$FALL-THROUGH$

            // at the beginning of a header line, after a CR/LF/CRLF
            case HEADER_LINESTART:
                if (processBoundary()) {
                    // found a part boundary, which may transition us to body parsing
                    return handleByte(b);
                } else if (misencodedCRLF()) {
                    // broken MUA sent the CRLF as "=0D\n" -- treat as header/body separator
                    clearHeader();
                    state = ParserState.BODY_LINESTART;
                    return handleByte(b);
                }
                newline();
                if (b == ' ' || b == '\t') {
                    // folded line; header value continues
                    addHeaderByte(b);
                    state = ParserState.HEADER;
                    break;
                }
                // not a folded header line, so record the header and continue
                saveHeader();
                // check for a blank line, which terminates the header
                if (b == '\n') {
                    lastEnding = LineEnding.LF;
                    state = ParserState.BODY_LINESTART;
                    break;
                } else if (b == '\r') {
                    state = ParserState.BODY_CR;
                    break;
                }
                state = ParserState.HEADER;
                //$FALL-THROUGH$

            // in a header line, after reading at least one byte
            case HEADER:
                addHeaderByte(b);
                if (b == '\n') {
                    lastEnding = LineEnding.LF;
                    if (!header.endsWith((byte) '\r')) {
                        header.pop().append('\r').append('\n');
                    }
                    state = ParserState.HEADER_LINESTART;
                } else if (b == '\r') {
                    state = ParserState.HEADER_CR;
                }
                break;

            // after a CR character at the end of a body line, expecting an LF
            case BODY_CR:
                if (b == '\n') {
                    lastEnding = LineEnding.CRLF;
                    state = ParserState.BODY_LINESTART;
                    break;
                }
                // \r without \n -- treat this as the first character of the next line
                lastEnding = LineEnding.CR;
                //$FALL-THROUGH$

            // at the beginning of a body line, after a CR/LF/CRLF
            case BODY_LINESTART:
                if (processBoundary()) {
                    // found a part boundary, which may transition us to header parsing
                    return handleByte(b);
                }
                newline();
                state = ParserState.BODY;
                if (currentPart().bodyStart < 0) {
                    // first line of the body part; we're now far enough along that we can create and store the MimePart
                    if (bodyStart(position)) {
                        // in one case (message/rfc822 attachments), starting the "body" transitions us back to header parsing...
                        return handleByte(b);
                    }
                }
                //$FALL-THROUGH$

            // somewhere within a body line
            case BODY:
                if (b == '\n') {
                    lastEnding = LineEnding.LF;
                    state = ParserState.BODY_LINESTART;
                } else if (b == '\r') {
                    state = ParserState.BODY_CR;
                }
                // XXX: could decode the cte and figure out unencoded part lengths...
                break;

            case TERMINATED:
                throw new IllegalStateException("parsing has already been terminated");
        }

        // if there are active MIME boundaries, check whether this line could match one of them
        if (boundaries != null) {
            checkBoundary(b);
        }

        position++;

        return true;
    }

    /** Registers a newline by updating the parser's line-oriented counters. */
    private boolean newline() {
        if (lineStart != position) {
            lineNumber++;  lineStart = position;  dashes = 0;  boundaryChecker = null;
            return true;
        } else {
            return false;
        }
    }

    /** Incrementally checks whether the current byte matches a MIME boundary.
     *  A MIME boundary is defined as a line consisting of two dashes
     *  ("<tt>--</tt>"), followed by the "boundary" parameter to an enclosing
     *  "multipart/*" part, optionally followed by two more dashes, optionally
     *  followed by whitespace, followed by a newline. */
    private void checkBoundary(byte b) {
        if (boundaries != null && b == '-' && dashes == position - lineStart && dashes < 2) {
            // 2 leading dashes may mean a MIME boundary
            if (++dashes == 2) {
                boundaryChecker = new BoundaryChecker(boundaries, lineStart, lastEnding);
            }
        } else if (dashes == 2 && boundaryChecker != null && b != '\r' && b != '\n') {
            int index = (int) (position - lineStart - 2);
            if (!boundaryChecker.checkByte(b, index)) {
                // no matching boundaries, so no need to check further
                boundaryChecker = null;
            }
        }
    }

    /** Checks whether a boundary was matched and, if so, handles it.  As a
     *  side effect, resets the boundary checking state for a new line.
     * @return Whether a boundary was matched. */
    private boolean processBoundary() {
        String bnd = null;
        boolean isEnd = false;
        long partEnd = -1;

        if (boundaryChecker != null) {
            Map.Entry<String, Integer> match = boundaryChecker.getMatch();
            if (match != null) {
                bnd = match.getKey();
                if (bnd.isEmpty()) {
                    String actualBoundary = boundaryChecker.getSavedBoundary();
                    bnd = actualBoundary.endsWith("--") ? null : actualBoundary;
                }
                isEnd = match.getValue() == 2;
                partEnd = boundaryChecker.getPartEnd();
            }
        }

        dashes = 0;  boundaryChecker = null;

        if (bnd != null) {
            boundary(bnd, isEnd, partEnd);
            return true;
        } else {
            return false;
        }
    }

    /** Handles a MIME boundary.  Closes parts up to the multipart that matches
     *  the boundary.  Starts a new part, either a new content part (if it was
     *  a start boundary) or a multipart epilogue (if it was an end boundary).
     * @param bnd      The matched boundary string.
     * @param isEnd    Whether it was an end boundary.
     * @param partEnd  The byte position of the end of the part(s) being closed
     *                 as a result of this boundary. */
    private void boundary(String bnd, boolean isEnd, long partEnd) {
        clearHeader();

        // close the current part
        PartInfo pcurrent = currentPart();
        if (pcurrent.bodyStart < 0) {
            // case where the boundary came in the middle of the headers (?!)
            bodyStart(lineStart);
        }
        // figure out which multipart matched this boundary
        int matchIndex;
        for (matchIndex = parts.size() - 1; matchIndex >= 0; matchIndex--) {
            PartInfo pinfo = parts.get(matchIndex);
            if (bnd.equals(pinfo.boundary))
                break;
            if ("".equals(pinfo.boundary) && (boundaries == null || !boundaries.contains(bnd)))
                break;
        }
        // close all parts up to that matching part (note: size() is 1-based and matchIndex is 0-based)
        while (parts.size() > Math.max(matchIndex + 1, 1)) {
            pcurrent = endPart(partEnd, matchIndex == parts.size() - 2);
        }
        if ("".equals(pcurrent.boundary)) {
            // "" meant that the multipart Content-Type didn't specify a boundary
            //   so pick the next new boundary we see and *that* is the boundary
            pcurrent.boundary = pcurrent.multi.implicitBoundary = BoundaryChecker.normalizeBoundary(bnd);
        }

        // set up the new part
        if (isEnd) {
            // now that we've hit the end boundary, this boundary is no longer active
            pcurrent.boundary = null;
            pcurrent.multi.markComplete();
            pcurrent.multi = null;
            state = ParserState.BODY_LINESTART;
        } else {
            // new proper subpart of the multipart -- starting with its MIME headers
            parts.add(pcurrent = new PartInfo(ZMimeBodyPart.newBodyPart(pcurrent.multi)));
            state = ParserState.HEADER_LINESTART;
        }

        recalculateBoundaries();
    }

    /** Regenerates the list of valid MIME boundaries from the set of active
     *  enclosing parts.  Sets {@link #boundaries} appropriately, or to
     *  {@code null} if there are no valid boundaries. */
    void recalculateBoundaries() {
        boundaries = new ArrayList<String>(parts.size());
        for (PartInfo pinfo : parts) {
            if (pinfo.boundary != null) {
                boundaries.add(pinfo.boundary);
            }
        }
        if (boundaries.isEmpty()) {
            boundaries = null;
        }
    }

    /** Records the endpoint of a MIME part.  Stores both the byte offset of
     *  the end of the part as well as the line count of the part body.  If
     *  the part being ended was a multipart preamble and was of nonzero
     *  length, associates the part with its parent appropriately. */
    private PartInfo endPart(long end, boolean clean) {
        PartInfo pinfo = parts.remove(parts.size() - 1);

        ZMimePart mp = pinfo.part;
        long bodyEnd = Math.max(pinfo.bodyStart, end), length = bodyEnd - pinfo.bodyStart;
        SharedInputStream bodyStream = (SharedInputStream) sis.newStream(pinfo.bodyStart, bodyEnd);

        if (pinfo.location == PartLocation.PREAMBLE) {
            if (!clean && !parts.isEmpty()) {
                PartInfo pcurrent = currentPart();
                String enc = pcurrent.part.getEncoding();
                if (enc != null && !ZMimeBodyPart.RAW_ENCODINGS.contains(enc)) {
                    // supposedly-encoded multipart and no boundary hit -- defer decoding and parsing
                    pcurrent.multi.setDataSource(new MimePartDataSource(pcurrent.part));
                    // don't save this as a preamble!
                    length = 0;
                }
            }
            if (length > 0) {
                try {
                    if (length > MAXIMUM_HEADER_LENGTH) {
                        // constrain preamble length to some reasonable value (64K)
                        bodyStream = (SharedInputStream) bodyStream.newStream(0, length = MAXIMUM_HEADER_LENGTH);
                    }
                    // save preamble to *parent* multipart
                    String preamble = new String(ByteUtil.readInput((InputStream) bodyStream, (int) length, (int) length), charset);
                    currentPart().multi.setPreamble(preamble);
                } catch (IOException ioe) {
                }
            }
        } else {
            mp.endPart(bodyStream, length, lineNumber - pinfo.firstLine);
        }

        return currentPart();
    }

    private boolean addHeaderByte(byte b) {
        if (header.size() <= MAXIMUM_HEADER_LENGTH) {
            header.append(b);
            return true;
        } else if (header.endsWith((byte) '\n')) {
            // it's too long and it's already terminated -- we're done
            return false;
        } else if (header.endsWith((byte) '\r')) {
            header.append('\n');
            return false;
        } else {
            header.append('\r').append('\n');
            return false;
        }
    }

    /** Resets all header-related members after processing a header line. */
    private void clearHeader() {
        header.reset();
    }

    boolean misencodedCRLF() {
        return header.size() == 5 && header.byteAt(0) == '=' && header.byteAt(1) == '0' && header.byteAt(2) == 'D' && currentPart().part.getEncoding().equals("quoted-printable");
    }

    /** Adds the current header to the active part's header block.  If the
     *  header is "<tt>Content-Type</tt>" and it's a <tt>multipart/*</tt>,
     *  updates the set of active MIME boundaries. */
    protected void saveHeader() {
        if (header.isEmpty())
            return;

        PartInfo pcurrent = currentPart();
        ZInternetHeader zhdr = new ZInternetHeader(header.toByteArray());
        pcurrent.part.appendHeader(zhdr);

        if (zhdr.getName().equalsIgnoreCase("Content-Type")) {
            pcurrent.setContentType(new ZContentType(zhdr, defaultContentType()));
        }

        clearHeader();
    }

    private String defaultContentType() {
        PartInfo parent = parts.size() <= 1 ? null : parts.get(parts.size() - 2);
        boolean inDigest = parent != null && parent.ctype.getBaseType().equals("multipart/digest");
        boolean isPreamble = currentPart().location == PartLocation.PREAMBLE;
        return inDigest && !isPreamble ? ZContentType.MESSAGE_RFC822 : ZContentType.TEXT_PLAIN;
    }

    /** Marks the transition from parsing MIME/message headers to skimming the
     *  part body.  Creates the corresponding {@code MimePart} object for the
     *  current active part and clears the {@code ZInternetHeaders} that was
     *  accumulating part headers.
     * @param pos  The byte offset of the beginning of the part body.
     * @return Whether the part was a <tt>message/rfc822</tt>, which requires
     *         a parser state transition back to header reading. */
    private boolean bodyStart(long pos) {
        PartInfo pcurrent = currentPart();

        ZContentType ctype = pcurrent.ctype;
        if (ctype == null) {
            // if there was no Content-Type header, use the default
            //   (e.g. text/plain unless we're in a multipart/digest)
            pcurrent.ctype = ctype = new ZContentType(defaultContentType());
        }

        // we're now far enough along to transition from header mode to body mode
        pcurrent.bodyStart = pos;
        pcurrent.firstLine = lineNumber;

        if (ctype.getPrimaryType().equals("multipart")) {
            pcurrent.multi = ZMimeMultipart.newMultipart(ctype, pcurrent.part);
            // create the preamble, which encloses the lines up to the first boundary in its *body*
            parts.add(pcurrent = new PartInfo(ZMimeBodyPart.newBodyPart(null), PartLocation.PREAMBLE));
            bodyStart(pos);
        } else if (ctype.getBaseType().equals(ZContentType.MESSAGE_RFC822)) {
            parts.add(pcurrent = new PartInfo(ZMimeMessage.newMessage(session, pcurrent.part)));
            state = ParserState.HEADER_LINESTART;
            return true;
        }

        return false;
    }

    /** Ends parsing of the message and marks all currently-active MIME parts
     *  as ended.  Do <u>not</u> call this method until all message bytes have
     *  been passed through {@link #handleByte(byte)}. */
    ZMimeParser endParse() {
        if (state == ParserState.TERMINATED) {
            // if we've already ended the parse, don't rerun this method
            return this;
        }

        // catch the case of a final MIME boundary without a newline
        processBoundary();

        // line count-wise, a partial line counts as a line
        newline();

        // catch any in-flight message headers without a newline
        if (!header.isEmpty()) {
            if (header.endsWith((byte) '\r')) {
                header.append('\n');
            } else if (!header.endsWith((byte) '\n')) {
                header.append('\r').append('\n');
            }
            saveHeader();
        }
        clearHeader();

        // catch the case where we terminate during a MIME/message header block
        if (currentPart().bodyStart < 0) {
            bodyStart(position);
        }
        if (currentPart().bodyStart < 0) {
            // in some very rare cases, the first bodyStart() creates new PartInfos that themselves must be tweaked
            bodyStart(position);
        }

        // record the end position and length in lines for all open parts
        while (!parts.isEmpty()) {
            endPart(position, parts.size() == 1);
        }

        // and ignore all subsequent calls to this method
        state = ParserState.TERMINATED;

        return this;
    }

    @Override
    public String toString() {
        return state + " @ " + position + ": " + parts;
    }


//    static class ZHeaderParser extends ZMimeParser {
//        private ZInternetHeaders headers;
//
//        ZInternetHeaders getHeaders() {
//            endParse();
//            return headers;
//        }
//
//        @Override
//        boolean handleByte(byte b) {
//            super.handleByte(b);
//            if (state == ParserState.BODY || state == ParserState.BODY_LINESTART) {
//                return false;
//            } else if (state != ParserState.BODY_CR) {
//                return true;
//            } else {
//                return headers == null && lastEnding != LineEnding.CR;
//            }
//        }
//
//        @Override
//        void endParse() {
//            if (headers == null) {
//                // catch any in-flight message headers without a newline
//                saveHeader();
//
//                headers = currentPart().headers;
//            }
//        }
//    }
}
