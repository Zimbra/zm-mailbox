/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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
package com.zimbra.cs.mailbox.event;

import java.util.Collections;

import com.google.common.collect.Multimap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.OctopusXmlConstants;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.cache.WatchCache;
import com.zimbra.cs.mailbox.event.MailboxEvent.EventFilter;
import com.zimbra.cs.service.account.GetInfo.GetInfoExt;
import com.zimbra.soap.ZimbraSoapContext;

public class NewNotifications {

    private static Log LOG = LogFactory.getLog(NewNotifications.class);
    private static final String CONFIG_KEY = "notifications";
    private static final String LAST_SEEN = "lastSeen";

    private final Mailbox mbox;
    private final OperationContext octxt;

    public static class GetInfoExtension implements GetInfoExt {
        @Override
        public void handle(ZimbraSoapContext zsc, Element getInfoResponse) {
            try {
                Provisioning prov = Provisioning.getInstance();
                Account authAccount = prov.getAccountById(zsc.getAuthtokenAccountId());
                Account targetAccount = prov.getAccountById(zsc.getRequestedAccountId());
                NewNotifications notif = new NewNotifications(authAccount, targetAccount);
                int count = notif.getNewNotificationCount();
                getInfoResponse.addUniqueElement(OctopusXmlConstants.E_NOTIFICATIONS).setText("" + count);
            } catch (ServiceException e) {
                LOG.warn("can't add to GetInfo", e);
            }
        }
    }

    public NewNotifications(Account authAccount, Account targetAccount) throws ServiceException {
        octxt = new OperationContext(authAccount);
        mbox = MailboxManager.getInstance().getMailboxByAccount(targetAccount);
    }

    /**
     * Updates lastSeen time stamp to now.
     *
     * @throws ServiceException
     */
    public void markSeen() throws ServiceException {
        Metadata config = mbox.getConfig(octxt, CONFIG_KEY);
        if (config == null) {
            config = new Metadata();
        }
        config.put(LAST_SEEN, System.currentTimeMillis() / 1000);  // store second since epoch
        mbox.setConfig(octxt, CONFIG_KEY, config);
    }

    /**
     * Returns the time stamp of last time the notifications were seen by the user.
     *
     * @return seconds since epoch
     * @throws ServiceException
     */
    public long getLastSeen() throws ServiceException {
        Metadata config = mbox.getConfig(octxt, CONFIG_KEY);
        if (config == null) {
            config = new Metadata();
        }
        return config.getLong(LAST_SEEN, 0);
    }

    /**
     * Returns the number of new notifications since last seen.
     *
     * @return number of unseen notifications
     * @throws ServiceException
     */
    public int getNewNotificationCount() throws ServiceException {
        return getLog().getEventCount();
    }

    /**
     * Returns the event log containing new notifications
     * the user has received since last seen.
     *
     * @return event log containing notifications
     * @throws ServiceException
     */
    public ItemEventLog getLog() throws ServiceException {
        long lastSeen = getLastSeen();
        EventFilter filter = new EventFilter();
        filter.ops.add(MailboxOperation.CreateMessage);
        filter.since = lastSeen;
        EventLogger logger = EventLogger.getInstance();

        // events for share notifications
        ItemEventLog shareEvents = logger.getLog(octxt.getAuthenticatedUser().getId(), mbox.getId(), Collections.singletonList(Mailbox.ID_FOLDER_INBOX), filter);

        MergedEventLog log = new MergedEventLog();
        log.merge(shareEvents);

        // events for watched items
        filter.ops.clear();
        Multimap<String,Integer> watchCache = WatchCache.get(octxt).getMap();
        for (String ownerAccountId : watchCache.keySet()) {
            ItemEventLog watchLog = logger.getLog(octxt.getAuthenticatedUser().getId(), ownerAccountId, watchCache.get(ownerAccountId), filter);
            log.merge(watchLog);
        }
        return log;
    }
}
