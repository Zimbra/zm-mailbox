/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
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

package com.zimbra.cs.mailbox.calendar.cache;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.mailbox.Mailbox;


public interface CtagInfoCache {

    CtagInfo get(String accountId, int folderId) throws ServiceException;

    Map<Pair<String,Integer>, CtagInfo> get(List<Pair<String,Integer>> keys) throws ServiceException;

    void put(String accountId, int folderId, CtagInfo ctagInfo) throws ServiceException;

    void put(Map<Pair<String,Integer>, CtagInfo> map) throws ServiceException;

    void remove(String accountId, int folderId) throws ServiceException;

    void remove(List<Pair<String, Integer>> keys) throws ServiceException;

    void remove(Mailbox mbox) throws ServiceException;
}
