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

package com.zimbra.soap.mail.type;

import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.util.StringUtil;

import com.zimbra.soap.type.KeyValuePair;
import com.zimbra.soap.type.KeyValuePairs;
import com.zimbra.soap.type.KeyValuePairsBase;

/*
 * Used for JAXB objects representing elements which have child node(s) of form:
 *     <a n="{key}">{value}</a>
 */
@XmlAccessorType(XmlAccessType.NONE)
public class MailKeyValuePairs extends KeyValuePairsBase {

    public MailKeyValuePairs() {
        super();
    }

    public MailKeyValuePairs(Iterable<KeyValuePair> keyValuePairs) {
        super(keyValuePairs);
    }

    public MailKeyValuePairs (Map<String, ? extends Object> keyValuePairs)
    throws ServiceException {
        super(keyValuePairs);
    }

    @XmlElement(name=AdminConstants.E_A)
    public void setKeyValuePairs(
                    List<KeyValuePair> keyValuePairs) {
        super.setKeyValuePairs(keyValuePairs);
    }

    public List<KeyValuePair> getKeyValuePairs() {
        return super.getKeyValuePairs();
    }
}
