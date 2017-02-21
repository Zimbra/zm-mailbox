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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.ExportAndDeleteMailboxSpec;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Exports the database data for the given items with SELECT INTO OUTFILE and deletes the
 * items from the mailbox.  Exported filenames follow the pattern {prefix}{table_name}.txt.  The files are written
 * to <b>sqlExportDir</b>.  When sqlExportDir is not specified, data is not exported.  Export is only supported for
 * MySQL.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_EXPORT_AND_DELETE_ITEMS_REQUEST)
public class ExportAndDeleteItemsRequest {

    /**
     * @zm-api-field-tag export-dir-path
     * @zm-api-field-description Path for export dir
     */
    @XmlAttribute(name=AdminConstants.A_EXPORT_DIR /* exportDir */, required=false)
    private final String exportDir;

    /**
     * @zm-api-field-tag filename-prefix
     * @zm-api-field-description Export filename prefix
     */
    @XmlAttribute(name=AdminConstants.A_EXPORT_FILENAME_PREFIX /* exportFilenamePrefix */, required=false)
    private final String exportFilenamePrefix;

    /**
     * @zm-api-field-description Mailbox
     */
    @XmlElement(name=AdminConstants.E_MAILBOX /* mbox */, required=true)
    private final ExportAndDeleteMailboxSpec mailbox;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ExportAndDeleteItemsRequest() {
        this((String) null, (String) null, (ExportAndDeleteMailboxSpec) null);
    }

    public ExportAndDeleteItemsRequest(String exportDir,
                    String exportFilenamePrefix, ExportAndDeleteMailboxSpec mailbox) {
        this.exportDir = exportDir;
        this.exportFilenamePrefix = exportFilenamePrefix;
        this.mailbox = mailbox;
    }

    public String getExportDir() { return exportDir; }
    public String getExportFilenamePrefix() { return exportFilenamePrefix; }
    public ExportAndDeleteMailboxSpec getMailbox() { return mailbox; }
}
