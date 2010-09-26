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
package com.zimbra.common.mime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

class MimeParser {
    MimeParser(Properties props) {
        properties = props;
        parts.add(new PartInfo(0, true));
    }

    MimeParser(MimeMessage target) {
        this(target.getProperties());
        parts.add(0, new PartInfo(target, 0, 0, PartInfo.Location.CONTENT));
    }

    private enum ParserState {
        HEADER_LINESTART, HEADER_NAME, HEADER_VALUE, HEADER_CR,
        BODY_LINESTART, BODY, BODY_CR
    }

    private enum LineEnding { CR, LF, CRLF }

    private static class BoundaryChecker {
        /** A <code>Map</code> mapping active boundary strings which have
         *  matched all checked bytes in the line thus far to the number
         *  of trailing dashes after the end of the boundary.  <tt>null</tt>
         *  values mean that we have not yet checked the last byte in the
         *  boundary; <tt>0</tt> means that the boundary matched completely
         *  without trailing dashes, <tt>2</tt> means that the boundary has
         *  matched and there are 2 trailing dashes (i.e. an end boundary). */
        private Map<String, Integer> boundaryCandidates;
        /** The byte position of the end of the current part if this line
         *  matches an active boundary. */
        private long partEnd;
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
         *  <i>Should really switch to having <code>index</code> be an
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
         * @return a <code>Map.Entry</code> whose <tt>key</tt> is the boundary
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
    }

    private static class PartInfo {
        enum Location { PREAMBLE, CONTENT, EPILOGUE }

        MimePart part;
        int firstLine;
        long partStart;
        String boundary;  // value: part boundary | "": unspecified boundary | null: not a multipart
        Location location;
        MimeHeaderBlock headers;
        ContentType ctype;

        PartInfo(long pos, boolean isMessage) {
            partStart = pos;
            location = Location.CONTENT;
            headers = new MimeHeaderBlock(isMessage);
        }

        PartInfo(MimePart mp, int line, long pos, Location loc) {
            part = mp;
            firstLine = line;
            partStart = pos;
            location = loc;
            ctype = mp.getContentType();
        }

        @Override public String toString() {
            return ctype == null ? null : ctype.getContentType();
        }
    }

    /** The current state of the parser.  Generally, a combination of which
     *  type of parsing is going on (HEADER vs. BODY) and where in the line
     *  we are (LINESTART, after CR, etc.). */
    protected ParserState state = ParserState.HEADER_LINESTART;
    /** The <code>MimeMessage</code> resulting from a full message parse.
     *  It is populated by either a call to {@link #getMessage()} or a call to
     *  {@link #endParse()}. */
    private MimeMessage mm;
    /** The <code>Properties</code> object used in the <code>MimeMessage</code>
     *  constructor.  It can define a default charset for parsing unencoded
     *  high-bit-set bytes in message headers. */
    private Properties properties;
    /** The stack of active message parts, outermost to innermost.  The
     *  outermost (index <tt>0</tt>) is always the <code>MimeMessage</code>
     *  which will be returned by the call to {@link #getMessage()}. */
    private List<PartInfo> parts = new ArrayList<PartInfo>(5);

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

    /** The index of the first '<tt>:</tt>' character in the current header,
     *  or <tt>-1</tt> if the first colon has not yet been read. */
    private int colon = -1;
    /** The "name" (the part preceding the colon) of the current header. */
    private StringBuilder name = new StringBuilder(25);
    /** The entire content of the current header.  This includes the name,
     *  the colon, the raw header value, any folding, and the trailing CRLF. */
    private HeaderUtils.ByteBuilder content = new HeaderUtils.ByteBuilder(80);


    /** Terminates message parsing and returns the <code>MimeMessage</code>
     *  resulting from the parse.  <b>Do not call this method until the entire
     *  message has been passed through the parser</b>, otherwise incorrect
     *  lengths may be recorded. */
    MimeMessage getMessage() {
        endParse();
        return mm;
    }

