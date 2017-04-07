/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.List;

public class CopyActionResult extends ItemActionResult {

    protected List<String> mCreatedIds;

    public CopyActionResult() {
        super();
        mCreatedIds = new ArrayList<String>();
    }

    public CopyActionResult(List<String> ids, List<String> createdIds) {
        super();
        mCreatedIds = createdIds;
        setSuccessIds(ids);
    }

    public List<String> getCreatedIds() {
        return mCreatedIds;
    }

    public void setCreatedIds(List<String> mCreatedIds) {
        this.mCreatedIds = mCreatedIds;
    }

    public void appendCreatedIds(List<String> createdIds) {
        this.mCreatedIds.addAll(createdIds);
    }

}
