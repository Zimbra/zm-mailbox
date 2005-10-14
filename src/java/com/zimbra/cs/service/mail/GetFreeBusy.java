/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.calendar.FreeBusy;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraContext;
import com.zimbra.soap.WriteOpDocumentHandler;

import java.util.*;

import com.zimbra.cs.service.util.*;

public class GetFreeBusy extends WriteOpDocumentHandler {

    
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
    private static StopWatch sWatch = StopWatch.getInstance("GetFreeBusy");
    
    private static final long MSEC_PER_DAY = 1000*60*60*24;
    
    private static final long MAX_PERIOD_SIZE_IN_DAYS = 200;
    
    
    public Element handle(Element request, Map context) throws ServiceException 
    {
        long startTime = sWatch.start();
        try {
            ZimbraContext zc = getZimbraContext(context);

            long rangeStart = request.getAttributeLong(MailService.A_APPT_START_TIME);
            long rangeEnd = request.getAttributeLong(MailService.A_APPT_END_TIME);
            
            if (rangeEnd < rangeStart) {
                throw ServiceException.INVALID_REQUEST("End time must be after Start time", null);
            }
            
            long days = (rangeEnd-rangeStart)/MSEC_PER_DAY;
            if (days > MAX_PERIOD_SIZE_IN_DAYS) {
                throw ServiceException.INVALID_REQUEST("Requested range is too large (Maximum "+MAX_PERIOD_SIZE_IN_DAYS+" days)", null);
            }
            
            
            
            Element response = zc.createElement(MailService.GET_FREE_BUSY_RESPONSE);
            
            String idParam = request.getAttribute(MailService.A_UID);
            
            System.out.println("<GetFreeBusyRequest uid=\""+ idParam + "\">");
            
            ArrayList /*ParseItemId*/local = new ArrayList();
            HashMap /*Server,ParamStr*/remote = new HashMap();
            partitionItems(zc, response, rangeStart, rangeEnd, idParam, local, remote);
            proxyRemoteItems(context, zc, response, rangeStart, rangeEnd, remote);

            if (!local.isEmpty()) {
                for (Iterator iter = local.iterator(); iter.hasNext();)
                {
                    ParseMailboxID id = (ParseMailboxID)(iter.next());
                    try {
                        getForOneMailbox(zc, response, id, rangeStart, rangeEnd);
                    } catch (ServiceException e) {
                        addFailureInfo(response, rangeStart, rangeEnd, id.toString(), e);
                    }
                }
            }
            return response;
        } finally {
            sWatch.stop(startTime);
        }
    }
    
    protected static void proxyRemoteItems(Map context,ZimbraContext zc,Element response,long rangeStart,long rangeEnd,Map /* Server, ParamStr */ remote)
    {
        for (Iterator it = remote.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            
            String server = (String)(entry.getKey());
            String paramStr = ((StringBuffer)(entry.getValue())).toString();
            System.out.println("PRXOXYING <GetFreeBusyRequest uid=\""+paramStr+"\"> to server "+server);
            
            try {
                Element req = zc.createElement(MailService.GET_FREE_BUSY_REQUEST);
                req.addAttribute(MailService.A_APPT_START_TIME, rangeStart);
                req.addAttribute(MailService.A_APPT_END_TIME, rangeEnd);
                req.addAttribute(MailService.A_UID, paramStr);
                
                Element remoteResponse = fwdRequestToRemoteHost(zc, req, context, server);
                
                for (Iterator respIter = remoteResponse.elementIterator(); respIter.hasNext();)
                {
                    Element thisElt = (Element)(respIter.next());
                    thisElt.detach();
                    response.addElement(thisElt);
                }
            } catch (ServiceException e) {
                String[] idStrs = paramStr.split(",");
                for (int i = 0; i < idStrs.length; i++) {
                    addFailureInfo(response, rangeStart, rangeEnd, idStrs[i], e);
                }
            } catch (SoapFaultException e) {
                String[] idStrs = paramStr.split(",");
                for (int i = 0; i < idStrs.length; i++) {
                    addFailureInfo(response, rangeStart, rangeEnd, idStrs[i], e);
                }
            }
        }
    }
    
    protected static void partitionItems(ZimbraContext zc, Element response, long rangeStart, long rangeEnd,
            String idParam, List /* ParseMailboxID */local, Map /*String,StringBuffer*/ remote)
    {
        String[] idStrs = idParam.split(",");
        for (int i = 0; i < idStrs.length; i++) {
            try {
                ParseMailboxID id = ParseMailboxID.parse(idStrs[i]);
                if (id != null) {
                    if (id.isLocal()) {
                        local.add(id);
                    } else {
                        String serverId = id.getServer();
                        assert(serverId != null);
                            
                        StringBuffer remoteStr = (StringBuffer)remote.get(serverId);
                        if (remoteStr == null) {
                            remoteStr = new StringBuffer(idStrs[i]);
                            remote.put(serverId, remoteStr);
                        } else {
                            remoteStr.append(",").append(idStrs[i]);
                        }
                    }
                }
            } catch (ServiceException e) {
                addFailureInfo(response, rangeStart, rangeEnd, idStrs[i], e);
            }
            
        }
    }
    
    protected static void addFailureInfo(Element response, long rangeStart, long rangeEnd, String idStr, Exception e)
    {
        sLog.debug("Could not get FreeBusy data for id "+idStr, e);
        Element mbxResp = response.addElement(MailService.E_FREEBUSY_USER);
        mbxResp.addAttribute(MailService.A_ID,idStr);
        Element elt;
        elt = mbxResp.addElement(MailService.E_FREEBUSY_NO_DATA);
        elt.addAttribute(MailService.A_APPT_START_TIME, rangeStart);
        elt.addAttribute(MailService.A_APPT_END_TIME, rangeEnd);
    }
    
    protected static void getForOneMailbox(ZimbraContext zc, Element response, ParseMailboxID id, long start, long end)
    throws ServiceException {
        if (id.isLocal()) {
            Element mbxResp = response.addElement(MailService.E_FREEBUSY_USER);
            mbxResp.addAttribute(MailService.A_ID,id.getString());
            
            Mailbox mbox = id.getMailbox();

            FreeBusy fb = mbox.getFreeBusy(zc.getOperationContext(), start, end);
            
            for (Iterator iter = fb.iterator(); iter.hasNext(); ) {
                FreeBusy.Interval cur = (FreeBusy.Interval)iter.next();
                String status = cur.getStatus();
                Element elt;
                if (status.equals(IcalXmlStrMap.FBTYPE_FREE)) {
                    elt = mbxResp.addElement(MailService.E_FREEBUSY_FREE);
                } else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY)) {
                    elt = mbxResp.addElement(MailService.E_FREEBUSY_BUSY);
                } else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE)) {
                    elt = mbxResp.addElement(MailService.E_FREEBUSY_BUSY_TENTATIVE);
                } else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE)) {
                    elt = mbxResp.addElement(MailService.E_FREEBUSY_BUSY_UNAVAILABLE);
                } else {
                    assert(false);
                    elt = null;
                }
                
                elt.addAttribute(MailService.A_APPT_START_TIME, cur.getStart());
                elt.addAttribute(MailService.A_APPT_END_TIME, cur.getEnd());
            }
        } else {
            throw new IllegalArgumentException("REMOTE MAILBOXES NOT SUPPORTED YET\n");
        }
    }
    
}
