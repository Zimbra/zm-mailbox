/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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

package com.zimbra.cs.mailbox.cache;

import com.google.common.base.MoreObjects;

public abstract class CachedObject<V> {

    protected String objectName;

    public CachedObject(String objectName) {
        this.objectName = objectName;
    }

    public String getObjectName() {
        return objectName;
    }

    public abstract V getObject();

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("obj", objectName).toString();
    }
}