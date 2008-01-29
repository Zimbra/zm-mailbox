package com.zimbra.cs.mailtest;

import java.io.IOException;
import java.io.File;

public class ImapResponse {
    private ImapParser mParser;
    private String mTag;
    private long mNumber = -1;
    private Atom mAtom;
    private ImapData[] mData;
    private ResponseText mResponseText;

    public static final String CONTINUATION = "+";
    public static final String UNTAGGED = "*";

    public static ImapResponse read(ImapParser tokenizer) throws IOException {
        return new ImapResponse(tokenizer).parse();
    }

    private ImapResponse(ImapParser tokenizer) {
        mParser = tokenizer;
    }
    
    private ImapResponse parse() throws IOException {
        switch (mParser.peekChar()) {
        case '*':
            mTag = UNTAGGED;
            mParser.skipChar('*');
            mParser.skipSpace();
            mTag = UNTAGGED;
            readUntagged();
            break;
        case '+':
            mTag = CONTINUATION;
            mParser.skipChar('+');
            mParser.skipSpace();
            mTag = "+";
            mData = new ImapData[] { mParser.readText() };
            break;
        default:
            mTag = mParser.readAtom().getStringValue();
            mParser.skipSpace();
            readTagged();
        }
        mParser.skipCRLF();
        return this;
    }

    private void readUntagged() throws IOException {
        if (Chars.isNumber(mParser.peekChar())) {
            mNumber = mParser.readNumber();
            mParser.skipSpace();
        }
        mAtom = readAtom();
        switch (mAtom) {
        case OK: case BAD: case NO: case BYE:
            mParser.skipSpace();
            mResponseText = ResponseText.read(mParser);
            break;
        case CAPABILITY:
            mParser.skipSpace();
            mData = new ImapData[] { mParser.readAtoms() };
            break;           
        case FLAGS:
            // "FLAGS" SP flag-list
            mParser.skipSpace();
            mData = new ImapData[] { mParser.readList() };
            break;
        case LIST: case LSUB:
            // "LIST" SP mailbox-list / "LSUB" SP mailbox-list
            // mailbox-list    = "(" [mbx-list-flags] ")" SP
            //                   (DQUOTE QUOTED-CHAR DQUOTE / nil) SP mailbox
            mParser.skipSpace();
            mData = new ImapData[3];
            mData[0] = mParser.readList();
            mParser.skipSpace();
            mData[1] = mParser.readAString();
            mParser.skipSpace();
            mData[2] = mParser.readAString();
            break;
        case SEARCH:
            // "SEARCH" *(SP nz-number)
            if (mParser.isSpace()) {
                mParser.skipSpace();
                mData = new ImapData[] { mParser.readAtoms() };
            }
            break;
        case STATUS:
            // "STATUS" SP mailbox SP "(" [status-att-list] ")"
            mData = new ImapData[2];
            mParser.skipSpace();
            mData[0] = mParser.readAString();
            mParser.skipSpace();
            mData[1] = mParser.readList();
            break;
        case FETCH:
            // message-data    = nz-number SP ("EXPUNGE" / ("FETCH" SP msg-att))
            // msg-att         = "(" (msg-att-dynamic / msg-att-static)
            //                    *(SP (msg-att-dynamic / msg-att-static)) ")"
            mParser.skipSpace();
            mData = new ImapData[] { mParser.readList() };
            break;
        case EXISTS: case RECENT: case EXPUNGE:
            break;
        default:
            throw new ParseException("Unknown response command: " + mAtom);
        }
    }

    private Atom readAtom() throws IOException {
        ImapData token = mParser.readAtom();
        Atom atom = token.getAtomValue();
        if (atom == Atom.UNKNOWN) {
            throw new ParseException(
                "Unrecognized response command or status: " + token.getStringValue());
        }
        return atom;
    }
    
    private void readTagged() throws IOException {
        mAtom = readAtom();
        mParser.skipSpace();
        switch (mAtom) {
        case OK: case NO: case BAD:
            mResponseText = ResponseText.read(mParser);
            break;
        default:
            throw new ParseException("Invalid state in tagged response: " + mAtom);
        }
    }

    public boolean isContinuation() {
        return CONTINUATION.equals(mTag);
    }

    public boolean isUntagged() {
        return UNTAGGED.equals(mTag);
    }

    public boolean isTagged() {
        return !isContinuation() && !isUntagged();
    }

    public String getTag() {
        return mTag;
    }

    public long getNumber() {
        return mNumber;
    }

    public Atom getAtom() {
        return mAtom;
    }

    public ImapData[] getData() {
        return mData;
    }

    public ResponseText getResponseText() {
        return mResponseText;
    }

    public String getError() {
        return mResponseText != null ? mResponseText.getText() : null;
    }
    
    public boolean isOK()  { return mAtom == Atom.OK; }
    public boolean isBAD() { return mAtom == Atom.BAD; }
    public boolean isNO()  { return mAtom == Atom.NO; }
    public boolean isBYE() { return mAtom == Atom.BYE; }

    /*
     * Cleanup response data, deleting any temporary files used to store large
     * literal data.
     */
    public void cleanup() {
        if (mData != null) {
            for (ImapData param : mData) {
                cleanup(param);
            }
        } else if (mResponseText != null) {
            cleanup(mResponseText.getData());
        }
    }

    private void cleanup(ImapData data) {
        if (data == null) return;
        if (data.isLiteral()) {
            File f = data.getLiteralValue().getFile();
            if (f != null) f.delete();
        } else if (data.isList()) {
            for (ImapData d : data.getListValue()) {
                cleanup(d);
            }
        }
    }
}
