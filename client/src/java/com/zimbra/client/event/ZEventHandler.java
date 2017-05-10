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

package com.zimbra.client.event;
import com.zimbra.soap.type.AccountWithModifications;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.service.ServiceException;

public class ZEventHandler {

    /**
     * default implementation is a no-op.
     * 
     * @param refreshEvent the refresh event
     * @param mailbox the mailbox that had the event
     */
    public void handleRefresh(ZRefreshEvent refreshEvent, ZMailbox mailbox) throws ServiceException {
        // do nothing by default
    }

    /**
     *
     * default implementation is a no-op
     *
     * @param event the create event
     * @param mailbox the mailbox that had the event
     */
    public void handleCreate(ZCreateEvent event, ZMailbox mailbox) throws ServiceException {
        // do nothing by default
    }

    /**
     *
     * default implementation is a no-op
     *
     * @param event the modify event
     * @param mailbox the mailbox that had the event
     */
    public void handleModify(ZModifyEvent event, ZMailbox mailbox) throws ServiceException {
        // do nothing by default
    }

     /**
     *
     * default implementation is a no-op
     *
     * @param event the delete event
     * @param mailbox the mailbox that had the event
     */
    public void handleDelete(ZDeleteEvent event, ZMailbox mailbox) throws ServiceException {
        // do nothing by default
    }

    /**
    *
    * default implementation is a no-op
    *
    * @param mods JAXB class with pending modifications
    */
    public void handlePendingModification(int changeId, AccountWithModifications info) throws ServiceException {
       // do nothing by default
   }
}
