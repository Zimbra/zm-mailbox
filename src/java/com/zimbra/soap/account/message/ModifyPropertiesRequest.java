/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
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
