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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

public interface ZEmailAddress {
    
    public static final String EMAIL_TYPE_BCC = "b";
    public static final String EMAIL_TYPE_CC = "c";
    public static final String EMAIL_TYPE_FROM = "f";
    public static final String EMAIL_TYPE_SENDER = "s";
    public static final String EMAIL_TYPE_TO = "t";
    public static final String EMAIL_TYPE_REPLY_TO = "r";
    
    /**
     * (f)rom, t(o), c(c), (s)ender, (r)eply-to, b(cc). Type is only sent when an individual message message is returned. In the
     * list of conversations, all the email addresseses returned for a conversation are a subset
     * of the participants. In the list of messages in a converstation, the email addressses are
     * the senders. 
     */
    public String getType();

    /**
     * the comment/name part of an address
     */
    public String getPersonal();
    
    /**
     * the user@domain part of an address
     */
    public String getAddress();
    
    /**
     * if we have personal, first word in "word1 word2" format, or last word in "word1, word2" format.
     * if no personal, take string before "@" in email-address.
     */
    public String getDisplay();
    
}
