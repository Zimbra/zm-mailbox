/*
 * ***** BEGIN LICENSE BLOCK ***** Zimbra Collaboration Suite Server Copyright
 * (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>. *****
 * END LICENSE BLOCK *****
 */

package com.zimbra.soap.type;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.service.ServiceException;

@XmlType
@XmlEnum(String.class)
public enum NewFileCreationTypes {
    @XmlEnumValue("document") document,
    @XmlEnumValue("presentation") presentation,
    @XmlEnumValue("spreadsheet") spreadsheet;
    
    public static NewFileCreationTypes fromString(String value) throws ServiceException {
        if (value == null) {
            return null;
        }
        try {
            return NewFileCreationTypes.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw ServiceException.INVALID_REQUEST(
                    "Invalid value: " + value + ", valid values: " + Arrays.asList(NewFileCreationTypes.values()), null);
        }
    }
}
