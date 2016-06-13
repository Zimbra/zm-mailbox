/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account.ldap.upgrade;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;

public class BUG_88098 extends UpgradeOp {

    @Override
    void doUpgrade() throws ServiceException {
        upgradeZimbraGalLdapFilterDef();
    }

    @SuppressWarnings("unchecked")
    private void upgradeZimbraGalLdapFilterDef() throws ServiceException {
        Config config = prov.getConfig();

        Pair<String,String> value = 
                new Pair<String, String>(
                        "zimbraAccountAutoComplete:(&(|(displayName=%s*)(cn=%s*)(sn=%s*)(gn=%s*)(zimbraPhoneticFirstName=%s*)(zimbraPhoneticLastName=%s*)(mail=%s*)(zimbraMailDeliveryAddress=%s*)(zimbraMailAlias=%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList)(objectclass=zimbraGroup))(!(objectclass=zimbraCalendarResource)))",
                        "zimbraAccountAutoComplete:(&(|(displayName=*%s*)(cn=%s*)(sn=%s*)(gn=%s*)(zimbraPhoneticFirstName=%s*)(zimbraPhoneticLastName=%s*)(mail=%s*)(zimbraMailDeliveryAddress=%s*)(zimbraMailAlias=%s*))(|(objectclass=zimbraAccount)(objectclass=zimbraDistributionList)(objectclass=zimbraGroup))(!(objectclass=zimbraCalendarResource)))");

        Set<String> curValues = config.getMultiAttrSet(Provisioning.A_zimbraGalLdapFilterDef);

        Map<String, Object> attrs = new LinkedHashMap<String, Object>();
        String oldValue = value.getFirst();
        String newValue = value.getSecond();

        if (curValues.contains(oldValue)) {
            StringUtil.addToMultiMap(attrs, "-" + Provisioning.A_zimbraGalLdapFilterDef, oldValue);
            StringUtil.addToMultiMap(attrs, "+" + Provisioning.A_zimbraGalLdapFilterDef, newValue);
        }
        modifyAttrs(config, attrs);
    }
}
