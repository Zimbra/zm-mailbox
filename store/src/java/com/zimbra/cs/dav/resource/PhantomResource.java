/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.dav.resource;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.mailbox.MailItem;

public abstract class PhantomResource extends DavResource {

    protected static final String BY_DATE = "by-date";
    protected static final String BY_TYPE = "by-type";
    protected static final String BY_SENDER = "by-sender";

    protected static final String TODAY = "today";
    protected static final String WEEK  = "last-week";
    protected static final String MONTH = "last-month";
    protected static final String YEAR  = "last-year";
    protected static final String ALL   = "all";

    protected List<String> mTokens;
    protected long mNow;

    protected static final Set<MailItem.Type> SEARCH_TYPES = EnumSet.of(MailItem.Type.MESSAGE);

    PhantomResource(String uri, String owner, List<String> tokens) {
        super(uri, owner);
        mTokens = tokens;
        mNow = System.currentTimeMillis();
        setCreationDate(mNow);
        setLastModifiedDate(mNow);
        setProperty(DavElements.P_GETCONTENTLENGTH, "0");
        setProperty(DavElements.P_DISPLAYNAME, tokens.get(tokens.size()-1));
    }

    protected static List<String> parseUri(String uri) {
        ArrayList<String> l = new ArrayList<String>();
        StringTokenizer tok = new StringTokenizer(uri, "/");
        while (tok.hasMoreTokens())
            l.add(tok.nextToken());
        return l;
    }

    @Override
    public void delete(DavContext ctxt) throws DavException {
        throw new DavException("cannot delete this resource", HttpServletResponse.SC_FORBIDDEN, null);
    }
}
