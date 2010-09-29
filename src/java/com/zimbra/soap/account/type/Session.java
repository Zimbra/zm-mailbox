/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/*
       [<session [id="{returned-from-server-in-last-response}" [seq="{highest_notification_received}"]]/>]
 */
@XmlType(propOrder = {})
public class Session {

    @XmlElement private String id;
    @XmlElement private Long seq;
    
    public Session() {
    }
    
    public Session(String id, Long seq) {
        setId(id);
        setSeq(seq);
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public Long getSeq() { return seq; }
    public void setSeq(Long seq) { this.seq = seq; }
}
