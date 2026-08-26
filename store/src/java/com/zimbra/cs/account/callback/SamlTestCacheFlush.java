/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.soap.admin.type.CacheEntryType;
import java.util.Map;

public class SamlTestCacheFlush extends AttributeCallback {

    @Override
    public void preModify(CallbackContext ctx, String name, Object val,
            Map mods, Entry entry) {
        // no-op
    }

    @Override
    public void postModify(CallbackContext ctx, String attrName, Entry entry) {
        if (ctx.isDoneAndSetIfNot(SamlTestCacheFlush.class)) {
            return;
        }  // once per modify batch
        if (entry == null) {
            return;
        }
        try {
            final CacheEntryType type;
            final Provisioning.CacheEntry[] entries;
            if (entry instanceof Domain) {
                type = CacheEntryType.domain;
                entries = new Provisioning.CacheEntry[]{
                        new Provisioning.CacheEntry(Key.CacheEntryBy.id,
                                ((Domain) entry).getId())
                };
            } else if (entry instanceof Config) {
                type = CacheEntryType.config; // global config takes no entry selector
                entries = null;
            } else {
                return; // attrs only live on domain/globalConfig
            }
            SoapProvisioning sp = SoapProvisioning.getAdminInstance();
            sp.flushCache(type.name(), entries, true /* allServers */, false /* imapDaemons */);
        } catch (ServiceException e) {
            ZimbraLog.account.warn("SAML test: failed to flush %s cache across servers",
                    entry.getLabel(), e);
        }
    }
}
