package com.zimbra.soap.mail.type;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.ModifyNotification.ModifyItemNotification;
import com.zimbra.soap.mail.type.ModifyNotification.ModifyTagNotification;
import com.zimbra.soap.mail.type.ModifyNotification.RenameFolderNotification;

@XmlAccessorType(XmlAccessType.NONE)
public class PendingFolderModifications {
    /**
     * @zm-api-field-tag id
     * @zm-api-field-description ID of signaled folder
     */
    @XmlAttribute(name=MailConstants.A_ID, required=true)
    private final Integer folderId;

    /**
     * @zm-api-field-tag created
     * @zm-api-field-description list of created items
     */
    @XmlElement(name=MailConstants.E_CREATED /* created */, required=false)
    private final List<CreateItemNotification> created = Lists.newArrayList();

    /**
     * @zm-api-field-tag deleted
     * @zm-api-field-description list of deleted items
     */
    @XmlElement(name=MailConstants.E_DELETED /* deleted */, required=false)
    private final List<DeleteItemNotification> deleted = Lists.newArrayList();

    /**
     * @zm-api-field-tag modMsgs
     * @zm-api-field-description list of modified messages
     */
    @XmlElement(name=MailConstants.E_MODIFIED_MSGS /* modMsgs */, required=false, type=ModifyItemNotification.class)
    private final List<ModifyItemNotification> modifiedMsgs = Lists.newArrayList();

    /**
     * @zm-api-field-tag modTags
     * @zm-api-field-description list of modified tags
     */
    @XmlElement(name=MailConstants.E_MODIFIED_TAGS /* modTags */, required=false, type=ModifyTagNotification.class)
    private final List<ModifyTagNotification> modifiedTags = Lists.newArrayList();

    /**
     * @zm-api-field-tag modFolders
     * @zm-api-field-description list of renamed folders
     */
    @XmlElement(name=MailConstants.E_MODIFIED_FOLDERS /* modFolders */, required=false, type=RenameFolderNotification.class)
    private final List<RenameFolderNotification> modifiedFolders = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    public PendingFolderModifications() {
        this((Integer)null);
    }

    public PendingFolderModifications(Integer folderId) {
        this.folderId = folderId;
    }

    public void addCreatedItem(CreateItemNotification item) {
        created.add(item);
    }

    public void addDeletedItem(DeleteItemNotification item) {
        deleted.add(item);
    }


    public List<CreateItemNotification> getCreated() {
        return created;
    }

    public List<DeleteItemNotification> getDeleted() {
        return deleted;
    }

    public void addModifiedMsg(ModifyItemNotification item) {
        modifiedMsgs.add(item);
    }

    public List<ModifyItemNotification> getModifiedMsgs() {
        return modifiedMsgs;
    }

    public void addModifiedTag(ModifyTagNotification item) {
        modifiedTags.add(item);
    }

    public List<ModifyTagNotification> getModifiedTags() {
        return modifiedTags;
    }

    public void addRenamedFolder(RenameFolderNotification item) {
        modifiedFolders.add(item);
    }

    public List<RenameFolderNotification> getRenamedFolders() {
        return modifiedFolders;
    }

    public Integer getFolderId() {
        return folderId;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper.add("folderId", folderId);
        if (!created.isEmpty()) {
            helper.add("created", created);
        }
        if (!deleted.isEmpty()) {
            helper.add("deleted", deleted);
        }
        if (!modifiedMsgs.isEmpty()) {
            helper.add("modifiedMsgs", modifiedMsgs);
        }
        if (!modifiedTags.isEmpty()) {
            helper.add("modifiedTags", modifiedTags);
        }
        if (!modifiedFolders.isEmpty()) {
            helper.add("modifiedFolders", modifiedFolders);
        }
        return helper;
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
