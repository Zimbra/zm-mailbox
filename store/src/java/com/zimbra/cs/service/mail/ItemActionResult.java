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

public class ItemActionResult {

    protected List<String> mSuccessIds;

    public ItemActionResult() {
        mSuccessIds = new ArrayList<String>();
    }

    public ItemActionResult(int[] ids) {
        mSuccessIds = new ArrayList<String>(ids.length);
        for (int id : ids) {
            mSuccessIds.add(Integer.toString(id));
        }
    }

    public ItemActionResult(List<Integer> ids) {
        mSuccessIds = new ArrayList<String>(ids.size());
        for (Integer id : ids) {
            mSuccessIds.add(id.toString());
        }
    }

    public List<String> getSuccessIds() {
        return mSuccessIds;
    }

    public void setSuccessIds(List<String> ids) {
        mSuccessIds = ids;
    }

    public void appendSuccessIds(List<String> ids) {
        for (String id : ids) {
            appendSuccessId(id);
        }
    }

    public void appendSuccessId(String id) {
        mSuccessIds.add(id);
    }

}
