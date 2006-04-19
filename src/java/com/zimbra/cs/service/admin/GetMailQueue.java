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
package com.zimbra.cs.service.admin;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.rmgmt.RemoteMailQueue;
import com.zimbra.cs.rmgmt.RemoteMailQueue.QueueAttr;
import com.zimbra.cs.rmgmt.RemoteMailQueue.SummaryItem;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class GetMailQueue extends AdminDocumentHandler {

    public static final int MAIL_QUEUE_QUERY_DEFAULT_LIMIT = 30;
    public static final int MAIL_QUEUE_SCAN_DEFUALT_WAIT_SECONDS = 3;
    public static final int MAIL_QUEUE_SUMMARY_CUTOFF = 100;
    
	public Element handle(Element request, Map context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        Element serverElem = request.getElement(AdminService.E_SERVER);
        String serverName = serverElem.getAttribute(AdminService.A_NAME);
        
        Server server = prov.getServerByName(serverName);
        if (server == null) {
            throw ServiceException.INVALID_REQUEST("server with name " + serverName + " could not be found", null);
        }
        
        Element queueElem = serverElem.getElement(AdminService.E_QUEUE);
        String queueName = queueElem.getAttribute(AdminService.A_NAME);
        boolean scan = queueElem.getAttributeBool(AdminService.A_SCAN, false);
        long waitMillis = (queueElem.getAttributeLong(AdminService.A_WAIT, MAIL_QUEUE_SCAN_DEFUALT_WAIT_SECONDS)) * 1000;
        
        Element queryElem = queueElem.getElement(AdminService.E_QUERY);
        int offset = (int)queryElem.getAttributeLong(AdminService.A_OFFSET, 0);
        int limit = (int)queryElem.getAttributeLong(AdminService.A_LIMIT, MAIL_QUEUE_QUERY_DEFAULT_LIMIT);
        Query query = buildLuceneQuery(queryElem);
        
        RemoteMailQueue rmq = RemoteMailQueue.getRemoteMailQueue(server, queueName, scan);
        boolean stillScanning = rmq.waitForScan(waitMillis);
        RemoteMailQueue.SearchResult sr = rmq.search(query, offset, limit);
        
        Element response = lc.createElement(AdminService.GET_MAIL_QUEUE_RESPONSE);
        serverElem = response.addElement(AdminService.E_SERVER);
        serverElem.addAttribute(AdminService.A_NAME, serverName);
     
        queueElem = serverElem.addElement(AdminService.E_QUEUE);
        queueElem.addAttribute(AdminService.A_NAME, queueName);
        queueElem.addAttribute(AdminService.A_TIME, rmq.getScanTime());
        queueElem.addAttribute(AdminService.A_SCAN, stillScanning);
        queueElem.addAttribute(AdminService.A_TOTAL, rmq.getNumMessages());
        queueElem.addAttribute(AdminService.A_MORE, ((offset + limit) < sr.hits)); 
        
        for (QueueAttr attr : sr.sitems.keySet()) {
            List<SummaryItem> slist = sr.sitems.get(attr);
            Collections.sort(slist);
            Element qsElem = queueElem.addElement(AdminService.A_QUEUE_SUMMARY);
            qsElem.addAttribute(AdminService.A_TYPE, attr.toString());
            int i = 0;
            for (SummaryItem sitem : slist) {
                i++;
                if (i > MAIL_QUEUE_SUMMARY_CUTOFF) {
                    break;
                }
                Element qsiElem = qsElem.addElement(AdminService.A_QUEUE_SUMMARY_ITEM);
                qsiElem.addAttribute(AdminService.A_N, sitem.count());
                qsiElem.addAttribute(AdminService.A_T, sitem.term());
            }
        }

        for (Map<QueueAttr,String> qitem : sr.qitems) {
            Element qiElem = queueElem.addElement(AdminService.A_QUEUE_ITEM);
            for (QueueAttr attr : qitem.keySet()) {
                qiElem.addAttribute(attr.toString(), qitem.get(attr));
            }
        }
	    return response;
	}

	public static Query buildLuceneQuery(Element queryElem) throws ServiceException {
		BooleanQuery fq = new BooleanQuery();
		boolean emptyQuery = true;
        for (Iterator fieldIter = queryElem.elementIterator(AdminService.E_FIELD); fieldIter.hasNext();) {
        	emptyQuery = false;
        	Element fieldElement = (Element)fieldIter.next();
        	String fieldName = fieldElement.getAttribute(AdminService.A_NAME);
        	BooleanQuery mq = new BooleanQuery();
        	for (Iterator matchIter = fieldElement.elementIterator(AdminService.E_MATCH); matchIter.hasNext();) {
        		Element matchElement = (Element)matchIter.next();
        		String matchValue = matchElement.getAttribute(AdminService.A_VALUE);
        		Term term = new Term(fieldName, matchValue);
        		TermQuery termQuery = new TermQuery(term);
                mq.add(new TermQuery(term), false, false); // OR all the matches
        	}
        	fq.add(mq, true, false); // AND all the fields
        }
        if (emptyQuery) {
        	return null;
        } else {
        	return fq;
        }
	}

}
