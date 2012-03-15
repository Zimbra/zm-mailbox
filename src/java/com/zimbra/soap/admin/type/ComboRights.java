/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
