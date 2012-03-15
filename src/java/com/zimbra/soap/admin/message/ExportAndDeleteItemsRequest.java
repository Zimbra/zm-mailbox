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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.MailboxIdAndItems;

/**
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
    @XmlAttribute(name=AdminConstants.A_EXPORT_DIR, required=false)
    private final String exportDir;

    /**
     * @zm-api-field-tag filename-prefix
     * @zm-api-field-description Export filename prefix
     */
    @XmlAttribute(name=AdminConstants.A_EXPORT_FILENAME_PREFIX, required=false)
    private final String exportFilenamePrefix;

    /**
     * @zm-api-field-description Mailbox
     */
    @XmlElement(name=AdminConstants.E_MAILBOX, required=true)
    private final MailboxIdAndItems mailbox;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ExportAndDeleteItemsRequest() {
        this((String) null, (String) null, (MailboxIdAndItems) null);
    }

    public ExportAndDeleteItemsRequest(String exportDir,
                    String exportFilenamePrefix, MailboxIdAndItems mailbox) {
        this.exportDir = exportDir;
        this.exportFilenamePrefix = exportFilenamePrefix;
        this.mailbox = mailbox;
    }

    public String getExportDir() { return exportDir; }
    public String getExportFilenamePrefix() { return exportFilenamePrefix; }
    public MailboxIdAndItems getMailbox() { return mailbox; }
}
