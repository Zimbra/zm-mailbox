package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Returns the IDs of all items modified since a given change number
 */
@XmlRootElement(name=MailConstants.E_GET_MODIFIED_ITEMS_IDS_REQUEST)
public class GetModifiedItemsIDsRequest {
    /**
     * @zm-api-field-tag root-folder-id
     * @zm-api-field-description Root folder ID.  If present, we start sync there rather than at folder 11
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=true)
    private String folderId;

    /**
     * @zm-api-field-tag CHANGEDSINCE <mod-sequence>
     * @zm-api-field-description mod-sequence value passed by IMAP client in CHANGEDSINCE modifier
     */
    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */, required=true)
    private Integer modSeq;

    public GetModifiedItemsIDsRequest() {
        modSeq = null;
        folderId = null;
    }

    /**
     * 
     * @param folderId
     * @param modSeq
     */
    public GetModifiedItemsIDsRequest(String folderId, Integer modSeq) {
        this.folderId = folderId;
        this.modSeq = modSeq;
    }

    public Integer getModSeq() {
        return modSeq;
    }
    public void setMobSeq(Integer modSeq) {
        this.modSeq = modSeq;
    }

    public String getFolderId() {
        return folderId;
    }
    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }
}
