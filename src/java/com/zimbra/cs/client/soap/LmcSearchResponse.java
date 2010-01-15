/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2010 Zimbra, Inc.
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

package com.zimbra.cs.client.soap;

import java.util.List;

public class LmcSearchResponse extends LmcSoapResponse {
    
    private String mOffset;
    private String mMore;
    private List mResults;
    
    public void setOffset(String o) { mOffset = o; }
    public void setMore(String m) { mMore = m; }
    public void setResults(List l) { mResults = l; }
    
    public String getOffset() { return mOffset; }
    public String getMore() { return mMore; }
    public List getResults() { return mResults; }
}
