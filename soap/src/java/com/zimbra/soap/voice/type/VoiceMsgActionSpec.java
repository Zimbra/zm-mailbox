/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.voice.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class VoiceMsgActionSpec {

    /**
     * @zm-api-field-tag operation
     * @zm-api-field-description <b>move|[!]read|empty|delete</b>
     * <ul>
     * <li> read op can be preceeded by a "!" to negate it</li>
     * <li> for move:
     *      <ul>
     *          <li> if dest location is a trash folder, server assumes source folder is voicemail and performs a
     *               gateway undelete </li>
     *          <li> if dest location is a voicemail folder, server assumes source folder is trash and performs a
     *               gateway soft delete </li>
     *      </ul>
     * </li>
     * <li> for delete:
     *      <ul>
     *          <li> hard deletes the voice msgs. location/dest folder can be null for this op. </li>
     *      </ul>
     * </li>
     * </ul>
     */
    @XmlAttribute(name=MailConstants.A_OPERATION /* op */, required=true)
    private String operation;

    /**
     * @zm-api-field-tag phone-number
     * @zm-api-field-description Phone number
     */
    @XmlAttribute(name=VoiceConstants.A_PHONE /* phone */, required=true)
    private String phoneNum;

    /**
     * @zm-api-field-tag location
     * @zm-api-field-description Folder ID of the destination location for the move
     * <ul>
     * <li> required for <b>op="move"</b> </li>
     * <li> ignored for all other ops </li>
     * </ul>
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    /**
     * @zm-api-field-tag list
     * @zm-api-field-description IDs list.
     * <ul>
     * <li> for <b>move</b> and <b>read</b> operations: list of message ids to act on </li>
     * <li> for <b>empty</b> operation: single value of folder id to act on.
     *      <br/> Currently the empty op is only supported for a trash folder.  If <b>{location}</b> points to a
     *            folder that is not of type trash, server will return <b>INVALID_REQUEST</b> error.
     * </li>
     * </ul>
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private String ids;

    public VoiceMsgActionSpec() {
    }

    public void setOperation(String operation) { this.operation = operation; }
    public void setPhoneNum(String phoneNum) { this.phoneNum = phoneNum; }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setIds(String ids) { this.ids = ids; }
    public String getOperation() { return operation; }
    public String getPhoneNum() { return phoneNum; }
    public String getFolderId() { return folderId; }
    public String getIds() { return ids; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("operation", operation)
            .add("phoneNum", phoneNum)
            .add("folderId", folderId)
            .add("ids", ids);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
