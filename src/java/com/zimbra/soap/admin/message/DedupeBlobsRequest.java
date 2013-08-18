/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.message.AutoProvTaskControlRequest.Action;
import com.zimbra.soap.admin.type.IntIdAttr;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Dedupe the blobs having the same digest.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_DEDUPE_BLOBS_REQUEST)
public class DedupeBlobsRequest {

    @XmlEnum
    public static enum DedupAction {
        start,
        status,
        stop,
        reset;
        
        public static Action fromString(String action) throws ServiceException {
            try {
                return Action.valueOf(action);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown action: " + action, e);
            }
        }
    }

    /**
     * @zm-api-field-description Action to perform - one of <b>start|status|stop</b>
     */
    @XmlAttribute(name=AdminConstants.E_ACTION, required=true)
    private final DedupAction action;

    // ShortIdAttr would be a more accurate fit
    /**
     * @zm-api-field-description Volumes
     */
    @XmlElement(name=AdminConstants.E_VOLUME /* volume */, required=false)
    private List<IntIdAttr> volumes = Lists.newArrayList();

    public void setVolumes(Iterable <IntIdAttr> volumes) {
        this.volumes.clear();
        if (volumes != null) {
            Iterables.addAll(this.volumes, volumes);
        }
    }
    
    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DedupeBlobsRequest() {
        this((DedupAction)null);
    }
    
    public DedupeBlobsRequest(DedupAction action) {
        this.action = action;
    }

    public DedupAction getAction() {
        return action;
    }

    public DedupeBlobsRequest addVolume(IntIdAttr volume) {
        this.volumes.add(volume);
        return this;
    }

    public List<IntIdAttr> getVolumes() {
        return Collections.unmodifiableList(volumes);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("action", action)
            .add("volumes", volumes);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
