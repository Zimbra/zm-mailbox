/* 
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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

package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description File Share With Me
 * <br />
 * This is an internal API, cannot be invoked directly
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = MailConstants.E_FILE_SHARED_WITH_ME_REQUEST)
public class FileSharedWithMeRequest {

    /**
     * @zm-api-field-tag action
     * @zm-api-field-description Action - Create, Edit, Revoke
     */
    @XmlElement(name = MailConstants.E_ACTION, required = true)
    private String action;

    /**
     * @zm-api-field-tag fileName
     * @zm-api-field-description Name of the file which is to be shared
     */
    @XmlElement(name = MailConstants.A_CONTENT_FILENAME, required = true)
    private String fileName;

    /**
     * @zm-api-field-tag ownerFileId
     * @zm-api-field-description Owner File ID 
     */
    @XmlElement(name = MailConstants.A_ITEMID, required = true)
    private int ownerFileId;

    /**
     * @zm-api-field-tag fileUUID
     * @zm-api-field-description Owner File UUID
     */
    @XmlElement(name = MailConstants.A_REMOTE_UUID, required = true)
    private String fileUUID;

    /**
     * @zm-api-field-tag fileOwnerName
     * @zm-api-field-description File Owner Name
     */
    @XmlElement(name = MailConstants.A_OWNER_NAME, required = true)
    private String fileOwnerName;

    /**
     * @zm-api-field-tag rights
     * @zm-api-field-description Permission provided to the file
     */
    @XmlElement(name = MailConstants.A_RIGHTS, required = true)
    private String rights;

    /**
     * @zm-api-field-tag contentType
     * @zm-api-field-description Content type of the file
     */
    @XmlElement(name = MailConstants.A_CONTENT_TYPE, required = true)
    private String contentType;

    /**
     * @zm-api-field-tag size
     * @zm-api-field-description Actual file size
     */
    @XmlElement(name = MailConstants.A_SIZE, required = true)
    private long size;

    /**
     * @zm-api-field-tag ownerAccountId
     * @zm-api-field-description Remote account owner ID
     */
    @XmlElement(name = MailConstants.A_REMOTE_ID, required = true)
    private String ownerAccountId;

    /**
     * @zm-api-field-tag date
     * @zm-api-field-description Actual file modified date
     */
    @XmlElement(name = MailConstants.A_DATE, required = true)
    private Long date;

    public FileSharedWithMeRequest() {
    }

    public FileSharedWithMeRequest(String action, String granteeId, String fileName, String ownerAccountId,
            int ownerFileId, String fileUUID, String fileOwnerName, String rights, String contentType, long size, long date) {
        super();
        this.action = action;
        this.fileName = fileName;
        this.ownerFileId = ownerFileId;
        this.fileUUID = fileUUID;
        this.fileOwnerName = fileOwnerName;
        this.rights = rights;
        this.contentType = contentType;
        this.size = size;
        this.ownerAccountId = ownerAccountId;
        this.date = date;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getOwnerFileId() {
        return ownerFileId;
    }

    public void setOwnerFileId(int ownerFileId) {
        this.ownerFileId = ownerFileId;
    }

    public String getFileUUID() {
        return fileUUID;
    }

    public void setFileUUID(String fileUUID) {
        this.fileUUID = fileUUID;
    }

    public String getFileOwnerName() {
        return fileOwnerName;
    }

    public void setFileOwnerName(String fileOwnerName) {
        this.fileOwnerName = fileOwnerName;
    }

    public String getRights() {
        return rights;
    }

    public void setRights(String rights) {
        this.rights = rights;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getOwnerAccountId() {
        return ownerAccountId;
    }

    public void setOwnerAccountId(String ownerAccountId) {
        this.ownerAccountId = ownerAccountId;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

}
