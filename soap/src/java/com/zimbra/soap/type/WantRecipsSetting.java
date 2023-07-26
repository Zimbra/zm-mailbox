/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.type;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.zimbra.common.gql.GqlConstants;

import io.leangen.graphql.annotations.types.GraphQLType;

@XmlEnum
@GraphQLType(name=GqlConstants.CLASS_INCLUDE_RECIPS_SETTING)
public enum WantRecipsSetting {
    @XmlEnumValue("0") PUT_SENDERS,
    @XmlEnumValue("1") PUT_RECIPIENTS,
    @XmlEnumValue("2") PUT_BOTH,
    @XmlEnumValue("3") PUT_ALL,
    @Deprecated @XmlEnumValue("false") LEGACY_PUT_SENDERS,
    @Deprecated @XmlEnumValue("true") LEGACY_PUT_RECIPS;

    /**
     * @return sanitized (i.e. non-legacy) value
     */
    public static WantRecipsSetting usefulValue(WantRecipsSetting setting) {
        if (setting == null) {
            return PUT_SENDERS;
        }
        if (WantRecipsSetting.LEGACY_PUT_SENDERS.equals(setting)) {
            return PUT_SENDERS;
        } else if (WantRecipsSetting.LEGACY_PUT_RECIPS.equals(setting)) {
            return PUT_RECIPIENTS;
        } else if (WantRecipsSetting.PUT_ALL.equals(setting)) {
            return PUT_ALL;
        }
        return setting;
    }
}