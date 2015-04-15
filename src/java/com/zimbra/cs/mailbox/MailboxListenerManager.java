/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
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
package com.zimbra.cs.mailbox;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeansException;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.datasource.DataSourceListener;
import com.zimbra.cs.fb.FreeBusyProvider;
import com.zimbra.cs.filter.FilterListener;
import com.zimbra.cs.mailbox.MailboxListener.ChangeNotification;
import com.zimbra.cs.mailbox.acl.AclPushListener;
import com.zimbra.cs.mailbox.acl.ShareExpirationListener;
import com.zimbra.cs.mailbox.alerts.CalItemReminderService;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraApplication;
import com.zimbra.cs.util.ZimbraConfig;


public class MailboxListenerManager {
    protected HashSet<MailboxListener> listeners = new HashSet<>();
    protected static MailboxListenerManager singleton;

    public static MailboxListenerManager getInstance() {
        if (singleton == null) {
            singleton = Zimbra.getAppContext().getBean(MailboxListenerManager.class);
        }
        return singleton;
    }

    public MailboxListenerManager() {
        reset();
    }

    @VisibleForTesting
    public void reset() {
        listeners.clear();
        ZimbraApplication application = ZimbraApplication.getInstance();
        if (application.supports(CalItemReminderService.class) && !DebugConfig.disableCalendarReminderEmail) {
            register(new CalItemReminderService());
        }
        register(new FilterListener());
        register(new CacheManager());
        register(new FreeBusyProvider.Listener());
        register(new DataSourceListener());
        register(new ShareStartStopListener());
        if (application.supports(AclPushListener.class)) {
            register(new AclPushListener());
        }
        if (application.supports(ShareExpirationListener.class) && !DebugConfig.isDisableShareExpirationListener()) {
            register(new ShareExpirationListener());
        }

        // Register external listeners configured by the zimbraMailboxListenerUrl attribute
        try {
            ZimbraConfig config = Zimbra.getAppContext().getBean(ZimbraConfig.class);
            List<MailboxListenerTransport> managers = config.externalMailboxListeners();
            managers = managers == null ? Collections.emptyList() : managers;
            for (MailboxListenerTransport manager: managers) {
                MailboxListener listener = new MailboxListener() {
                    @Override
                    public Set<MailItem.Type> notifyForItemTypes() {
                        return MailboxListener.ALL_ITEM_TYPES;
                    }

                    @Override
                    public void notify(ChangeNotification notification) {
                        try {
                            manager.publish(notification);
                        } catch (ServiceException e) {
                            ZimbraLog.session.warn("Failed publishing ChangeNotification to external mailbox listener", e);
                        }
                    }
                };
                register(listener);
            }
        } catch (Exception e) {
            ZimbraLog.session.warn("Failed reading external mailbox listener configuration", e);
        }
    }

    public void register(MailboxListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @VisibleForTesting
    public void unregister(MailboxListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public void notifyListeners(ChangeNotification notification) throws BeansException, ServiceException {
        for (MailboxListener listener: listeners) {
            if (!Collections.disjoint(notification.mods.changedTypes, listener.notifyForItemTypes())) {
                listener.notify(notification);
            }
        }
    }
}
