/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;

public class CalendarItemDavBaseNameValidator extends DavBaseNameValidator {
    private final String uid;
    private boolean lookedForItemWithUid = false;
    private CalendarItem origItemWithSameUid;

    public CalendarItemDavBaseNameValidator(Collection collection, DavContext ctxt, Mailbox mbox,
            String clientChoiceForDavBaseName, String uid) {
        super(collection, ctxt, mbox, clientChoiceForDavBaseName);
        this.uid = uid;
        suffix = CalendarObject.CAL_EXTENSION;
    }

    @Override
    protected MailItem getPreExistingWithUniqueId() {
        if (!lookedForItemWithUid) {
            try {
                origItemWithSameUid = mbox.getCalendarItemWithUidInFolder(ctxt.getOperationContext(), uid,
                        collection.mId);
            } catch (ServiceException e) {
                ZimbraLog.dav.debug("Exception thrown when looking for item with same UID", e);
                origItemWithSameUid = null;
            }
            lookedForItemWithUid = true;
        }
        return origItemWithSameUid;
    }

    @Override
    public ITEM_IN_DB getStateOfItemInDB() {
        getExistingMailItemWithClientChosenDavBaseName();
        if (null != existingMailItemWithClientChosenDavBaseName) {
            if (existingMailItemWithClientChosenDavBaseName instanceof CalendarItem) {
                CalendarItem calItem = (CalendarItem) existingMailItemWithClientChosenDavBaseName;
                if (uid.equals(calItem.getUid())) {
                    stateOfItemInDb = ITEM_IN_DB.EXISTS;
                } else {
                    stateOfItemInDb = ITEM_IN_DB.CONFLICTS;
                }
            } else {
                stateOfItemInDb = ITEM_IN_DB.CONFLICTS;
            }
            newHref = clientChoiceForDavBaseName;
            return stateOfItemInDb;
        }
        return super.getStateOfItemInDB();
    }

    @Override
    public String chooseNewDavBaseName() {
        DavResource resource = null;
        String newName = uid + suffix;
        if (validateDavBaseName(newName)) {
            try {
                resource = UrlNamespace.getResourceAt(ctxt, mbox.getAccount().getName(), getPathForBaseName(newName));
            } catch (DavException | ServiceException e) {
            }
            if (resource == null) {
                return newName;
            }
        }
        return super.chooseNewDavBaseName();
    }
}
