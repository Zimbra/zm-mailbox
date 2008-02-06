package com.zimbra.cs.mailclient.imap;

import com.zimbra.cs.mailclient.MailException;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * IMAP server capabilities
 */
public class Capabilities {
    private final List<String> mCapabilities = new ArrayList<String>();
    
    public static final String IMAP4REV1 = "IMAP4rev1";
    public static final String STARTTLS = "STARTTLS";
    public static final String LOGINDISABLED = "LOGINDISABLED";
    public static final String IMAP4 = "IMAP4";

    private static final String[] REQUIRED_CAPABILITIES =
        { IMAP4REV1, STARTTLS, LOGINDISABLED };

    public static Capabilities read(ImapParser parser) throws IOException {
        Capabilities caps = new Capabilities();
        for (ImapData atom : parser.readAtoms().getListValue()) {
            caps.addCapability(atom.getStringValue());
        }
        caps.validate();
        return caps;
    }

    public Capabilities() {}

    public void addCapability(String cap) {
        mCapabilities.add(cap);
    }

    public boolean hasCapability(String cap) {
        return contains(mCapabilities, cap);
    }

    public boolean hasAuthMethod(String method) {
        return hasCapability("AUTH=" + method);
    }

    private static boolean contains(List<String> list, String key) {
        for (String s : list) {
            if (s.equalsIgnoreCase(key)) return true;
        }
        return false;
    }

    public String[] getCapabilities() {
        return mCapabilities.toArray(new String[mCapabilities.size()]);
    }

    public void validate() throws MailException {
        for (String cap : REQUIRED_CAPABILITIES) {
            if (!hasCapability(cap)) {
                throw new MailException("Capability '" + cap + "' must be supported");
            }
        }
    }
        
    public String toString() {
        StringBuilder sb = new StringBuilder("CAPABILITIES[");
        for (String cap : mCapabilities) {
            sb.append(' ').append(cap);
        }
        return sb.append(']').toString();
    }
}
