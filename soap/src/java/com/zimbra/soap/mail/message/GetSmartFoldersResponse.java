package com.zimbra.soap.mail.message;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.TagInfo;

@XmlRootElement(name=MailConstants.E_GET_SMART_FOLDERS_RESPONSE)
public class GetSmartFoldersResponse {
    /**
     * @zm-api-field-description Information about smart folders
     */
    @XmlElement(name=MailConstants.E_SMART_FOLDER, required=false)
    private List<TagInfo> smartFolders = Lists.newArrayList();

    public GetSmartFoldersResponse() {
    }

    public void setSmartFolders(Iterable <TagInfo> tags) {
        this.smartFolders.clear();
        if (tags != null) {
            Iterables.addAll(this.smartFolders, tags);
        }
    }

    public GetSmartFoldersResponse addSmartFolder(TagInfo tag) {
        this.smartFolders.add(tag);
        return this;
    }

    public List<TagInfo> getSmartFolders() {
        return Collections.unmodifiableList(smartFolders);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("smartFolders", smartFolders)
            .toString();
    }
}
