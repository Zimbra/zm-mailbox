/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import com.zimbra.cs.util.ZimbraLog;

final class ImapLiteral {
    private int mOctets;

    private int mLength;

    private boolean mBlocking;

    private ImapLiteral() { }

    /** octets specified to be in the literal, for {12+}, this is 12. */
    public int octets() {
        return mOctets;
    }

    /** length of the specification, for {12+} this is 5. */
    public int length() {
        return mLength;
    }

    /* is this a blocking literal? */
    public boolean blocking() {
        return mBlocking;
    }
    
    public static ImapLiteral parse(String tag, String line) throws ImapParseException {
        ImapLiteral result = new ImapLiteral();
        int digitLimit;
        if (line.endsWith("+}")) {
            digitLimit = line.length() - 2;
            result.mBlocking = false;
        } else if (line.endsWith("}")) {
            digitLimit = line.length() - 1;
            result.mBlocking = true;
        } else {
            /* no literal present */
            return result;
        }
        
        int openBraceIndex = line.lastIndexOf("{");
        if (openBraceIndex < 0) {
            throw new ImapParseException(tag, "malformed literal no open brace found");
        }

        int digitStart = openBraceIndex + 1;
        if ((digitLimit - digitStart) <= 0) {
            throw new ImapParseException(tag, "malformed literal no octet count found");
        }

        try {
            String number = line.substring(digitStart, digitLimit);
            if (ZimbraLog.imap.isDebugEnabled()) ZimbraLog.imap.debug("LITERAL found count string=" + number);
            result.mOctets = Integer.parseInt(number);
            result.mLength = digitLimit - digitStart + (result.mBlocking ? 2 /* {} */ : 3 /* {+} */); 
        } catch (NumberFormatException nfe) {
            if (ZimbraLog.imap.isDebugEnabled()) ZimbraLog.imap.debug("LITERAL exception", nfe);
            throw new ImapParseException(tag, "malformed literal octect count not a number");
        }
        return result;
    }
    
    public static void main(String[] args) throws Exception {
    	ImapLiteral l = parse(args[0], args[1]);
    	System.err.println(l.length());
    }
}
