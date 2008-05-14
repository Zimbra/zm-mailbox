package com.zimbra.cs.mailclient.imap;

import com.zimbra.cs.mailclient.ParseException;
import com.zimbra.cs.mailclient.util.Ascii;

import java.nio.ByteBuffer;
import java.io.IOException;

public final class MailboxName {
    private String name; // Decoded name

    public static final MailboxName INBOX = new MailboxName("INBOX");

    private static final char ENCODE_PEM[] = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
        'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
        'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
        'w', 'x', 'y', 'z', '0', '1', '2', '3',
        '4', '5', '6', '7', '8', '9', '+', ','
    };

    private static final byte DECODE_PEM[] = new byte[256];
    static {
        for (int i = 0; i < 256; i++) {
            DECODE_PEM[i] = -1;
        }
        for (int i = 0; i < ENCODE_PEM.length; i++) {
            DECODE_PEM[ENCODE_PEM[i]] = (byte) i;
        }
    }

    public static MailboxName decode(ImapData encoded) throws IOException {
        return decode(ByteBuffer.wrap(encoded.getBytes()));
    }

    public static MailboxName decode(String encoded) throws IOException {
        return decode(ByteBuffer.wrap(Ascii.getBytes(encoded)));
    }

    public static MailboxName decode(ByteBuffer bb) throws IOException {
        return new MailboxName(decodeBytes(bb));
    }
    
    public MailboxName(String name) {
        this.name = name;
    }

    private static String decodeBytes(ByteBuffer bb) throws ParseException {
        StringBuffer sb = new StringBuffer(bb.remaining());
        while (bb.hasRemaining()) {
            int c = bb.get();
            if (c < 0) {
                throw new ParseException("Not a 7-bit character: " + c);
            }
            if (c == '&') {
                decodePEM(bb, sb);
            } else {
                sb.append((char) c);
            }
        }
        return sb.toString();
    }

    private static void decodePEM(ByteBuffer bb, StringBuffer sb)
        throws ParseException {
        try {
            int c = bb.get();
            if (c == '-') {
                sb.append('&');
                return;
            }
            int bits = 0;
            int count = 0;
            do {
                byte b = DECODE_PEM[c & 0xff];
                if (b == -1) {
                    throw new ParseException("Invalid Base64 character: " + c);
                }
                bits = bits << 6 | b;
                count += 6;
                if (count > 15) {
                    count -= 16; // bits remaining
                    sb.append((char) (bits >> count));
                    bits &= (count * 2 - 1);
                }
            } while ((c = bb.get()) != '-');
            if (count > 0) {
                sb.append((char) (bits & 0xffff));
            }
        } catch (IndexOutOfBoundsException e) {
            throw new ParseException("Unterminated Base64 encoding");
        }
    }

    public String encode() {
        StringBuilder sb = new StringBuilder(name.length());
        int index = 0;
        while (index < name.length()) {
            char c = name.charAt(index);
            if (c >= 0x20 && c <= 0x7e) {
                if (c == '&') {
                    sb.append('&').append('-');
                } else {
                    sb.append(c);
                }
                index++;
            } else {
                index = encodePEM(sb, index);
            }
        }
        return sb.toString();
    }

    private int encodePEM(StringBuilder sb, int index) {
        sb.append('&');
        int bits = 0;
        int count = 0;
        while (index < name.length()) {
            char c = name.charAt(index);
            if (c >= 0x20 && c <= 0x7e) break;
            index++;
            bits = bits << 16 | c;
            count += 2;
            System.out.println("Count = " + count);
            if (count == 4) {
                encodePEM(sb, bits >>> 8, 3);
                bits &= 0xff;
                count = 1;
            } else if (count == 3) {
                encodePEM(sb, bits, count);
                bits = 0;
                count = 0;
            }
        }
        if (count > 0) {
            encodePEM(sb, bits, count);
        }
        sb.append('-');
        return index;
    }
    
    private static void encodePEM(StringBuilder sb, int bits, int count) {
        System.out.printf("bits = %x\n", bits);
        switch (count) {
        case 3:
            sb.append(encodePEM(bits, 18));
            sb.append(encodePEM(bits, 12));
            sb.append(encodePEM(bits, 6));
            sb.append(encodePEM(bits, 0));
            break;
        case 2:
            bits <<= 2;
            sb.append(encodePEM(bits, 12));
            sb.append(encodePEM(bits, 6));
            sb.append(encodePEM(bits, 0));
            break;
        case 1:
            bits <<= 4;
            sb.append(encodePEM(bits, 6));
            sb.append(encodePEM(bits, 0));
        }
    }

    private static char encodePEM(int bits, int shift) {
        return ENCODE_PEM[(bits >> shift) & 0x3f];
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MailboxName &&
               name.equals(((MailboxName) obj).name);
    }

    @Override
    public String toString() {
        return name;
    }
}
