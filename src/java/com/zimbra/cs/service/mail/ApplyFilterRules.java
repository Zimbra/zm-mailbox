/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jsieve.parser.generated.Node;
import org.apache.jsieve.parser.generated.ParseException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.ZimbraSoapContext;


public class ApplyFilterRules extends MailDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("cannot access account");

        // Get rules.
        String fullScript = RuleManager.getRules(account);
        if (StringUtil.isNullOrEmpty(fullScript)) {
            throw ServiceException.INVALID_REQUEST("Account has no filter rules defined.", null);
        }
        
        List<Element> ruleElements = request.listElements(MailConstants.E_FILTER_RULE);
        if (ruleElements.size() == 0) {
            String msg = String.format("No %s elements specified.", MailConstants.E_FILTER_RULE);
            throw ServiceException.INVALID_REQUEST(msg, null);
        }

        // Concatenate script parts and create a new script to run on existing messages.
        StringBuilder buf = new StringBuilder();
        for (Element ruleEl : ruleElements) {
            String name = ruleEl.getAttribute(MailConstants.A_NAME);
            String singleRule = RuleManager.getRuleByName(fullScript, name);
            if (singleRule == null) {
                String msg = String.format("Could not find a rule named '%s'", name);
                throw ServiceException.INVALID_REQUEST(msg, null);
            }
            buf.append(singleRule).append("\n");
        }
        String partialScript = buf.toString();
        ZimbraLog.filter.debug("Applying partial script to existing messages: %s", partialScript);
        Node node = null;
        try {
            node = RuleManager.parse(partialScript); 
        } catch (ParseException e) {
            throw ServiceException.FAILURE("Unable to parse Sieve script: " + partialScript, e);
        }
        
        // Get the ids of the messages to filter.
        String idCsv = getElementText(request, MailConstants.E_IDS);
        String query = getElementText(request, MailConstants.E_QUERY);
        if (idCsv != null && query != null) {
            String msg = String.format("Cannot specify both %s and %s elements.",
                MailConstants.E_IDS, MailConstants.E_QUERY);
            throw ServiceException.INVALID_REQUEST(msg, null);
        }

        Mailbox mbox = getRequestedMailbox(zsc);
        List<Integer> messageIds = new ArrayList<Integer>();
        List<Integer> affectedIds = new ArrayList<Integer>();
        
        if (idCsv != null) {
            String[] ids = idCsv.split(",");
            for (String id : ids) {
                messageIds.add(Integer.valueOf(id));
            }
        } else if (query != null) {
            byte[] types = new byte[] { MailItem.TYPE_MESSAGE };
            ZimbraQueryResults results = null;
            
            try {
                results = mbox.search(new Mailbox.OperationContext(mbox), query, types,
                    MailboxIndex.SortBy.NONE, Integer.MAX_VALUE);
                while (results.hasNext()) {
                    ZimbraHit hit = results.getNext();
                    messageIds.add(hit.getItemId());
                }
            } catch (Exception e) {
                String msg = String.format("Unable to run search for query: '%s'", query);
                throw ServiceException.INVALID_REQUEST(msg, e);
            } finally {
                if (results != null) {
                    results.doneWithSearchResults();
                }
            }
        } else {
            String msg = String.format("Must specify either the %s or %s element.",
                MailConstants.E_IDS, MailConstants.E_QUERY);
            throw ServiceException.INVALID_REQUEST(msg, null);
        }
        
        // Apply filter rules.
        for (int id : messageIds) {
            if (RuleManager.applyRulesToExistingMessage(mbox, id, node)) {
                affectedIds.add(id);
            }
        }
        
        // Send response.
        Element response = zsc.createElement(MailConstants.APPLY_FILTER_RULES_RESPONSE);
        if (affectedIds.size() > 0) {
            response.addAttribute(MailConstants.A_AFFECTED_IDS, StringUtil.join(",", affectedIds));
        }
        return response;
    }
    
    private String getElementText(Element parent, String childName) {
        Element child = parent.getOptionalElement(childName);
        if (child == null) {
            return null;
        }
        return child.getTextTrim();
    }
}
