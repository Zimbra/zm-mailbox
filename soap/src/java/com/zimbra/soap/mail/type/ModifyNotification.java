package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.account.message.ImapMessageInfo;

@XmlAccessorType(XmlAccessType.NONE)
public class ModifyNotification {

    /**
     * @zm-api-field-description bitmask of modification change
     */
    @XmlAttribute(name=MailConstants.A_CHANGE, /* change */ required=true)
    private int changeBitmask;

    public ModifyNotification() {}

    public ModifyNotification(int bitmask) { changeBitmask = bitmask; }

    public void setChangeBitmask(int bitmask) { changeBitmask = bitmask; }
    public int getChangeBitmask() { return changeBitmask; }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class ModifyTagNotification extends ModifyNotification {

        /**
         * @zm-api-field-description ID of modified tag
         */
        @XmlElement(name=MailConstants.A_ID, /* id */ required=true)
        private int id;

        /**
         * @zm-api-field-description name of modified tag
         */
        @XmlElement(name=MailConstants.A_NAME, /* name */ required=true)
        private String name;

        public ModifyTagNotification() {}

        public ModifyTagNotification(int id, String name, int changeBitmask) {
            super(changeBitmask);
            this.id = id;
            this.name = name;
        }

        public void setTagIdAndName(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() { return id; }
        public String getName() { return name; }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class ModifyItemNotification extends ModifyNotification {

        /**
         * @zm-api-field-description modified IMAP message
         */
        @XmlElement(name=MailConstants.E_MSG, /* m */ required=true)
        private ImapMessageInfo msgInfo;

        public ModifyItemNotification() {}

        public ModifyItemNotification(ImapMessageInfo msgInfo, int changeBitmask) {
            super(changeBitmask);
            this.msgInfo = msgInfo;
        }

        public void setMessageInfo(ImapMessageInfo msgInfo) { this.msgInfo = msgInfo; }
        public ImapMessageInfo getMessageInfo() { return msgInfo; }
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class RenameFolderNotification extends ModifyNotification {

        /**
         * @zm-api-field-description ID of renamed folder
         */
        @XmlAttribute(name=MailConstants.A_ID, /* id */ required=true)
        private int folderId;

        /**
         * @zm-api-field-description new path of renamed folder
         */
        @XmlAttribute(name=MailConstants.A_PATH, /* path */ required=true)
        private String path;

        public RenameFolderNotification() {}

        public RenameFolderNotification(int id, String path, int changeBitmask) {
            super(changeBitmask);
            this.folderId = id;
            this.path = path;
        }

        public void setFolderId(int id) { this.folderId = id; }
        public int getFolderId() { return folderId; }

        public void setPath(String path) { this.path = path; }
        public String getPath() { return path; }
    }
}
