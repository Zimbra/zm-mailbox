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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.BrowseData;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_BROWSE_RESPONSE)
public class BrowseResponse {

    /**
     * @zm-api-field-description Browse data
     */
    @XmlElement(name=MailConstants.E_BROWSE_DATA, required=false)
    private List<BrowseData> browseDatas = Lists.newArrayList();

    public BrowseResponse() {
    }

    public void setBrowseDatas(Iterable <BrowseData> browseDatas) {
        this.browseDatas.clear();
        if (browseDatas != null) {
            Iterables.addAll(this.browseDatas,browseDatas);
        }
    }

    public BrowseResponse addBrowseData(BrowseData browseData) {
        this.browseDatas.add(browseData);
        return this;
    }

    public List<BrowseData> getBrowseDatas() {
        return Collections.unmodifiableList(browseDatas);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("browseDatas", browseDatas)
            .toString();
    }
}
