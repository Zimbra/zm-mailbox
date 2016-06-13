/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
