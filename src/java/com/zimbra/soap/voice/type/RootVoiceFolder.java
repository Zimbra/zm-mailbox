/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.soap.voice.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class RootVoiceFolder extends VoiceFolder {

    /**
     * @zm-api-field-description Folders
     */
    @XmlElement(name=MailConstants.E_FOLDER /* folder */, required=false)
    private List<VoiceFolder> folders = Lists.newArrayList();

    public RootVoiceFolder() {
    }

    public void setFolders(Iterable <VoiceFolder> folders) {
        this.folders.clear();
        if (folders != null) {
            Iterables.addAll(this.folders, folders);
        }
    }

    public void addFolder(VoiceFolder folder) {
        this.folders.add(folder);
    }

    public List<VoiceFolder> getFolders() {
        return folders;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("folders", folders);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
