/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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
package com.zimbra.cs.store;
import com.zimbra.common.util.ZimbraLog;

import java.util.Set;

public abstract class StoreManagerRegistrar {

    private static Set<String> smList;

    public static void registerStoreManager(Class<? extends StoreManager> smTypeClass) {
        String smName = smTypeClass.getClass().getSimpleName();
        smList.add(smName);
        System.out.print("Register store manager : " + smName);
        ZimbraLog.misc.info("Register store manager : " + smName);
    }

    public static Set<String> getSMList() {
        return smList;
    }
}
