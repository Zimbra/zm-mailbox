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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ZimletStatusParent {

    /**
     * @zm-api-field-description Status information
     */
    @XmlElement(name=AdminConstants.E_ZIMLET /* zimlet */, required=false)
    private List<ZimletStatus> zimlets = Lists.newArrayList();

    public ZimletStatusParent() {
    }

    public void setZimlets(Iterable <ZimletStatus> zimlets) {
        this.zimlets.clear();
        if (zimlets != null) {
            Iterables.addAll(this.zimlets,zimlets);
        }
    }

    public ZimletStatusParent addZimlet(ZimletStatus zimlet) {
        this.zimlets.add(zimlet);
        return this;
    }

    public List<ZimletStatus> getZimlets() {
        return Collections.unmodifiableList(zimlets);
    }
}
