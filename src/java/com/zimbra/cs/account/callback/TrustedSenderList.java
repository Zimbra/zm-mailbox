/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.callback;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;

/**
 * {@link AttributeCallback} for {@code zimbraPrefMailTrustedSenderList}.
 * <p>
 * This callback only intercepts pre-modify.
 * <ul>
 *  <li>Throw {@link AccountServiceException#TOO_MANY_TRUSTED_SENDERS} if the
 *  number of {@code zimbraPrefMailTrustedSenderList} entries is exceeding
 *  {@code zimbraMailTrustedSenderListMaxNumEntries} by the modify attempt.
 *  <li>Copy CoS values if the modify attempt is newly creating
 *  {@code zimbraPrefMailTrustedSenderList} entries at the account level.
 * </ul>
 * {@code zimbraPrefMailTrustedSenderList} is included in the {@code prefs}
 * section of {@code GetInfoResponse}, so that client can cache all values upon
 * login. Having it in the {@code prefs} section might not be optimal because
 * the lengthy {@code name="zimbraPrefMailTrustedSenderList"} is repeated many
 * times. If we find it a real performance hit, we should move it to a separate
 * section in {@code GetInfoResponse}. As a drawback, we have to provide a
 * separate SOAP API for modify because {@code ModifyPrefsRequest} can only
 * handle stuff in the {@code prefs} section.
 * <p>
 * TODO: If all entries at the account level are removed, it falls back to CoS
 * again, which is not desirable. Need to find a way to represent empty without
 * falling back to CoS.
 *
 * @author ysasaki
 */
public final class TrustedSenderList extends AttributeCallback {

    @SuppressWarnings("unchecked")
    @Override
    public void preModify(CallbackContext context, String name,
            Object value, @SuppressWarnings("rawtypes") Map mod, Entry entry) 
    throws ServiceException {

        if (context.isCreate() || !(entry instanceof Account)) {
            return;
        }
        
        // This is called for each of name, +name and -name.
        // Skip if already processed.
        if (context.isDoneAndSetIfNot(TrustedSenderList.class)) {
            return;
        }

        Account account = (Account) entry;

        int max = account.getMailTrustedSenderListMaxNumEntries();

        Object replace = mod.get(name);
        Object add = mod.get("+" + name);
        Object remove = mod.get("-" + name);

        if (replace != null) {
            Set<String> set = getMultiValueSet(replace);
            if (set.size() > max) {
                throw AccountServiceException.TOO_MANY_TRUSTED_SENDERS(
                        set.size() + " > " + max);
            }
        } else {
            String[] current = account.getMultiAttr(name, false);

            if (add != null) {
                Set<String> set = getMultiValueSet(add);
                if (current.length > 0) {
                    if (current.length + set.size() > max) {
                        throw AccountServiceException.TOO_MANY_TRUSTED_SENDERS(
                                current.length + " + " +  set.size() + " > " + max);
                    }
                } else { // copy CoS values
                    List<String> def = getMultiValue(account.getAttrDefault(name));
                    if (def.size() + set.size() > max) {
                        throw AccountServiceException.TOO_MANY_TRUSTED_SENDERS(
                                def.size() + " + " +  set.size() + " > " + max);
                    }
                    set.addAll(def);
                    mod.put("+" + name, set);
                }
            }

            if (remove != null) {
                if (current.length == 0) { // copy CoS values
                    Set<String> def;
                    if (add == null) {
                        def = getMultiValueSet(account.getAttrDefault(name));
                    } else { // honor the result from "add"
                        def = (Set<String>) mod.get("+" + name);
                        assert(def != null);
                    }
                    def.removeAll(getMultiValueSet(remove));
                    mod.remove("-" + name);
                    mod.put("+" + name, def);
                }
            }
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }

}
