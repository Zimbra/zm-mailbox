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

package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso({
    NoteActionSelector.class,
    ContactActionSelector.class,
    FolderActionSelector.class
})
@GraphQLType(name="ActionSelector")
public class ActionSelector {

    /**
     * @zm-api-field-tag comma-sep-ids
     * @zm-api-field-description Comma separated list of item IDs to act on.  Required except for TagActionRequest,
     * where the tags items can be specified using their tag names as an alternative.
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    @GraphQLQuery(name="ids", description="Comma separated list of item IDs to act on.  Required except for op=tag, where the tags items can be specified using their tag names as an alternative.")
    protected final String ids;

    /**
     * @zm-api-field-tag operation
     * @zm-api-field-description Operation
     * <br />
     * For ItemAction    - delete|dumpsterdelete|recover|read|flag|priority|tag|move|trash|rename|update|color|lock|unlock|resetimapuid|copy
     * <br />
     * For MsgAction     - delete|read|flag|tag|move|update|spam|trash
     * <br />
     * For ConvAction    - delete|read|flag|priority|tag|move|spam|trash
     * <br />
     * For FolderAction  - read|delete|rename|move|trash|empty|color|[!]grant|revokeorphangrants|url|import|sync|fb|[!]check|update|[!]syncon|retentionpolicy|[!]disableactivesync|webofflinesyncdays
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
     *    resetimapuid   reset IMAP item UIDs
     * </pre>
     */
    @XmlAttribute(name=MailConstants.A_OPERATION /* op */, required=true)
    @GraphQLNonNull @GraphQLQuery(name="operation", description="The operation to perform")
    protected final String operation;

    /**
     * @zm-api-field-tag [-]constraint
     * @zm-api-field-description List of characters; constrains the set of affected items in a conversation
     * <table>
     * <tr> <td> <b>t</b> </td> <td> include items in the Trash </td> </tr>
     * <tr> <td> <b>j</b> </td> <td> include items in Spam/Junk </td> </tr>
     * <tr> <td> <b>s</b> </td> <td> include items in the user's Sent folder (not necessarily "Sent") </td> </tr>
     * <tr> <td> <b>d</b> </td> <td> include items in Drafts folder </td> </tr>
     * <tr> <td> <b>o</b> </td> <td> include items in any other folder </td> </tr>
     * </table>
     * A leading '-' means to negate the constraint (e.g. "-t" means all messages not in Trash)
     */
    @XmlAttribute(name=MailConstants.A_TARGET_CONSTRAINT /* tcon */, required=false)
    @GraphQLQuery(name="constraint", description="List of characters; constrains the set of affected items in a conversation. t|j|s|d|o")
    protected String constraint;

    // Deprecated, use tagNames instead
    /**
     * @zm-api-field-tag tag
     * @zm-api-field-description Deprecated - use "tn" instead
     */
    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAG /* tag */, required=false)
    @GraphQLIgnore
    protected Integer tag;

    /**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description Folder ID
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    @GraphQLQuery(name="folderId", description="Folder id")
    protected String folder;

    /**
     * @zm-api-field-tag rgb-color
     * @zm-api-field-description RGB color in format #rrggbb where r,g and b are hex digits
     */
    @XmlAttribute(name=MailConstants.A_RGB /* rgb */, required=false)
    @GraphQLQuery(name="rgb", description="RGB color in format #rrggbb where r,g and b are hex digits")
    protected String rgb;

    /**
     * @zm-api-field-tag color
     * @zm-api-field-description color numeric; range 0-127; defaults to 0 if not present; client can display only 0-7
     */
    @XmlAttribute(name=MailConstants.A_COLOR /* color */, required=false)
    @GraphQLQuery(name="color", description="color numeric; range 0-127; defaults to 0 if not present; client can display only 0-7")
    protected Byte color;

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    @GraphQLQuery(name="name", description="Name")
    protected String name;

    /**
     * @zm-api-field-tag flags
     * @zm-api-field-description Flags
     */
    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    @GraphQLQuery(name="flags", description="Flags")
    protected String flags;

    /**
     * @zm-api-field-tag tags
     * @zm-api-field-description Tags - Comma separated list of integers.  DEPRECATED - use "tn" instead
     */
    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    @GraphQLIgnore
    protected String tags;

    /**
     * @zm-api-field-tag tag-names
     * @zm-api-field-description Comma-separated list of tag names
     */
    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    @GraphQLQuery(name="tagNames", description="Comma-separated list of tag names")
    protected String tagNames;

    /**
     * @zm-api-field-tag non-existent-ids
     * @zm-api-field-description Flag to signify that any non-existent ids should be returned
     */
    @XmlAttribute(name=MailConstants.A_NON_EXISTENT_IDS /* nei */, required=false)
    @GraphQLQuery(name="nonExistentIds", description="Flag to signify that any non-existent ids should be returned")
    protected ZmBoolean nonExistentIds;

    /**
     * @zm-api-field-tag newly-created-ids
     * @zm-api-field-description Flag to signify that ids of new items should be returned
     * <br /> applies to COPY action
     */
    @XmlAttribute(name=MailConstants.A_NEWLY_CREATED_IDS /* nci */, required=false)
    @GraphQLQuery(name="newlyCreatedIds", description="Flag to signify that ids of new items should be returned applies to COPY action")
    protected ZmBoolean newlyCreatedIds;

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
    public void setNonExistentIds(boolean r) { this.nonExistentIds = ZmBoolean.fromBool(r); };
    /**
     * Use {@link ActionSelector#setTagNames(String)} instead.
     */
    @Deprecated
    public void setTags(String tags) { this.tags = tags; }
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }

    public String getIds() { return ids; }
    public String getOperation() { return operation; }
    public String getConstraint() { return constraint; }
    public boolean getNonExistentIds() { return ZmBoolean.toBool(nonExistentIds); };
    public void setNewlyCreatedIds(boolean r) { this.newlyCreatedIds = ZmBoolean.fromBool(r); };
    public boolean getNewlyCreatedIds() { return ZmBoolean.toBool(newlyCreatedIds); };

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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
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
            .add("tagNames", tagNames)
            .add("nonExistentIds", nonExistentIds)
            .add("newlyCreatedIds", newlyCreatedIds);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
