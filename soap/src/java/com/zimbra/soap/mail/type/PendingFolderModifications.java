package com.zimbra.soap.mail.type;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;

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
     * @zm-api-field-tag modified
     * @zm-api-field-description list of modified items
     */
    @XmlElement(name=MailConstants.E_MODIFIED /* modified */, required=false)
    private final List<ModifyNotification> modified = Lists.newArrayList();

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

    public void addModifiedItem(ModifyNotification item) {
        modified.add(item);
    }

    public List<CreateItemNotification> getCreated() {
        return created;
    }

    public List<DeleteItemNotification> getDeleted() {
        return deleted;
    }

    public List<ModifyNotification> getModified() {
        return modified;
    }

    public Integer getFolderId() {
        return folderId;
    }
}
