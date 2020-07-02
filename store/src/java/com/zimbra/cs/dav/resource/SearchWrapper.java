/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.index.MessageHit;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;

/**
 * SearchWrapper is a phantom resource that resembles a Collection (folder)
 * with the contents dynamically generated from email contents in the mailbox.
 *
 * @author jylee
 *
 */
public class SearchWrapper extends PhantomResource {

    protected static final long DAY_IN_MS = 24 * 60 * 60 * 1000;
    protected static final long WEEK_IN_MS = 7 * DAY_IN_MS;
    protected static final long MONTH_IN_MS = 4 * WEEK_IN_MS;

    protected static final int SEARCH_LIMIT = 100;

    private String mContentType;
    private StringBuilder mQuery;

    public SearchWrapper(String uri, String owner) {
        this(uri, owner, parseUri(uri));
    }

    public SearchWrapper(String uri, String owner, List<String> tokens) {
        super(uri, owner, tokens);
        mContentType = null;
        mQuery = new StringBuilder();
        mQuery.append("has:attachment ");
        String prevToken = null;
        for (String token : tokens) {
            buildQuery(prevToken, token);
            prevToken = token;
        }
    }

    private void buildQuery(String prevTerm, String term) {
        if (term.equals(TODAY))
            mQuery.append("after:\"-1day\" ");
        else if (term.equals(WEEK))
            mQuery.append("after:\"-1week\" ");
        else if (term.equals(MONTH))
            mQuery.append("after:\"-1month\" ");
        else if (term.equals(YEAR))
            mQuery.append("after:\"-1year\" ");
        else if (BrowseWrapper.BY_SENDER.equals(prevTerm))
            mQuery.append("from:(@"+term+") ");
        else if (BrowseWrapper.BY_TYPE.equals(prevTerm)) {
            mQuery.append("attachment:\""+term+"\" ");
            if (!term.equals("any"))
                mContentType = getActualContentType(term);
        }
    }

    private static HashMap<String,String> sCTMap;

    static {
        sCTMap = new HashMap<String,String>();
        sCTMap.put("ppt",   "application/vnd.ms-powerpoint");
        sCTMap.put("excel", "application/vnd.ms-excel");
        sCTMap.put("word",  "application/msword");
        sCTMap.put("pdf",   "application/pdf");
    }

    private static String getActualContentType(String str) {
        String v = sCTMap.get(str);
        if (v != null)
            return v;

        return str;
    }

    @Override
    public boolean isCollection() {
        return true;
    }

    @Override
    public Collection<DavResource> getChildren(DavContext ctxt) {
        ArrayList<DavResource> children = new ArrayList<DavResource>();
        String user = ctxt.getUser();
        Provisioning prov = Provisioning.getInstance();
        try {
            Account account = prov.get(AccountBy.name, user);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            SearchParams params = new SearchParams();
            params.setQueryString(mQuery.toString());
            params.setTypes(SEARCH_TYPES);
            params.setSortBy(SortBy.NAME_ASC);
            params.setFetchMode(SearchParams.Fetch.NORMAL);
            params.setPrefetch(true);
            params.setChunkSize(SEARCH_LIMIT);
            try (ZimbraQueryResults zqr = mbox.index.search(SoapProtocol.Soap12,
                ctxt.getOperationContext(), params)) {
                while (zqr.hasNext()) {
                    ZimbraHit hit = zqr.getNext();
                    if (hit instanceof MessageHit)
                        addAttachmentResources((MessageHit) hit, children);
                }
            }
        } catch (Exception e) {
            ZimbraLog.dav.error("can't search: uri="+getUri(), e);
        }
        return children;
    }

    private void addAttachmentResources(MessageHit hit, List<DavResource> children) {
        try {
            Message msg = hit.getMessage();
            List<MPartInfo> parts = Mime.getParts(msg.getMimeMessage());
            for (MPartInfo p : parts) {
                String name = p.getFilename();
                String ct = p.getContentType();
                if (mContentType != null && ct != null && !ct.startsWith(mContentType))
                    continue;
                if (name != null && name.length() > 0)
                    children.add(new Attachment(getUri()+name, getOwner(), msg.getDate(), p.getSize()));
            }
        } catch (Exception e) {
            ZimbraLog.dav.error("can't get attachments from msg: itemid:"+hit.getItemId(), e);
        }
    }
}
