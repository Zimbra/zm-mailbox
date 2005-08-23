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
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
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
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.StopWatch;
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
    
    private static Log mLog = LogFactory.getLog(GetFreeBusy.class);
    private static StopWatch sWatch = StopWatch.getInstance("GetFreeBusy");
    
    
    public Element handle(Element request, Map context)
            throws ServiceException {
        // TODO Auto-generated method stub
        
        long startTime = sWatch.start();
        try {
            ZimbraContext lc = getZimbraContext(context);

            long rangeStart = request.getAttributeLong(MailService.A_APPT_START_TIME);
            long rangeEnd = request.getAttributeLong(MailService.A_APPT_END_TIME);
            
            Element response = lc.createElement(MailService.GET_FREE_BUSY_RESPONSE);
            
            String idParam = request.getAttribute(MailService.A_UID);
            String[] idStrs = idParam.split(",");
            for (int i = 0; i < idStrs.length; i++) {
                try {
                    ParseMailboxID id = ParseMailboxID.parse(idStrs[i]);
                    if (id != null) {
                        getForOneMailbox(response, id, rangeStart, rangeEnd);
                    }
                } catch (ServiceException e) {
                    mLog.debug("Could not get FreeBusy data for id "+idStrs[i], e);
                    Element mbxResp = response.addElement(MailService.E_FREEBUSY_USER);
                    mbxResp.addAttribute(MailService.A_ID,idStrs[i]);
                    Element elt;
                    elt = mbxResp.addElement(MailService.E_FREEBUSY_NO_DATA);
                    elt.addAttribute(MailService.A_APPT_START_TIME, rangeStart);
                    elt.addAttribute(MailService.A_APPT_END_TIME, rangeEnd);
                }
            }
            
            return response;
        } finally {
            sWatch.stop(startTime);
        }
    }
    
    private void getForOneMailbox(Element response, ParseMailboxID id, long start, long end) throws ServiceException
    {
        if (id.isLocal()) {
            Element mbxResp = response.addElement(MailService.E_FREEBUSY_USER);
            mbxResp.addAttribute(MailService.A_ID,id.getString());
            
            Mailbox mbx = id.getMailbox();

            FreeBusy fb = mbx.getFreeBusy(start, end);
            
            for (Iterator iter = fb.iterator(); iter.hasNext(); ) {
//            for (FreeBusy.Interval cur = fb.getHead(); cur!=null; cur = cur.getNext()) {
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
