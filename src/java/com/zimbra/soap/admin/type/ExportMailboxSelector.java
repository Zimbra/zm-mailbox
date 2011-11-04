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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
public class ExportMailboxSelector {

    // Account must exist and be provisioned on the local server
    @XmlAttribute(name=BackupConstants.A_NAME /* name */, required=true)
    private final String name;

    // Must differ from the account's host server
    @XmlAttribute(name=BackupConstants.A_TARGET /* dest */, required=true)
    private final String target;

    @XmlAttribute(name=BackupConstants.A_PORT /* destPort */, required=false)
    private Integer destPort;

    @XmlAttribute(name=BackupConstants.A_TEMP_DIR /* tempDir */, required=false)
    private String tempDir;

    @XmlAttribute(name=BackupConstants.A_OVERWRITE /* overwrite */, required=false)
    private ZmBoolean overwrite;

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
    public void setTempDir(String tempDir) { this.tempDir = tempDir; }
    public void setOverwrite(Boolean overwrite) { this.overwrite = ZmBoolean.fromBool(overwrite); }
    public String getName() { return name; }
    public String getTarget() { return target; }
    public Integer getDestPort() { return destPort; }
    public String getTempDir() { return tempDir; }
    public Boolean getOverwrite() { return ZmBoolean.toBool(overwrite); }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("target", target)
            .add("destPort", destPort)
            .add("tempDir", tempDir)
            .add("overwrite", overwrite);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
