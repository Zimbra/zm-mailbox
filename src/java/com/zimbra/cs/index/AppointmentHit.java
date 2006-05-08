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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Feb 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.index;

import org.apache.lucene.document.Document;

import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;

/**
 * @author tim
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AppointmentHit extends ZimbraHit {
    
    protected int mId;
    protected Appointment mAppt;

    /**
     * @param results
     * @param mbx
     * @param d
     * @param score
     */
    public AppointmentHit(ZimbraQueryResultsImpl results, Mailbox mbx, 
    		int mailItemId, Document d, float score, MailItem.UnderlyingData ud) throws ServiceException {
        super(results, mbx, score);
        mId = mailItemId;
        if (ud != null)
            mAppt = (Appointment)mbx.getItemFromUnderlyingData(ud);
    }
    
    /**
     * @param results
     * @param mbx
     * @param id
     * @param score
     */
    public AppointmentHit(ZimbraQueryResultsImpl results, Mailbox mbx, int id,
            float score, MailItem.UnderlyingData ud) throws ServiceException {
        super(results, mbx, score);

        mId = id;
        
        if (ud != null)
            mAppt = (Appointment)mbx.getItemFromUnderlyingData(ud);
    }
    
    public Appointment getAppointment() throws ServiceException {
        if (mAppt == null) {
            mAppt = this.getMailbox().getAppointmentById(null, mId);
        }
        return mAppt;
    }

    public long getDate() throws ServiceException {
        return getAppointment().getDate();
    }
    
    public int getSize() throws ServiceException {
        return (int)getAppointment().getSize();
    }
    
    public int getConversationId() throws ServiceException {
        assert(false);
        return 0;
    }
    
    public int getItemId() throws ServiceException {
        return mId;
    }
    
    public byte getItemType() throws ServiceException {
        return MailItem.TYPE_APPOINTMENT;
    }

    void setItem(MailItem item) {
        mAppt = (Appointment)item;
    }
    
    boolean itemIsLoaded() {
        return (mId == 0) || (mAppt != null);
    }
    
    public String getSubject() throws ServiceException {
        return getAppointment().getSubject();
    }
    
    public String getName() throws ServiceException {
        return getAppointment().getSubject();
    }
}
