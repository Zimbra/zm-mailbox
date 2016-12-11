/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.base;

import java.util.List;

import com.zimbra.soap.type.Id;
import com.zimbra.soap.type.WaitSetAddSpec;

public interface WaitSetReq {
    public void setBlock(Boolean block);
    public void setDefaultInterests(String defaultInterests);
    public void setTimeout(Long timeout);
    public void setAddAccounts(Iterable <WaitSetAddSpec> addAccounts);
    public WaitSetReq addAddAccount(WaitSetAddSpec addAccount);
    public void setUpdateAccounts(Iterable <WaitSetAddSpec> updateAccounts);
    public WaitSetReq addUpdateAccount(WaitSetAddSpec updateAccount);
    public void setRemoveAccounts(Iterable <Id> removeAccounts);
    public WaitSetReq addRemoveAccount(Id removeAccount);
    public String getWaitSetId();
    public String getLastKnownSeqNo();
    public Boolean getBlock();
    public String getDefaultInterests();
    public Long getTimeout();
    public List<WaitSetAddSpec> getAddAccounts();
    public List<WaitSetAddSpec> getUpdateAccounts();
    public List<Id> getRemoveAccounts();
}
