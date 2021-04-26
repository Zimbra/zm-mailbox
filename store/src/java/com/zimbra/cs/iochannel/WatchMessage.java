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
package com.zimbra.cs.iochannel;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.cache.WatchCache;
import com.zimbra.cs.service.util.ItemId;

public class WatchMessage extends Message {

    private static final String AppId = "watch";

    private String accountId;
    private Op op;
    private ItemId watchItemId;

    private enum Op {
        watch, unwatch
    }

    public WatchMessage() {
    }

    public WatchMessage(String accountId, Op op, String aid, int iid) {
        super();
        this.accountId = accountId;
        this.op = op;
        watchItemId = new ItemId(aid, iid);
    }

    private WatchMessage(ByteBuffer in) throws IOException {
        super();
        accountId = readString(in);
        op = Op.valueOf(readString(in));
        String aid = readString(in);
        String iid = readString(in);
        watchItemId = new ItemId(aid, Integer.parseInt(iid));
    }

    public static WatchMessage watch(String accountId, String watchAccountId, int itemId) {
        return new WatchMessage(accountId, Op.watch, watchAccountId, itemId);
    }

    public static WatchMessage unwatch(String accountId, String watchAccountId, int itemId) {
        return new WatchMessage(accountId, Op.unwatch, watchAccountId, itemId);
    }

    @Override
    public String getAppId() {
        return AppId;
    }

    @Override
    public String getRecipientAccountId() {
        return accountId;
    }

    @Override
    protected int size() {
        return (accountId.length() + watchItemId.toString().length() + op.name().length()) * 2 + 16;
    }

    @Override
    protected void serialize(ByteBuffer out) throws IOException {
        writeString(out, accountId);
        writeString(out, op.name());
        writeString(out, watchItemId.getAccountId());
        writeString(out, Integer.toString(watchItemId.getId()));
    }

    @Override
    protected Message construct(ByteBuffer in) throws IOException {
        return new WatchMessage(in);
    }

    @Override
    public MessageHandler getHandler() {
        return new MessageHandler() {
            @Override
            public void handle(Message m, String clientId) {
                if (!(m instanceof WatchMessage)) {
                    return;
                }
                try {
                    WatchMessage message = (WatchMessage)m;
                    Account account = Provisioning.getInstance().getAccountById(accountId);
                    WatchCache watchCache = WatchCache.get(account);
                    switch (message.op) {
                    case watch:
                        log.debug("watching item " + watchItemId);
                        watchCache.watch(watchItemId.getAccountId(), watchItemId.getId());
                        break;
                    case unwatch:
                        log.debug("unwatching item " + watchItemId);
                        watchCache.unwatch(watchItemId.getAccountId(), watchItemId.getId());
                        break;
                    }
                } catch (ServiceException e) {
                    log.warn("can't change watch mapping", e);
                    return;
                }
            }
        };
    }
}
