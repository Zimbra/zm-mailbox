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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.calendar.FreeBusy;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.service.util.ParseMailboxID;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.soap.ZimbraSoapContext;

public class GetFreeBusy extends MailDocumentHandler {

    
//    <GetFreeBusyRequest s="date" e="date" [uid="id,..."]/>
//    <GetFreeBusyResponse>
//      <usr id="id">
//        <f s="date" e="date"/>*
//        <b s="date" e="date"/>*
//        <t s="date" e="date"/>*
//        <o s="date" e="date"/>*
//      </usr>  
//    <GetFreeBusyResponse>
//
//    (f)ree (b)usy (t)entative and (o)ut-of-office
    
    private static Log sLog = LogFactory.getLog(GetFreeBusy.class);
    
    private static final long MSEC_PER_DAY = 1000*60*60*24;
    
    private static final long MAX_PERIOD_SIZE_IN_DAYS = 200;

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zc = getZimbraSoapContext(context);
        
        long rangeStart = request.getAttributeLong(MailConstants.A_CAL_START_TIME);
        long rangeEnd = request.getAttributeLong(MailConstants.A_CAL_END_TIME);
        
        if (rangeEnd < rangeStart)
            throw ServiceException.INVALID_REQUEST("End time must be after Start time", null);

        long days = (rangeEnd - rangeStart) / MSEC_PER_DAY;
        if (days > MAX_PERIOD_SIZE_IN_DAYS)
            throw ServiceException.INVALID_REQUEST("Requested range is too large (Maximum "+MAX_PERIOD_SIZE_IN_DAYS+" days)", null);

        Element response = getResponseElement(zc);
        
        // MailConstants.A_UID should be deprecated at some point, bug 21776, comment #14
        String idParam = request.getAttribute(MailConstants.A_UID, null);    // comma-separated list of account emails or zimbraId GUIDs that *must* match UUID format
        String uidParam = request.getAttribute(MailConstants.A_ID, null);    // comma-separated list of account zimbraId GUIDs
        String nameParam = request.getAttribute(MailConstants.A_NAME, null); // comma-separated list of account emails
        
        List<ParseMailboxID> local = new ArrayList<ParseMailboxID>();
        Map<String, StringBuilder> remote = new HashMap<String, StringBuilder>();
        partitionItems(zc, response, rangeStart, rangeEnd, idParam, uidParam, nameParam, local, remote);
        proxyRemoteItems(context, zc, response, rangeStart, rangeEnd, remote);
        
