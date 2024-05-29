/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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
package com.zimbra.cs.service.util;

import java.util.LinkedList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.admin.type.VolumeInfo;

public class VolumeQualifier {

    List<Qualifier> qualifierlist = new LinkedList<>();
    private static VolumeQualifier volumeQualifier = new VolumeQualifier();

    private VolumeQualifier() {}

    public static VolumeQualifier getInstance() {
        return volumeQualifier;
    }

    public void register(Qualifier volQualifier) {
        qualifierlist.add(volQualifier);
    }

    public boolean qualify(VolumeInfo volInfo) throws ServiceException {
        for(Qualifier qualifier: qualifierlist){
            if (!qualifier.qualify(volInfo)) {
                return false;
            }
         }
        return true;
    } 

    public interface Qualifier {
        public boolean qualify(VolumeInfo volInfo) throws ServiceException;
    }
}
