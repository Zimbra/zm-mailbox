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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso({
    NoteActionSelector.class,
    ContactActionSelector.class,
    FolderActionSelector.class
})
public class ActionSelector {

    /**
     * @zm-api-field-tag comma-sep-ids
     * @zm-api-field-description Comma separated list of item IDS to act on
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    protected final String ids;

    /**
     * @zm-api-field-tag operation
     * @zm-api-field-description Operation
     * <br />
     * For ItemAction    - delete|dumpsterdelete|recover|read|flag|priority|tag|move|trash|rename|update|color|lock|unlock
     * <br />
     * For MsgAction     - delete|read|flag|tag|move|update|spam|trash
     * <br />
     * For ConvAction    - delete|read|flag|priority|tag|move|spam|trash
     * <br />
     * For FolderAction  - read|delete|rename|move|trash|empty|color|[!]grant|revokeorphangrants|url|import|sync|fb|[!]check|update|[!]syncon|retentionpolicy|[!]disableactivesync
     * <br />
     * For TagAction     - read|rename|color|delete|update|retentionpolicy
     * <br />
     * For ContactAction - move|delete|flag|trash|tag|update
     * <br />
     * For DistributionListAction -
     * <pre>
     *    delete         delete the list
     *    rename         rename the list
     *    modify         modify the list
     *    addOwners      add list owner
     *    removeOwners   remove list owners
     *    setOwners      set list owners
     *    grantRights    grant rights
     *    revokeRights   revoke rights
     *    setRights      set rights
     *    addMembers     add list members
     *    removeMembers  remove list members
     *    acceptSubsReq  accept subscription/un-subscription request
     *    rejectSubsReq  reject subscription/un-subscription request
     * </pre>
     */
    @XmlAttribute(name=MailConstants.A_OPERATION /* op */, required=true)
    protected final String operation;

    /**
     * @zm-api-field-tag [-]constraint
     * @zm-api-field-description List of characters; constrains the set of affected items in a conversation
     * <table>
     * <tr> <td> <b>t</b> </td> <td> include items in the Trash </td> </tr>
     * <tr> <td> <b>j</b> </td> <td> include items in Spam/Junk </td> </tr>
     * <tr> <td> <b>s</b> </td> <td> include items in the user's Sent folder (not necessarily "Sent") </td> </tr>
     * <tr> <td> <b>o</b> </td> <td> include items in any other folder </td> </tr>
     * </table>
     * A leading '-' means to negate the constraint (e.g. "-t" means all messages not in Trash)
     */
    @XmlAttribute(name=MailConstants.A_TARGET_CONSTRAINT /* tcon */, required=false)
    protected String constraint;

    // Deprecated, use tagNames instead
    /**
     * @zm-api-field-tag tag
     * @zm-api-field-description Deprecated - use "tn" instead
     */
    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAG /* tag */, required=false)
    protected Integer tag;

    /**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description Folder ID
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    protected String folder;

    /**
     * @zm-api-field-tag rgb-color
     * @zm-api-field-description RGB color in format #rrggbb where r,g and b are hex digits
     */
    @XmlAttribute(name=MailConstants.A_RGB /* rgb */, required=false)
    protected String rgb;

    /**
     * @zm-api-field-tag color
     * @zm-api-field-description color numeric; range 0-127; defaults to 0 if not present; client can display only 0-7
     */
    @XmlAttribute(name=MailConstants.A_COLOR /* color */, required=false)
    protected Byte color;

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    protected String name;

    /**
     * @zm-api-field-tag flags
     * @zm-api-field-description Flags
     */
    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    protected String flags;

    /**
     * @zm-api-field-tag tags
     * @zm-api-field-description Tags - Comma separated list of integers.  DEPRECATED - use "tn" instead
     */
    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    protected String tags;

    /**
     * @zm-api-field-tag tag-names
     * @zm-api-field-description Comma-separated list of tag names
     */
    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    protected String tagNames;

    /**
     * no-argument constructor wanted by JAXB
     */
    protected ActionSelector() {
        this((String) null, (String) null);
    }

    public ActionSelector(String ids, String operation) {
        this.ids = ids;
        this.operation = operation;
    }

    public static ActionSelector createForIdsAndOperation(String ids, String operation) {
        return new ActionSelector(ids, operation);
    }

    public void setConstraint(String constraint) { this.constraint = constraint; }

    /**
     * Use {@link ActionSelector#setTagNames(String)} instead.
     */
    @Deprecated
    public void setTag(Integer tag) { this.tag = tag; }
    public void setFolder(String folder) { this.folder = folder; }
    public void setRgb(String rgb) { this.rgb = rgb; }
    public void setColor(Byte color) { this.color = color; }
    public void setName(String name) { this.name = name; }
    public void setFlags(String flags) { this.flags = flags; }
    /**
     * Use {@link ActionSelector#setTagNames(String)} instead.
     */
    @Deprecated
    public void setTags(String tags) { this.tags = tags; }
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }

    public String getIds() { return ids; }
    public String getOperation() { return operation; }
    public String getConstraint() { return constraint; }

    /**
     * Use {@link ActionSelector#getTagNames()} instead.
     */
    @Deprecated
    public Integer getTag() { return tag; }
    public String getFolder() { return folder; }
    public String getRgb() { return rgb; }
    public Byte getColor() { return color; }
    public String getName() { return name; }
    public String getFlags() { return flags; }
    /**
     * Use {@link ActionSelector#getTagNames()} instead.
     */
    @Deprecated
    public String getTags() { return tags; }
    public String getTagNames() { return tagNames; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("ids", ids)
            .add("operation", operation)
            .add("constraint", constraint)
            .add("tag", tag)
            .add("folder", folder)
            .add("rgb", rgb)
            .add("color", color)
            .add("name", name)
            .add("flags", flags)
            .add("tags", tags)
            .add("tagNames", tagNames);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
