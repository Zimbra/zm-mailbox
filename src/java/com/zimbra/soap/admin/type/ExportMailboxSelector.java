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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExportMailboxSelector {

    // Account must exist and be provisioned on the local server
    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String name;

    // Must differ from the account's host server
    @XmlAttribute(name=AdminConstants.A_DEST, required=true)
    private final String target;

    @XmlAttribute(name="destPort", required=false)
    private Integer destPort;

    @XmlAttribute(name="overwrite", required=false)
    private Boolean overwrite;

    @XmlAttribute(name="tempDir", required=false)
    private String tempDir;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ExportMailboxSelector() {
        this((String) null, (String) null);
    }

    public ExportMailboxSelector(String name, String target) {
        this.name = name;
        this.target = target;
    }

    public void setDestPort(Integer destPort) { this.destPort = destPort; }
    public void setOverwrite(Boolean overwrite) { this.overwrite = overwrite; }
    public void setTempDir(String tempDir) { this.tempDir = tempDir; }
    public String getName() { return name; }
    public String getTarget() { return target; }
    public Integer getDestPort() { return destPort; }
    public Boolean getOverwrite() { return overwrite; }
    public String getTempDir() { return tempDir; }
}
