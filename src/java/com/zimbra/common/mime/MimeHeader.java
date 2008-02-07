/**
 * 
 */
package com.zimbra.common.mime;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

class MimeHeader {
    protected final String mName;
    protected final byte[] mContent;
    protected final int mValueStart;

    /** Constructor for pre-analyzed header line read from message source.
     * @param name    Header field name.
     * @param content Complete raw header line, with folding and trailing CRLF
     *                and 2047-encoding intact.
     * @param start   The position within <code>content</code> where the header
     *                field value begins (after the ":"/": "). */
    MimeHeader(String name, byte[] content, int start) {
        mName = name;  mContent = content;  mValueStart = start;
    }

    /** Constructor for new header lines.  Header will be serialized as
     *  <tt>{name}: {value}CRLF</tt>.  <i>Note: No folding or 2047-encoding
     *  is done at present.</i> */
    MimeHeader(String name, String value) {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        byte[] bname = name.getBytes(), bvalue = (value == null ? "" : value).getBytes();
        content.write(bname, 0, bname.length);  content.write(':');  content.write(' ');
        int start = content.size();
        content.write(bvalue, 0, bvalue.length);  content.write('\r');  content.write('\n');

        mName = name;  mContent = content.toByteArray();  mValueStart = start;
    }

    MimeHeader(String name, MimeCompoundHeader mch) {
        mName = name;  mContent = (mch.toString(name) + "\r\n").getBytes();  mValueStart = mName.length() + 2;
    }

    String getName()       { return mName; }
    byte[] getRawHeader()  { return mContent; }
    String getValue()      { return unfold(new String(mContent, mValueStart, mContent.length - mValueStart)); }
    String getValue(String charset) {
        if (charset == null || charset.equals(""))
            return getValue();
        try {
            return unfold(new String(mContent, mValueStart, mContent.length - mValueStart, charset));
        } catch (UnsupportedEncodingException e) {
            return getValue();
        }
    }
    private String unfold(final String folded) {
        int length = folded.length();
        StringBuilder unfolded = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = folded.charAt(i);
            if (c != '\r' && c != '\n')
                unfolded.append(c);
        }
        return unfolded.toString();
    }

    @Override public String toString()  { return new String(mContent); }


    static class MimeAddressHeader extends MimeHeader {
        private List<InternetAddress> mAddresses = new ArrayList<InternetAddress>(3);

        MimeAddressHeader(final String name, final String value) {
            super(name, value);
            if (mValueStart > 0)
                parse(mContent, mValueStart, mContent.length - mValueStart);
        }

        private void parse(final byte[] content, final int start, final int length) {
            boolean quoted = false, escaped = false, group = false;
            int pos = start, astart = pos, end = start + length, clevel = 0;

            while (pos < end) {
                byte c = content[pos++];
                if (c == '\r' || c == '\n') {
                    // ignore folding, even where it's not actually permitted
                    escaped = false;
                } else if (quoted) {
                    quoted = !escaped && c == '"';
                    escaped = !escaped && c == '\\';
                } else if (c == '(' || clevel > 0) {
                    // handle comments outside of quoted strings, even where they're not actually permitted
                    if (!escaped && (c == '(' || c == ')'))
                        clevel += c == '(' ? 1 : -1;
                    escaped = !escaped && c == '\\';
                } else if (c == '"') {
                    quoted = true;
                } else if (c == ',' || (c == ';' && group)) {
                    // this concludes the address portion of our program
                    mAddresses.add(new InternetAddress(content, astart, pos - astart));
                    group = c == ';';
                    astart = pos;
                } else if (c == ':' && !group) {
                    // ignore the group name that we've just passed
                    group = true;
                    astart = pos;
                }
            }
        }

        MimeAddressHeader addAddress(InternetAddress iaddr) {
            mAddresses.add(iaddr);
//            reserialize();
            return this;
        }
    }
}