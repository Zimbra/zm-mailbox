/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.iochannel;

import static com.zimbra.common.util.TaskUtil.newDaemonThreadFactory;
import static java.util.concurrent.Executors.newCachedThreadPool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import com.zimbra.common.mailbox.MailboxStore;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;

public class MailboxNotification extends Message {

    public static final String AppId = "mbn";

    private static final ExecutorService executor = newCachedThreadPool(newDaemonThreadFactory("MailboxNotification"));

    public static MailboxNotification create(String accountId, int changeId, byte[] data) throws MessageChannelException {
        return new MailboxNotification(accountId, changeId, data);
    }

    @Override
    protected int size() {
        // 4 byte int padding for length of each strings.
        return accountId.length() + 4 + payload.length + 8;
    }


    @Override
    protected void serialize(ByteBuffer buffer) throws IOException {
        writeString(buffer, accountId);
        buffer.putInt(changeId);
        writeBytes(buffer, payload);
        //String base64Str = Base64.encodeBase64String(payload);
        //writeString(buffer, base64Str);
    }

    @Override
    protected Message construct(ByteBuffer buffer) throws IOException {
        return new MailboxNotification(buffer);
    }

    @Override
    public String getAppId() {
        return AppId;
    }

    @Override
    public String getRecipientAccountId() {
        return accountId;
    }

    public byte[] getPayload() {
        return payload;
    }

    public int getChangeId() {
        return changeId;
    }

    MailboxNotification() {
    }

    public MailboxNotification(ByteBuffer buffer) throws IOException {
        super();
        accountId = readString(buffer);
        changeId = buffer.getInt();
        payload = readBytes(buffer);
        //String payloadStr = readString(buffer);
        //payload = Base64.decodeBase64(payloadStr);
    }

    protected void writeBytes(ByteBuffer buffer, byte[] data) throws IOException {
        buffer.putInt(data.length);
        buffer.put(data);
    }

    protected byte[] readBytes(ByteBuffer buffer) throws IOException {
        int len = buffer.getInt();
        ByteBuffer sub = buffer.slice();
        sub.limit(len);
        buffer.position(buffer.position() + len);
        byte[] data = new byte[len];
        sub.get(data);
        return data;
    }

    private MailboxNotification(String aid, int cid, byte[] ntfn) {
        super();
        accountId = aid;
        changeId = cid;
        payload = ntfn;
    }

    private void handleMailboxNotification(final MailboxNotification message) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                Collection<Session> sessions = SessionCache.getAllSessions(message.getRecipientAccountId());
                if (sessions == null || sessions.isEmpty()) {
                    log.warn("no active sessions for account %s", message.getRecipientAccountId());
                    return;
                }

                PendingModifications pms = null;
                for (Session session : sessions) {
                    log.debug("notifying session %s", session.toString());
                    if (pms == null) {
                        try {
                            MailboxStore mboxStore = session.getMailbox();
                            if ((null == mboxStore) || (mboxStore instanceof Mailbox)) {
                                pms = PendingModifications.deserialize((Mailbox)mboxStore, message.getPayload());
                            } else {
                                log.warn("could not deserialize notification for non-Mailbox MailboxStore '%s'",
                                        mboxStore.getClass().getName());
                            }
                        } catch (IOException e) {
                            log.warn("could not deserialize notification", e);
                            return;
                        } catch (ClassNotFoundException e) {
                            log.warn("could not deserialize notification", e);
                            return;
                        } catch (ServiceException e) {
                            log.warn("could not deserialize notification", e);
                            return;
                        }
                    }
                    session.notifyPendingChanges(pms, message.getChangeId(), null);
                }
            }
        });
    }

    @Override
    public MessageHandler getHandler() {
        return new MessageHandler() {
            @Override
            public void handle(Message m, String clientId) {
                if (!(m instanceof MailboxNotification)) {
                    return;
                }
                handleMailboxNotification((MailboxNotification) m);
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(AppId).append(":");
        buf.append(accountId).append(":");
        buf.append(changeId).append(":");
        buf.append(payload);
        return buf.toString();
    }

    private String accountId;
    private int changeId;
    private byte[] payload;
}
