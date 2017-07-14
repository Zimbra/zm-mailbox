/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account.callback;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.cs.account.AttributeCallback;


public class CallbackContext {

    public enum Op {
        CREATE,
        MODIFY;
    };

    public enum DataKey {
        MAX_SIGNATURE_LEN,
        MAIL_FORWARDING_ADDRESS_MAX_LEN,
        MAIL_FORWARDING_ADDRESS_MAX_NUM_ADDRS,
        MAIL_WHITELIST_MAX_NUM_ENTRIES,
        MAIL_BLACKLIST_MAX_NUM_ENTRIES,
        PREV_EPHEMERAL_BACKEND_URL;
    };

    // whether the entry is being created
    private final Op op;

    // named of the entry being created
    private String creatingEntryName;

    // set of AttributeCallback marked themselves done
    private Set<Class<? extends AttributeCallback>> done = Sets.newHashSet();

    // data map
    private Map<DataKey, String> dataMap = Maps.newHashMap();

    public CallbackContext(Op op) {
        this.op = op;
    }

    public boolean isCreate() {
        return op == Op.CREATE;
    }

    public void setCreatingEntryName(String name) {
        creatingEntryName = name;
    }

    // name of the entry being creating
    public String getCreatingEntryName() {
        return creatingEntryName;
    }

    private void setDone(Class<? extends AttributeCallback> callback) {
        done.add(callback);
    }

    public boolean isDoneAndSetIfNot(Class<? extends AttributeCallback> callback) {
        boolean isDone = done.contains(callback);
        if (!isDone) {
            setDone(callback);
        }
        return isDone;
    }

    public void setData(DataKey key, String value) {
        dataMap.put(key, value);
    }

    public String getData(DataKey key) {
        return dataMap.get(key);
    }

}