        if (!local.isEmpty()) {
            for (ParseMailboxID id : local) {
                try {
                    getForOneMailbox(zc, response, id, rangeStart, rangeEnd);
                } catch (ServiceException e) {
                    addFailureInfo(response, rangeStart, rangeEnd, id.toString(), e);
                }
            }
        }
        return response;
    }

    protected static void proxyRemoteItems(
            Map<String, Object> context, ZimbraSoapContext zc, Element response,
            long rangeStart, long rangeEnd, Map<String, StringBuilder> remote) {
        Provisioning prov = Provisioning.getInstance();
        for (Map.Entry<String, StringBuilder> entry : remote.entrySet()) {
            // String server = entry.getKey();
            String paramStr = entry.getValue().toString();
            String[] idStrs = paramStr.split(",");

            try {
                Element req = zc.getRequestProtocol().getFactory().createElement(MailConstants.GET_FREE_BUSY_REQUEST);
                req.addAttribute(MailConstants.A_CAL_START_TIME, rangeStart);
                req.addAttribute(MailConstants.A_CAL_END_TIME, rangeEnd);
                req.addAttribute(MailConstants.A_UID, paramStr);

                // hack: use the ID of the first user
                Account acct = prov.get(AccountBy.name, idStrs[0]);
                if (acct == null)
                    acct = prov.get(AccountBy.id, idStrs[0]);
                if (acct != null) {
                    Element remoteResponse = proxyRequest(req, context, acct.getId());
                    for (Element thisElt : remoteResponse.listElements())
                        response.addElement(thisElt.detach());
                } else {
                    ZimbraLog.calendar.debug("Account " + idStrs[0] + " not found while searching free/busy");
                }
            } catch (SoapFaultException e) {
                for (int i = 0; i < idStrs.length; i++)
                    addFailureInfo(response, rangeStart, rangeEnd, idStrs[i], e);
            } catch (ServiceException e) {
                for (int i = 0; i < idStrs.length; i++)
                    addFailureInfo(response, rangeStart, rangeEnd, idStrs[i], e);
            }
        }
    }
    
    protected static void partitionItems(ZimbraSoapContext zc, Element response, long rangeStart, long rangeEnd,
                                         String idParam, String uidParam, String nameParam, List<ParseMailboxID> local, Map<String, StringBuilder> remote) {
        
        String[] idStrs = null;
        
        // idParam should be deprecated at some point, bug 21776 comment #14
        if (idParam != null) {
            idStrs = idParam.split(",");
            for (int i = 0; i < idStrs.length; i++) {
                try {
                    ParseMailboxID id = null;
                    if (Provisioning.isUUID(idStrs[i]))
                        id = ParseMailboxID.byAccountId(idStrs[i]);
                    else
                        id = ParseMailboxID.byEmailAddress(idStrs[i]);
                    partitionItems(zc, response, rangeStart, rangeEnd, id, local, remote);
                } catch (ServiceException e) {
                    addFailureInfo(response, rangeStart, rangeEnd, idStrs[i], e);
                }
            }
        }
        
        if (uidParam != null) {
            idStrs = uidParam.split(",");
            for (int i = 0; i < idStrs.length; i++) {
                try {
                    ParseMailboxID id = ParseMailboxID.byAccountId(idStrs[i]);
                    partitionItems(zc, response, rangeStart, rangeEnd, id, local, remote);
                } catch (ServiceException e) {
                    addFailureInfo(response, rangeStart, rangeEnd, idStrs[i], e);
                }
            }
        }
        
        if (nameParam != null) {
            idStrs = nameParam.split(",");
            for (int i = 0; i < idStrs.length; i++) {
                try {
                    ParseMailboxID id = ParseMailboxID.byEmailAddress(idStrs[i]);
                    partitionItems(zc, response, rangeStart, rangeEnd, id, local, remote);
                } catch (ServiceException e) {
                    addFailureInfo(response, rangeStart, rangeEnd, idStrs[i], e);
                }
            }
        }
    }
    
    protected static void partitionItems(ZimbraSoapContext zc, Element response, long rangeStart, long rangeEnd,
                                         ParseMailboxID id, List<ParseMailboxID> local, Map<String, StringBuilder> remote) {
        String idStr = id.getString();
            
        if (id != null) {
            if (id.isLocal()) {
                local.add(id);
            } else {
                String serverId = id.getServer();
                        
                assert(serverId != null);
                        
                StringBuilder remoteStr = remote.get(serverId);
                if (remoteStr == null) {
                    remoteStr = new StringBuilder(idStr);
                    remote.put(serverId, remoteStr);
                } else {
                    remoteStr.append(",").append(idStr);
                }
            }
        }
    }

    protected static void addFailureInfo(Element response, long rangeStart, long rangeEnd, String idStr, Exception e) {
        sLog.debug("Could not get FreeBusy data for id " + idStr, e);
        Element usr = response.addElement(MailConstants.E_FREEBUSY_USER);
        usr.addAttribute(MailConstants.A_ID, idStr);
        usr.addElement(MailConstants.E_FREEBUSY_NO_DATA)
           .addAttribute(MailConstants.A_CAL_START_TIME, rangeStart)
           .addAttribute(MailConstants.A_CAL_END_TIME, rangeEnd);
    }
    
    protected static void getForOneMailbox(ZimbraSoapContext zc, Element response, ParseMailboxID id, long start, long end)
    throws ServiceException {
        if (id.isLocal()) {
            Element mbxResp = response.addElement(MailConstants.E_FREEBUSY_USER);
            mbxResp.addAttribute(MailConstants.A_ID,id.getString());
            
            Mailbox mbox = id.getMailbox();

            FreeBusy fb = mbox.getFreeBusy(start, end);
            for (Iterator<FreeBusy.Interval> iter = fb.iterator(); iter.hasNext(); ) {
                FreeBusy.Interval cur = iter.next();
                String status = cur.getStatus();
                Element elt;
                if (status.equals(IcalXmlStrMap.FBTYPE_FREE)) {
                    elt = mbxResp.addElement(MailConstants.E_FREEBUSY_FREE);
                } else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY)) {
                    elt = mbxResp.addElement(MailConstants.E_FREEBUSY_BUSY);
                } else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE)) {
                    elt = mbxResp.addElement(MailConstants.E_FREEBUSY_BUSY_TENTATIVE);
                } else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE)) {
                    elt = mbxResp.addElement(MailConstants.E_FREEBUSY_BUSY_UNAVAILABLE);
                } else {
                    assert(false);
                    elt = null;
                }
                
                elt.addAttribute(MailConstants.A_CAL_START_TIME, cur.getStart());
                elt.addAttribute(MailConstants.A_CAL_END_TIME, cur.getEnd());
            }
        }
    }
}
