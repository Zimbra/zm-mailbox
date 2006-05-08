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
package com.zimbra.cs.service.wiki;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.ServiceException.Argument;

public class WikiServiceException extends ServiceException {
	
	public static final String NO_SUCH_WIKI = "wiki.NO_SUCH_WIKI";
	public static final String NOT_WIKI_ITEM = "wiki.NOT_WIKI_ITEM";
	public static final String CANNOT_READ = "wiki.CANNOT_READ";
	
	public static final String WIKI_ID = "w";
	
    private WikiServiceException(String message, String code, boolean isReceiversFault, Argument... args) {
        super(message, code, isReceiversFault, args);
    }
    public static WikiServiceException NO_SUCH_WIKI(String w) {
        return new WikiServiceException("no such wiki: "+ w, NO_SUCH_WIKI, SENDERS_FAULT, new Argument(WIKI_ID, w));
    }
    public static WikiServiceException NOT_WIKI_ITEM(String w) {
        return new WikiServiceException("not WikiItem: "+ w, NO_SUCH_WIKI, SENDERS_FAULT, new Argument(WIKI_ID, w));
    }
    public static WikiServiceException CANNOT_READ(String w) {
        return new WikiServiceException("cannot read wiki message body: "+ w, CANNOT_READ, RECEIVERS_FAULT, new Argument(WIKI_ID, w));
    }
}
