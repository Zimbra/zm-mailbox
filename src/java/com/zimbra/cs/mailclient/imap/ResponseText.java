package com.zimbra.cs.mailclient.imap;

import java.io.IOException;

/**
 * resp-text       = ["[" resp-text-code "]" SP] text
 *
 * resp-text-code  = "ALERT" /
 *                   "BADCHARSET" [SP "(" astring *(SP astring) ")" ] /
 *                   capability-data / "PARSE" /
 *                   "PERMANENTFLAGS" SP "("
 *                   [flag-perm *(SP flag-perm)] ")" /
 *                   "READ-ONLY" / "READ-WRITE" / "TRYCREATE" /
 *                   "UIDNEXT" SP nz-number / "UIDVALIDITY" SP nz-number /
 *                   "UNSEEN" SP nz-number /
 *                   atom [SP 1*<any TEXT-CHAR except "]">]
 */
public class ResponseText {
    private ImapData mCode;
    private ImapData mData;
    private String mText;

    public static ResponseText read(ImapParser parser) throws IOException {
        ResponseText rt = new ResponseText();
        if (parser.peek() == '[') {
            rt.parseCode(parser);
        }
        rt.mText = parser.readText().getStringValue();
        return rt;
    }

    private void parseCode(ImapParser parser) throws IOException {
        parser.skipChar('[');
        mCode = parser.readAtom();
        switch (mCode.getAtomValue()) {
        case ALERT: case PARSE: case READ_ONLY: case READ_WRITE: case TRYCREATE:
            break;
        case UIDNEXT: case UIDVALIDITY: case UNSEEN:
            parser.skipSpace();
            mData = parser.readAtom(); // Validate
            break;
        case BADCHARSET:
            if (parser.isSpace()) {
                parser.skipSpace();
                mData = parser.readList();
            }
            break;
        case PERMANENTFLAGS:
            parser.skipSpace();
            mData = parser.readList();
            break;
        case CAPABILITY:
            parser.skipSpace();
            mData = parser.readAtoms();
            break;
        default:
            if (parser.isSpace()) {
                parser.skipSpace();
                mData = parser.readText(']');
            }
        }
        parser.skipChar(']');
    }

    public ImapData getCode() {
        return mCode;
    }

    public ImapData getData() {
        return mData;
    }

    public String getText() {
        return mText;
    }
}
