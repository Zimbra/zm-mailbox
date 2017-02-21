/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.type;

import java.util.List;
import java.util.Collection;
import java.util.Collections;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import com.google.common.collect.Lists;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ComboRights {

    /**
     * @zm-api-field-description Rights information
     */
    @XmlElement(name=AdminConstants.E_R, required=false)
    private List <ComboRightInfo> comboRights = Lists.newArrayList();

    public ComboRights () {
    }

    public ComboRights (Collection <ComboRightInfo> comboRights) {
        this.setComboRights(comboRights);
    }

    public ComboRights setComboRights(Collection<ComboRightInfo> comboRights) {
        this.comboRights.clear();
        if (comboRights != null) {
            this.comboRights.addAll(comboRights);
        }
        return this;
    }

    public ComboRights addComboRight(ComboRightInfo comboRight) {
        comboRights.add(comboRight);
        return this;
    }

    public List <ComboRightInfo> getComboRights() {
        return Collections.unmodifiableList(comboRights);
    }
}
