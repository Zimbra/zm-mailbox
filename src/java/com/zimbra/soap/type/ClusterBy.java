/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2013, 2014 Zimbra, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.type;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlEnum;

import com.zimbra.common.service.ServiceException;


/**
 * @author zimbra
 *
 */
@XmlEnum
public enum ClusterBy {

    // case must match
    id, name;

    public static ClusterBy fromString(String s)
    throws ServiceException {
        try {
            return ClusterBy.valueOf(s);
        } catch (IllegalArgumentException e) {
           throw ServiceException.INVALID_REQUEST("unknown 'By' key: " + s + ", valid values: " +
                   Arrays.asList(ClusterBy.values()), null);
        }
    }
}
