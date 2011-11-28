package com.zimbra.qa.unittest.ldap;

import com.zimbra.cs.account.IDNUtil;

public class Names {
    
    public static class IDNName {
        String mUincodeName;
        String mAsciiName;
        
        public IDNName(String uName) {
            mUincodeName = uName;
            
            String[] parts = uName.split("@");
            if (parts.length == 2)
                mAsciiName = parts[0] + "@" + IDNUtil.toAsciiDomainName(parts[1]);
            else
                mAsciiName = IDNUtil.toAsciiDomainName(uName);
        }
        
        public IDNName(String localPart, String uName) {
            mUincodeName = localPart + "@" + uName;
            mAsciiName = localPart + "@" + IDNUtil.toAsciiDomainName(uName);
        }
        
        public String uName() { return mUincodeName; } 
        public String aName() { return mAsciiName; }
    }

    /**
     * Given a name (which is to be turn into a DN), mix in chars 
     * defined in rfc2253.txt that need to be escaped in RDN value.
     * 
     * http://www.ietf.org/rfc/rfc2253.txt?number=2253
     * 
     * - a space or "#" character occurring at the beginning of the
     *   string
     *
     * - a space character occurring at the end of the string
     *
     * - one of the characters ",", "+", """, "\", "<", ">" or ";"
     * 
     * Implementations MAY escape other characters.
     *
     * If a character to be escaped is one of the list shown above, then it
     * is prefixed by a backslash ('\' ASCII 92).
     *
     * Otherwise the character to be escaped is replaced by a backslash and
     * two hex digits, which form a single byte in the code of the
     * character.
     * 
     * @param name
     * @return
     */    
    private static String makeRFC2253Name(String name, boolean wantTrailingBlank) {
        String LEADING_CHARS = "#";
        String TRAILING_CHARS = " ";
        String BACKSLASH_ESCAPED_CHARS = "# ,+\"\\<>;";
        String UNICODE_CHARS = "\u4e2d\u6587";
        
        if (wantTrailingBlank) {
            return LEADING_CHARS + BACKSLASH_ESCAPED_CHARS + DOT_ATOM_CHARS + UNICODE_CHARS + "---" + name + TRAILING_CHARS;
        } else {
            return LEADING_CHARS + BACKSLASH_ESCAPED_CHARS + DOT_ATOM_CHARS + UNICODE_CHARS + "---" + name;
        }
    }
    
    // RFC 2822
    private static final String ATOM_CHARS = "!#$%&'*+-/=?^_`{|}~";   
    private static final String DOT_ATOM_CHARS = "." + ATOM_CHARS;
    
    private static String makeRFC2253NameEmailLocalPart(String name) {
        String LEADING_CHAR = "#";
        return LEADING_CHAR + DOT_ATOM_CHARS + "---" + name;
    }
    
    private static String makeRFC2253NameDomainName(String name) {
        String UNICODE_CHARS = "\u4e2d\u6587";
        
        // hmm, javamail does not like any of the ATOM_CHARS 
        return /* ATOM_CHARS + */ UNICODE_CHARS + "---" + name;
    }

    static String makeAccountNameLocalPart(String localPart) {
        return makeRFC2253NameEmailLocalPart(localPart);
    }

    static String makeAliasNameLocalPart(String localPart) {
        return makeRFC2253NameEmailLocalPart(localPart);
    }

    static String makeCosName(String name) {
        return makeRFC2253Name(name, false);
    }

    static String makeDataSourceName(String name) {
        // historically we allow trailing blank in data source name
        // should probably make it consistent across the board.
        return makeRFC2253Name(name, true);
    }
    
    static String makeDLNameLocalPart(String localPart) {
        return makeRFC2253NameEmailLocalPart(localPart);
    }
    
    static String makeDomainName(String name) {
        return makeRFC2253NameDomainName(name);
    }
    
    static String makeIdentityName(String name) {
        // historically we allow trailing blank in identity name
        // should probably make it consistent across the board.
        return makeRFC2253Name(name, true);
    }
    
    static String makeServerName(String name) {
        return makeRFC2253Name(name, false);
    }
    
    static String makeSignatureName(String name) {
        return makeRFC2253Name(name, false);
    }
    
    static String makeXMPPName(String name) {
        return makeRFC2253Name(name, false);
    }
    
    static String makeZimletName(String name) {
        return makeRFC2253Name(name, false);
    }
}
