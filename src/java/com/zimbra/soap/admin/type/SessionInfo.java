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
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class SessionInfo {

    /**
     * @zm-api-field-tag account-id
     * @zm-api-field-description Account ID
     */
    @XmlAttribute(name=AdminConstants.A_ZIMBRA_ID /* zid */, required=false)
    private final String zimbraId;

    /**
     * @zm-api-field-tag account-name
     * @zm-api-field-description Account name
     */
    @XmlAttribute(name=AdminConstants.A_NAME /* name */, required=false)
    private final String name;

    /**
     * @zm-api-field-tag session-id
     * @zm-api-field-description Session ID
     */
    @XmlAttribute(name=AdminConstants.A_SESSION_ID /* sid */, required=true)
    private final String sessionId;

    /**
     * @zm-api-field-tag creation-date
     * @zm-api-field-description Creation date
     */
    @XmlAttribute(name=AdminConstants.A_CREATED_DATE /* cd */, required=true)
    private final long createdDate;

    /**
     * @zm-api-field-tag last-accessed-date
     * @zm-api-field-description Last accessed date
     */
    @XmlAttribute(name=AdminConstants.A_LAST_ACCESSED_DATE /* ld */, required=true)
    private final long lastAccessedDate;

    // Overrides of Server-side encodedState MAY add elements or attributes.
    // Session.doEncodeState does nothing but
    // SoapSession.doEncodeState adds an optional Boolean attribute "push"
    // ImapSession.doEncodeState adds an "imap" element which is
    // populated by PagedFolderData.doEncodeState or ImapFolder.doEncodeState

    /**
     * @zm-api-field-description Extra attributes - possibly including "push"
     */
    @XmlAnyAttribute
    private Map<QName,Object> extraAttributes = Maps.newHashMap();

    /**
     * @zm-api-field-description Extra elements - possibly including an "imap" element
     */
    @XmlAnyElement
    private List<org.w3c.dom.Element> extraElements = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SessionInfo() {
        this((String) null, (String) null, (String) null, -1L, -1L);
    }

    public SessionInfo(String zimbraId, String name, String sessionId,
                    long createdDate, long lastAccessedDate) {
        this.zimbraId = zimbraId;
        this.name = name;
        this.sessionId = sessionId;
        this.createdDate = createdDate;
        this.lastAccessedDate = lastAccessedDate;
    }

    public void setExtraAttributes(Map<QName,Object> extraAttributes) {
        this.extraAttributes.clear();
        if (extraAttributes != null) {
            this.extraAttributes.putAll(extraAttributes);
        }
    }

    public void addExtraAttribute(QName qn, Object value) {
        if ((qn != null) && (value != null)) {
            this.extraAttributes.put(qn, value);
        }
    }

    public void setExtraElements(Iterable <org.w3c.dom.Element> extraElements) {
        this.extraElements.clear();
        if (extraElements != null) {
            Iterables.addAll(this.extraElements,extraElements);
        }
    }

    public SessionInfo addExtraElement(org.w3c.dom.Element extraElement) {
        this.extraElements.add(extraElement);
        return this;
    }

    public String getZimbraId() { return zimbraId; }
    public String getName() { return name; }
    public String getSessionId() { return sessionId; }
    public long getCreatedDate() { return createdDate; }
    public long getLastAccessedDate() { return lastAccessedDate; }
    public Map<QName,Object> getExtraAttributes() { return extraAttributes; }
    public List<org.w3c.dom.Element> getExtraElements() {
        return Collections.unmodifiableList(extraElements);
    }
}
