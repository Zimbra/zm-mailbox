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

package com.zimbra.soap.account.message;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.Prop;

/**
      <ModifyPropertiesRequest>
          <prop zimlet="{zimlet-name}" name="{name}">{value}</prop>
          ...
          <prop zimlet="{zimlet-name}" name="{name}">{value}</prop>
      </ModifyPropertiesRequest>
 * @zm-api-command-description Modify properties related to zimlets
 */
@XmlRootElement(name=AccountConstants.E_MODIFY_PROPERTIES_REQUEST)
public class ModifyPropertiesRequest {
    /**
     * @zm-api-field-description Property to be modified
     */
    @XmlElement(name=AccountConstants.E_PROPERTY, required=true)
    private List<Prop> props = new ArrayList<Prop>();

    public List<Prop> getProps() {
        return props; 
    }

    public void setProps(List<Prop> props) {
        this.props = props;
    }
}