    /** Returns the structure representing the "currently active" part being
     *  handled by the parser. */
    protected PartInfo currentPart() {
        return parts.get(parts.size() - 1);
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
                    content.append(b);
                    lastEnding = LineEnding.CRLF;
                    break;
                }
                // \r without \n -- treat this as the first character of the next line
                lastEnding = LineEnding.CR;
                content.append('\n');
                //$FALL-THROUGH$

            // at the beginning of a header line, after a CR/LF/CRLF
            case HEADER_LINESTART:
                if (processBoundary()) {
                    // found a part boundary, which may transition us to body parsing
                    return handleByte(b);
                }
                newline();
                if (colon != -1 && (b == ' ' || b == '\t')) {
                    // folded line; header value continues
                    content.append(b);
                    state = ParserState.HEADER_VALUE;
                    break;
                }
                // not a folded header line, so record the header and continue
                saveHeader();
                if (b == '\n') {
                    // a blank line terminates the header
                    lastEnding = LineEnding.LF;
                    state = ParserState.BODY_LINESTART;
                    break;
                } else if (b == '\r') {
                    // a blank line terminates the header
                    state = ParserState.BODY_CR;
                    break;
                } else {
                    state = ParserState.HEADER_NAME;
                }
                //$FALL-THROUGH$

            // in a header line, before reaching the colon
            case HEADER_NAME:
                content.append(b);
                if (b == ':') {
                    // found the colon; that concludes the header name
                    colon = (int) (position - lineStart);
                    state = ParserState.HEADER_VALUE;
                } else if (b == '\n') {
                    // header lines not involving a colon are ignored
                    clearHeader();
                    lastEnding = LineEnding.LF;
                    state = ParserState.HEADER_LINESTART;
                } else if (b == '\r') {
                    // header lines not involving a colon are ignored
                    clearHeader();
                    state = ParserState.HEADER_CR;
                } else {
                    name.append((char) b);
                }
                break;

            // in a header line, after reaching the colon
            case HEADER_VALUE:
                content.append(b);
                if (b == '\n') {
                    lastEnding = LineEnding.LF;
                    content.pop().append('\r').append('\n');
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
                if (currentPart().part == null) {
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
        }

        // if there are active MIME boundaries, check whether this line may be one
        if (boundaries != null) {
            checkBoundary(b);
        }

        position++;

        return true;
    }

