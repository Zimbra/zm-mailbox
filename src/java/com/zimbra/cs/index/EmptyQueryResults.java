/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Oct 22, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.index;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author tim
 *
 */
class EmptyQueryResults extends ZimbraQueryResultsImpl {

    
    EmptyQueryResults(byte[] types, SortBy searchOrder, Mailbox.SearchResultMode mode) {
        super(types, searchOrder, mode);
    }
    
    public void resetIterator()  {
    }

    public ZimbraHit getNext() {
        return null;
    }

    public ZimbraHit peekNext() {
        return null;
    }
    
    public void doneWithSearchResults() {
    }

    public ZimbraHit skipToHit(int hitNo) {
        return null;
    }
    
    public List<QueryInfo> getResultInfo() { return new ArrayList<QueryInfo>(); }
    
    public int estimateResultSize() throws ServiceException { return 0; }
    
}
