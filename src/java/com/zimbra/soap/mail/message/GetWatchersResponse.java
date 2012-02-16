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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.OctopusXmlConstants;
import com.zimbra.soap.mail.type.WatcherInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=OctopusXmlConstants.E_GET_WATCHERS_RESPONSE)
public class GetWatchersResponse {

    /**
     * @zm-api-field-description Information on items being watched by users
     */
    @XmlElement(name=MailConstants.E_WATCHER /* watcher */, required=false)
    private List<WatcherInfo> watchers = Lists.newArrayList();

    public GetWatchersResponse() {
    }

    public void setWatchers(Iterable <WatcherInfo> watchers) {
        this.watchers.clear();
        if (watchers != null) {
            Iterables.addAll(this.watchers,watchers);
        }
    }

    public void addWatcher(WatcherInfo watcher) {
        this.watchers.add(watcher);
    }

    public List<WatcherInfo> getWatchers() {
        return Collections.unmodifiableList(watchers);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("watchers", watchers);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
