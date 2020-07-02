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

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class ExportMailboxSelector {

    /**
     * @zm-api-field-tag account-email-address
     * @zm-api-field-description Account email address.
     * <br />
     * Account must exist and be provisioned on the local server
     */
    @XmlAttribute(name=BackupConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag hostname-of-target-server
     * @zm-api-field-description Hostname of target server.
     * <br />
     * Must differ from the account's host server
     */
    @XmlAttribute(name=BackupConstants.A_TARGET /* dest */, required=true)
    private final String target;

    /**
     * @zm-api-field-tag target-port-for-import
     * @zm-api-field-description Target port for mailbox import
     */
    @XmlAttribute(name=BackupConstants.A_PORT /* destPort */, required=false)
    private Integer destPort;

    /**
     * @zm-api-field-tag source-svr-tempdir
     * @zm-api-field-description Temporary directory to use on source server
     */
    @XmlAttribute(name=BackupConstants.A_TEMP_DIR /* tempDir */, required=false)
    private String tempDir;

    /**
     * @zm-api-field-tag overwrite-flag
     * @zm-api-field-description If this flag is set, the target mailbox will be replaced if it exists
     */
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("target", target)
            .add("destPort", destPort)
            .add("tempDir", tempDir)
            .add("overwrite", overwrite);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
