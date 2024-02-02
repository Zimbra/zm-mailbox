package com.zimbra.soap.mail.message;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;

@XmlRootElement(name=MailConstants.E_GET_MODIFIED_ITEMS_IDS_RESPONSE)
public class GetModifiedItemsIDsResponse {
    /**
     * @zm-api-field-description IDs of modified items
     */
    @XmlElement(name=MailConstants.A_MODIFIED_IDS /* mids */, required=false)
    private List<Integer> mids = Lists.newArrayList();
    /**
     * @zm-api-field-description IDs of deleted items
     */
    @XmlElement(name=MailConstants.A_DELETED_IDS /* dids */, required=false)
    private List<Integer> dids = Lists.newArrayList();

    public GetModifiedItemsIDsResponse() {
    }

    public void setMids(Iterable <Integer> mids) {
        this.mids.clear();
        if (mids != null) {
            Iterables.addAll(this.mids,mids);
        }
    }

    public void setDids(Iterable <Integer> dids) {
        this.dids.clear();
        if (dids != null) {
            Iterables.addAll(this.dids,dids);
        }
    }

    public GetModifiedItemsIDsResponse addId(Integer id) {
        this.mids.add(id);
        return this;
    }

    public List<Integer> getMids() {
        return Collections.unmodifiableList(mids);
    }

    public List<Integer> getDids() {
        return Collections.unmodifiableList(dids);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper.add("mids", mids).add("dids", dids);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
