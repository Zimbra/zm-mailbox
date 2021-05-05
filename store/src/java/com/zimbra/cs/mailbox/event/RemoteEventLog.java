/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2021 Synacor, Inc.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.OctopusXmlConstants;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.mail.message.GetActivityStreamResponse;
import com.zimbra.soap.mail.type.ActivityInfo;
import com.zimbra.soap.mail.type.IdEmailName;
import com.zimbra.soap.type.NamedElement;
import com.zimbra.soap.type.NamedValue;

public class RemoteEventLog extends ItemEventLog {

    private static final int batchSize = 100;

    private final int itemId;
    private final Account authAccount;
    private final Account targetAccount;
    private final HashSet<MailboxOperation> operations;
    private final HashMap<String,String> users;
    private final ArrayList<MailboxEvent> activities;

    private int total;
    private String sessionId;
    private ZimbraAuthToken authToken;

    @Override
    public int getEventCount() throws ServiceException {
        return total;
    }

    @Override
    public Collection<MailboxEvent> getEvents(int offset, int count)
            throws ServiceException {
        while (activities.size() < offset + count) {
            int num = load();
            if (num == 0) {
                break;
            }
        }
        if (offset > activities.size()) {
            return Collections.emptyList();
        }
        int limit = offset + count;
        if (limit > activities.size()) {
            limit = activities.size();
        }
        return activities.subList(offset, limit);
    }

    @Override
    public Collection<MailboxEvent> getEventsBefore(long timestamp, int count)
            throws ServiceException {
        int end = activities.size() - 1;
        // check if we need to load additional events
        while (count < end && activities.get(end).getTimestamp() >= timestamp) {
            int num = load();
            if (num == 0) {
                break;
            }
        }
        int start = 0;
        int mid = (end - start) / 2;
        while (activities.get(mid).getTimestamp() != timestamp) {
            if (activities.get(mid).getTimestamp() > timestamp) {
                start = mid;
            } else {
                end = mid;
            }
            if ((end - start) < 3) {
                mid = start;
                break;
            }
            mid = start + (end - start) / 2;
        }
        // roll forward to the first item with timestamp - 1 or less
        while (mid < end && activities.get(mid).getTimestamp() >= timestamp) {
            mid++;
        }
        int limit = mid + count;
        if (limit > activities.size()) {
            limit = activities.size();
        }
        return activities.subList(mid, limit);
    }

    @Override
    public Collection<MailboxOperation> getLoggedOps() throws ServiceException {
        return operations;
    }

    @Override
    public Collection<String> getLoggedUsers() throws ServiceException {
        return users.values();
    }

    @Override
    public void addEvent(MailboxEvent event) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public RemoteEventLog(Account authAccount, Account targetAccount, int itemId) throws ServiceException {
        this.authAccount = authAccount;
        this.targetAccount = targetAccount;
        this.itemId = itemId;
        operations = new HashSet<MailboxOperation>();
        users = new HashMap<String,String>();
        activities = new ArrayList<MailboxEvent>();
        load();
    }

    private synchronized int load() throws ServiceException {
        int offset = activities.size();
        SoapHttpTransport transport = null;
        try {
            if (authToken == null) {
                authToken = new ZimbraAuthToken(authAccount);
            }
            String url = URLUtil.getSoapURL(targetAccount.getServer(), true);
            transport = new SoapHttpTransport(url);
            transport.setTargetAcctId(targetAccount.getId());
            transport.setAuthToken(authToken.toZAuthToken());
            transport.setTimeout(10000);
            transport.setResponseProtocol(SoapProtocol.Soap12);
            XMLElement req = new XMLElement(OctopusXmlConstants.GET_ACTIVITY_STREAM_REQUEST);
            req.addAttribute(MailConstants.A_ID, itemId);
            req.addAttribute(MailConstants.A_QUERY_OFFSET, offset);
            req.addAttribute(MailConstants.A_QUERY_LIMIT, batchSize);
            if (sessionId != null) {
                Element filter = req.addElement(MailConstants.E_FILTER);
                filter.addAttribute(MailConstants.A_SESSION, sessionId);
            }
            GetActivityStreamResponse resp = JaxbUtil.elementToJaxb(transport.invokeWithoutSession(req));
            sessionId = resp.getSession();
            total = Integer.parseInt(resp.getCount());
            for (NamedElement op : resp.getOperations()) {
                operations.add(MailboxOperation.valueOf(op.getName()));
            }
            for (IdEmailName user : resp.getUsers()) {
                users.put(user.getEmail(), user.getId());
            }
            for (ActivityInfo activity : resp.getActivities()) {
                ItemId iid = new ItemId(activity.getItemId(), targetAccount.getId());
                HashMap<String,String> arg = new HashMap<String,String>();
                for (NamedValue nv : activity.getArgs()) {
                    arg.put(nv.getName(), nv.getValue());
                }
                MailboxOperation op = MailboxOperation.valueOf(activity.getOperation());
                String accountId = users.get(activity.getEmail());
                int version = activity.getVersion() == null ? 0 : activity.getVersion();
                activities.add(
                    new MailboxEvent(
                        accountId,
                        op,
                        iid.getId(),
                        version,
                        0, // folderId
                        activity.getTimeStamp(),
                        null, // user agent
                        arg // arg
                        )
                    );
            }
            return resp.getActivities().size();
        } catch (IOException e) {
            throw ServiceException.PROXY_ERROR(e, targetAccount.getName());
        } finally {
            if (transport != null)
                transport.shutdown();
        }
    }
}
