package com.zimbra.cs.mailclient.imap;

import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Iterator;

/**
 * IMAP data type.
 */
public final class ImapData {
    private final Type mType;
    private final Object mValue;
    
    public static enum Type {
        TEXT, QUOTED, LITERAL, LIST
    }

    public static ImapData text(String s) {
        return new ImapData(Type.TEXT, s);
    }

    public static ImapData quoted(String s) {
        return new ImapData(Type.QUOTED, s);
    }

    public static ImapData literal(Literal l) {
        return new ImapData(Type.LITERAL, l);
    }

    public static ImapData list (List<ImapData> l) {
        return new ImapData(Type.LIST, l);
    }

    private ImapData(Type type, Object value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        mType = type;
        mValue = value;
    }

    public Type getType() {
        return mType;
    }
    
    public Object getValue() {
        return mValue;
    }

    public String getStringValue() {
        return mValue.toString();
    }

    public long getLongValue() {
        return Integer.parseInt(getStringValue());
    }
    
    public Literal getLiteralValue() {
        return (Literal) mValue;
    }

    @SuppressWarnings("unchecked")
    public List<ImapData> getListValue() {
        return (List<ImapData>) mValue;
    }
    
    public boolean isText() {
        return mType == Type.TEXT;
    }

    public boolean isQuoted() {
        return mType == Type.QUOTED;
    }

    public boolean isLiteral() {
        return mType == Type.LITERAL;
    }

    public boolean isList() {
        return mType == Type.LIST;
    }
    
    public boolean isString() {
        return isQuoted() || isLiteral();
    }

    public boolean isAtom() {
        return isText() && Chars.isAtom(getStringValue());
    }

    public boolean isAString() {
        return isAtom() || isString();
    }

    public boolean isNString() {
        return isNil() || isString();
    }
    
    public boolean isNil() {
        return isAtom("NIL");
    }

    public boolean isNumber() {
        return isText() && Chars.isNumber(getStringValue());
    }

    public Atom getAtomValue() {
        return isText() ? Atom.get(getStringValue()) : null;
    }

    public boolean isAtom(Atom atom) {
        return getAtomValue() == atom;
    }

    public boolean isAtom(String name) {
        return isText() && getStringValue().equalsIgnoreCase(name);
    }

    public void write(OutputStream os) throws IOException {
        write(os, false);
    }

    public void write(OutputStream os, boolean summary) throws IOException {
        switch (mType) {
        case TEXT:
            Chars.write(os, getStringValue());
            break;
        case QUOTED:
            writeQuoted(os, getStringValue());
            break;
        case LITERAL:
            if (summary) {
                Chars.write(os, "LITERAL{" + getLiteralValue().getSize() + "}");
            } else {
                getLiteralValue().write(os);
            }
            break;
        case LIST:
            os.write('(');
            Iterator<ImapData> it = getListValue().iterator();
            it.next().write(os, summary);
            while (it.hasNext()) {
                os.write(' ');
                it.next().write(os, summary);
            }
            os.write(')');
        }
    }

    private static void writeQuoted(OutputStream os, String s)
            throws IOException {
        os.write('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '\\': case '"':
                os.write('\\');
            default:
                os.write(c);
            }
        }
        os.write('"');
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ImapData data = (ImapData) obj;
        return mType == data.mType && mValue.equals(data.mValue);
    }

    public int hashCode() {
        return mType.hashCode() ^ mValue.hashCode();
    }

    public String toString() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            write(os, true);
        } catch (IOException e) {
            throw new AssertionError();
        }
        return Chars.toString(os.toByteArray());
    }
}
