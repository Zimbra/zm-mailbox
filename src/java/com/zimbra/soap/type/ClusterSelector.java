/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2013, 2014 Zimbra, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.account.Key.AlwaysOnClusterBy;
import com.zimbra.common.soap.AdminConstants;


/**
 * @author zimbra
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ClusterSelector {
    /**
     * @zm-api-field-tag cluster-selector-by
     * @zm-api-field-description Select the meaning of <b>{cluster-selector-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY, required=true)
    private final AlwaysOnClusterBy clusterBy;

    /**
     * @zm-api-field-tag cluster-selector-key
     * @zm-api-field-description The key used to identify the account. Meaning determined by <b>{cluster-selector-by}</b>
     */
    @XmlValue
    private final String key;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ClusterSelector() {
        this.clusterBy = null;
        this.key = null;
    }

    public ClusterSelector(AlwaysOnClusterBy by, String key) {
        this.clusterBy = by;
        this.key = key;
    }

    public String getKey() { return key; }

    public AlwaysOnClusterBy getBy() { return clusterBy; }

    public static ClusterSelector fromId(String id) {
        return new ClusterSelector(AlwaysOnClusterBy.id, id);
    }

    public static ClusterSelector fromName(String name) {
        return new ClusterSelector(AlwaysOnClusterBy.name, name);
    }

}
