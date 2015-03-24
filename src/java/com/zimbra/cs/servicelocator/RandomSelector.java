/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.cs.servicelocator;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.servicelocator.ServiceLocator.Entry;

public class RandomSelector implements Selector {
    public Entry selectOne(List<Entry> list) throws IOException, ServiceException {
        if (list.isEmpty()) {
            return null;
        } else if (list.size() == 1) {
            return list.get(0);
        } else {
            int index = new Random().nextInt(list.size() - 1);
            return list.get(index);
        }
    }
}