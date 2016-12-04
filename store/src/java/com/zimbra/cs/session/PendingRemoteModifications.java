/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.session;

import com.zimbra.client.ZBaseItem;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public final class PendingRemoteModifications extends PendingModifications<ZBaseItem> {

    public static final class ModificationKey extends PendingModifications.ModificationKey {

        /*
         * TODO - Looks like we will be having to deal with ServiceExceptions in
         * numerous places in PendingRemoteModifications. See if there is a
         * cleaner way to handle them.
         */
        public ModificationKey(ZBaseItem item) {
            super("", 0);
            String actId = null;
            int idInMbox = 0;
            try {
                actId = item.getMailbox().getAccountId();
                idInMbox = item.getIdInMailbox();
            } catch (ServiceException e) {
                ZimbraLog.mailbox.warn("error retrieving account id or id in mailbox", e);
            }
            setAccountId(actId);
            setItemId(Integer.valueOf(idInMbox));
        }
    }

}
