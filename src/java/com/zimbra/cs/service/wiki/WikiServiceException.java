/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.wiki;

import com.zimbra.common.service.ServiceException;

@SuppressWarnings("serial")
public class WikiServiceException extends ServiceException {
	
	public static final String NO_SUCH_WIKI = "wiki.NO_SUCH_WIKI";
	public static final String NOT_WIKI_ITEM = "wiki.NOT_WIKI_ITEM";
	public static final String CANNOT_READ = "wiki.CANNOT_READ";
	public static final String INVALID_PATH = "wiki.INVALID_PATH";
	public static final String ERROR = "wiki.ERROR";
	
	public static final String WIKI_ID = "w";
	
    private WikiServiceException(String message, String code, boolean isReceiversFault, Argument... args) {
        super(message, code, isReceiversFault, args);
    }
    private WikiServiceException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message, code, isReceiversFault, cause);
    }
    public static class NoSuchWikiException extends WikiServiceException {
    	public NoSuchWikiException(String w) {
    		super("no such wiki: "+ w, NO_SUCH_WIKI, SENDERS_FAULT, new Argument(WIKI_ID, w, Argument.Type.STR));
    	}
    }
    public static WikiServiceException NOT_WIKI_ITEM(String w) {
        return new WikiServiceException("not WikiItem: "+ w, NO_SUCH_WIKI, SENDERS_FAULT, new Argument(WIKI_ID, w, Argument.Type.STR));
    }
    public static WikiServiceException CANNOT_READ(String w) {
        return new WikiServiceException("cannot read wiki message body: "+ w, CANNOT_READ, RECEIVERS_FAULT, new Argument(WIKI_ID, w, Argument.Type.STR));
    }
    public static WikiServiceException ERROR(String w) {
        return new WikiServiceException("error: "+ w, ERROR, SENDERS_FAULT);
    }
    public static WikiServiceException ERROR(String w, Throwable cause) {
        return new WikiServiceException("error: "+ w, ERROR, SENDERS_FAULT, cause);
    }
    public static WikiServiceException INVALID_PATH(String path) {
        return new WikiServiceException("invalid path: "+ path, ERROR, SENDERS_FAULT);
    }
    public static WikiServiceException NOT_ENABLED() {
        return new WikiServiceException("notebook is not enabled", ERROR, RECEIVERS_FAULT);
    }
    public static WikiServiceException BRIEFCASES_NOT_ENABLED() {
        return new WikiServiceException("briefcases is not enabled", ERROR, RECEIVERS_FAULT);
    }
}
