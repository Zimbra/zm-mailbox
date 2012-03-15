/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
