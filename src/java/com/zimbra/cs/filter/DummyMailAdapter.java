/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Apr 11, 2005
 *
 */
package com.zimbra.cs.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.InternetAddressException;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.Action;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.mail.SieveMailException;

public class DummyMailAdapter implements MailAdapter {

    private List mHeaders = new ArrayList(1);
    private List mActions = new ArrayList(1);

    public void setContext(SieveContext context) {
    }

    public List getActions() {
        return mActions;
    }

    public ListIterator getActionsIterator() {
        return mActions.listIterator();
    }

    public List getHeader(String name) throws SieveMailException {
        return Collections.EMPTY_LIST;
    }

    public List getMatchingHeader(String name) throws SieveMailException {
        return mHeaders;
    }

    public List getHeaderNames() throws SieveMailException {
        return Collections.EMPTY_LIST;
    }

    public void addAction(Action action) {
    }

    public void executeActions() throws SieveException {
    }

    public int getSize() throws SieveMailException {
        return 0;
    }

    public Object getContent() {
        return "";
    }
    
    public String getContentType() {
        return "text/plain";
    }

    public boolean isInBodyText(String phraseCaseInsensitive) throws SieveMailException {
        return false;
    }

    public Address[] parseAddresses(String headerName) {
        return FilterAddress.EMPTY_ADDRESS_ARRAY;
    }
}
