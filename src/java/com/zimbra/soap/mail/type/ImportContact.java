/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.type;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ImportContact {

    private List <String> listOfCreatedIds = new ArrayList<String>();

    /**
     * @zm-api-field-tag num-imported
     * @zm-api-field-description Number imported
     */
    @XmlAttribute(name=MailConstants.A_NUM /* n */, required=false)
    private long numImported;

    public ImportContact() {
    }

    private static Splitter COMMA_SPLITTER = Splitter.on(",");
    private static Joiner COMMA_JOINER = Joiner.on(",");

    /**
     * @zm-api-field-tag comma-sep-created-ids
     * @zm-api-field-description Comma-separated list of created IDs
     */
    @XmlAttribute(name=MailConstants.A_IDS /* ids */, required=true)
    public String getListOfCreatedIds() {
        return COMMA_JOINER.join(listOfCreatedIds);
    }

    public void setListOfCreatedIds(String commaSepIds) {
        for (String id : COMMA_SPLITTER.split(commaSepIds)) {
            addCreatedId(id);
        }
    }

    public void addCreatedId(String id) {
        listOfCreatedIds.add(id);
    }

    public long getNumImported() {
        return numImported;
    }

    public void setNumImported(long numImported) {
        this.numImported = numImported;
    }
}
