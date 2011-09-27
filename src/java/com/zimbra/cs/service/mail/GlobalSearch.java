/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.index.global.GlobalSearchHit;
import com.zimbra.cs.index.global.GlobalSearchQuery;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.mail.message.GlobalSearchRequest;
import com.zimbra.soap.mail.message.GlobalSearchResponse;

/**
 * Search your documents as well as documents shared with you.
 *
 * @author ysasaki
 */
public final class GlobalSearch extends MailDocumentHandler {

    @Override
    public Element handle(Element req, Map<String, Object> ctx) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        return zsc.jaxbToElement(handle((GlobalSearchRequest) JaxbUtil.elementToJaxb(req), ctx));
    }

    private GlobalSearchResponse handle(GlobalSearchRequest req, Map<String, Object> ctx) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(ctx);
        Mailbox mbox = getRequestedMailbox(zsc);

        List<GlobalSearchHit> hits = mbox.index.search(new GlobalSearchQuery(req.getQuery()));
        GlobalSearchResponse resp = new GlobalSearchResponse();
        resp.setDocuments(toResponse(hits));
        return resp;
    }

    private List<GlobalSearchResponse.Document> toResponse(List<GlobalSearchHit> hits) {
        List<GlobalSearchResponse.Document> result = new ArrayList<GlobalSearchResponse.Document>(hits.size());
        for (GlobalSearchHit hit : hits) {
            GlobalSearchResponse.Document doc = new GlobalSearchResponse.Document();
            doc.setID(hit.getDocument().getGID().toString());
            doc.setScore(hit.getScore());
            doc.setDate(hit.getDocument().getDate());
            doc.setSize(hit.getDocument().getSize());
            doc.setName(hit.getDocument().getFilename());
            doc.setFragment(hit.getDocument().getFragment());
            result.add(doc);
        }
        return result;
    }

}