    /** Registers a newline by updating the parser's line-oriented counters. */
    private void newline() {
        if (lineStart != position) {
            lineNumber++;  lineStart = position;  dashes = 0;  boundaryChecker = null;
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
                bnd = match.getKey().isEmpty() ? boundaryChecker.getSavedBoundary() : match.getKey();
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
        if (pcurrent.part == null) {
            // case where the boundary came in the middle of the headers (?!)
            bodyStart(lineStart);
        }
        while (!bnd.equals(pcurrent.boundary) && parts.size() > 1) {
            if ("".equals(pcurrent.boundary) && (boundaries == null || !boundaries.contains(bnd))) {
                // "" meant that the multipart Content-Type didn't specify a boundary
                //   so pick the next new boundary we see and *that* is the boundary
                ((MimeMultipart) pcurrent.part).setEffectiveBoundary(bnd);
                pcurrent.boundary = ((MimeMultipart) pcurrent.part).getBoundary();
                break;
            }
            endPart(pcurrent, partEnd);
            parts.remove(parts.size() - 1);
            pcurrent = currentPart();
        }
        MimeMultipart multi = (MimeMultipart) pcurrent.part;

        // set up the new part
        if (isEnd) {
            // now that we've hit the end boundary, this boundary is no longer active 
            pcurrent.boundary = null;
            // create the epilogue, which encloses the remaining lines in its *body*
            MimeBodyPart epilogue = new MimeBodyPart(new ContentType(ContentType.TEXT_PLAIN), multi, position, position, null);
            parts.add(pcurrent = new PartInfo(epilogue, lineNumber, position, PartInfo.Location.EPILOGUE));
            state = ParserState.BODY_LINESTART;
        } else {
            // new proper subpart of the multipart -- starting with its MIME headers
            parts.add(pcurrent = new PartInfo(position, false));
            state = ParserState.HEADER_LINESTART;
        }

        recalculateBoundaries();
    }

    /** Regenerates the list of valid MIME boundaries from the set of active
     *  enclosing parts.  Sets {@link #boundaries} appropriately, or to
     *  <tt>null</tt> if there are no valid boundaries. */
    private void recalculateBoundaries() {
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
     *  the part being ended was a multipart preamble or epilogue and was of
     *  nonzero length, associates the part with its parent appropriately. */
    private void endPart(PartInfo pinfo, long end) {
        MimePart mp = pinfo.part;
        long partEnd = Math.max(mp.getBodyOffset(), end);
        mp.recordEndpoint(partEnd, lineNumber - pinfo.firstLine);

        if (mp.getSize() > 0) {
            if (pinfo.location == PartInfo.Location.PREAMBLE) {
                ((MimeMultipart) mp.getParent()).setPreamble((MimeBodyPart) mp);
            } else if (pinfo.location == PartInfo.Location.EPILOGUE) {
                ((MimeMultipart) mp.getParent()).setEpilogue((MimeBodyPart) mp);
            }
        }
    }

    /** Resets all header-related members after processing a header line. */
    private void clearHeader() {
        name.setLength(0);  content.reset();  colon = -1;
    }

    /** Adds the current header to the active part's header block.  If the
     *  header is "<tt>Content-Type</tt>" and it's a <tt>multipart/*</tt>,
     *  updates the set of active MIME boundaries. */
    protected void saveHeader() {
        String key = name.toString().trim();
        if (colon != -1 && !key.equals("")) {
            // the actual content of the header starts after an optional space and/or CRLF
            int valueStart = colon + 1;
            for (int wsp = 0, headerLength = content.size(); valueStart < headerLength; valueStart++) {
                byte b = content.byteAt(valueStart);
                if (b != '\n' && b != '\r' && ((b != ' ' && b != '\t') || ++wsp >= 2))
                    break;
            }

            PartInfo pcurrent = currentPart();
            MimeHeader header = new MimeHeader(key, content.toByteArray(), valueStart);
            pcurrent.headers.addHeader(key, header);

            if (key.equalsIgnoreCase("Content-Type")) {
                pcurrent.ctype = new ContentType(header, defaultContentType());
                if (pcurrent.ctype.getPrimaryType().equals("multipart")) {
                    String bnd = pcurrent.ctype.getParameter("boundary");
                    pcurrent.boundary = bnd == null || bnd.trim().isEmpty() ? "" : bnd;
                    recalculateBoundaries();
                }
            }
        }

        clearHeader();
    }

    private String defaultContentType() {
        MimePart parent = parts.size() <= 1 ? null : parts.get(parts.size() - 2).part;
        boolean inDigest = parent != null && parent.getContentType().getContentType().equals("multipart/digest");
        return inDigest ? ContentType.MESSAGE_RFC822 : ContentType.TEXT_PLAIN;
    }

    /** Marks the transition from parsing MIME/message headers to skimming the
     *  part body.  Creates the corresponding <code>MimePart</code> object for
     *  the current active part and clears the <code>MimeHeaderBlock</code>
     *  that was accumulating part headers.  
     * @param pos  The byte offset of the beginning of the part body.
     * @return Whether the part was a <tt>message/rfc822</tt>, which requires
     *         a parser state transition back to header reading. */
    private boolean bodyStart(long pos) {
        MimePart parent = parts.size() <= 1 ? null : parts.get(parts.size() - 2).part;
        PartInfo pcurrent = currentPart();

        ContentType ctype = pcurrent.ctype;
        if (ctype == null) {
            // if there was no Content-Type header, use the default
            //   (e.g. text/plain unless we're in a multipart/digest)
            pcurrent.ctype = ctype = new ContentType(defaultContentType());
        }

        // we're now far enough along to call the MimePart constructor and
        //   to transition from header mode to body mode
        MimePart mp;
        if (ctype.getContentType().equals(ContentType.MESSAGE_RFC822)) {
            mp = new MimeMessage(ctype, parent, pcurrent.partStart, pos, pcurrent.headers);
        } else if (ctype.getPrimaryType().equals("multipart")) {
            mp = new MimeMultipart(ctype, parent, pcurrent.partStart, pos, pcurrent.headers);
            // set the boundary to the MimeMultipart's official value
            pcurrent.boundary = ((MimeMultipart) mp).getBoundary();
        } else {
            mp = new MimeBodyPart(ctype, parent, pcurrent.partStart, pos, pcurrent.headers);
        }
        pcurrent.part = mp;
        pcurrent.headers = null;
        pcurrent.firstLine = lineNumber;

        // associate the new part with its parent
        if (parent == null) {
            // now that we have the top-level message body, we can create the enclosing message
            parts.add(0, new PartInfo(new MimeMessage(mp, properties), 0, 0, PartInfo.Location.CONTENT));
        } else {
            if (parent instanceof MimeMessage) {
                ((MimeMessage) parent).setBodyPart(mp);
            } else if (parent instanceof MimeMultipart) {
                ((MimeMultipart) parent).addPart(mp);
            }
        }

        if (mp instanceof MimeMultipart) {
            // create the preamble, which encloses the lines up to the first boundary in its *body*
            MimeBodyPart preamble = new MimeBodyPart(new ContentType(ContentType.TEXT_PLAIN), mp, pos, pos, null);
            parts.add(new PartInfo(preamble, lineNumber, pos, PartInfo.Location.PREAMBLE));
        } else if (mp instanceof MimeMessage) {
            // message/rfc822 attachments are just wrappers around their topmost subpart
            parts.add(new PartInfo(pos, true));
            state = ParserState.HEADER_LINESTART;
            return true;
        }

        return false;
    }

    /** Ends parsing of the message and marks all currently-active MIME parts
     *  as ended.  Do <u>not</u> call this method until all message bytes have
     *  been passed through {@link #handleByte(byte)}. */
    void endParse() {
        if (mm != null) {
            return;
        }

        // catch the case of a final MIME boundary without a newline
        processBoundary();

        // line count-wise, a partial line counts as a line
        if (position > lineStart) {
            newline();
        }

        // catch any in-flight message headers without a newline
        if (colon > 0) {
            content.append('\r').append('\n');
            saveHeader();
        }
        clearHeader();

        // catch the case where we terminate during a MIME/message header block
        if (currentPart().part == null) {
            bodyStart(position);
        }

        // record the end position and length in lines for all open parts
        for (PartInfo pinfo : parts) {
            endPart(pinfo, position);
        }

        // the MimeMessage resulting from the parse is the outermost part
        mm = (MimeMessage) parts.get(0).part;
    }


    static class HeaderParser extends MimeParser {
        private MimeHeaderBlock headers;

        HeaderParser() {
            super((Properties) null);
        }

        MimeHeaderBlock getHeaders() {
            endParse();
            return headers;
        }

        @Override boolean handleByte(byte b) {
            super.handleByte(b);
            if (state == ParserState.BODY || state == ParserState.BODY_LINESTART) {
                return false;
            } else if (state != ParserState.BODY_CR) {
                return true;
            } else {
                return headers == null && lastEnding != LineEnding.CR;
            }
        }

        @Override void endParse() {
            if (headers == null) {
                // catch any in-flight message headers without a newline
                saveHeader();

                headers = currentPart().headers;
            }
        }
    }


    public static void main(String... args) throws java.io.IOException {
        java.io.File msgdir = new java.io.File("/Users/dkarp/Documents/messages/unused");
        for (java.io.File file : msgdir.listFiles()) {
            System.out.println("+++ processing message: " + file);
            MimeMessage.dumpParts(new MimeMessage(file));
        }
    }
}
