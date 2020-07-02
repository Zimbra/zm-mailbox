/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account;

import java.util.Map;
import java.util.Set;

public class Zimlet extends NamedEntry {
	public Zimlet(String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(name, id, attrs, null, prov);
    }

    @Override
    public EntryType getEntryType() {
        return EntryType.ZIMLET;
    }
    
    public boolean isEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraZimletEnabled, false);
    }
    
    public String getPriority() {
        return getAttr(Provisioning.A_zimbraZimletPriority);
    }
    
    public boolean isExtension() {
        return getBooleanAttr(Provisioning.A_zimbraZimletIsExtension, false);
    }

    public String getType() {
        return getAttr(Provisioning.A_cn);
    }
    
    public String getDescription() {
        return getAttr(Provisioning.A_zimbraZimletDescription);
    }
    
    public boolean isIndexingEnabled() {
        return getBooleanAttr(Provisioning.A_zimbraZimletIndexingEnabled, false);
    }
    
    public String getHandlerClassName() {
        return getAttr(Provisioning.A_zimbraZimletHandlerClass);
    }
    
    public String getHandlerConfig() {
        return getAttr(Provisioning.A_zimbraZimletHandlerConfig);
    }

    public String getServerIndexRegex() {
        return getAttr(Provisioning.A_zimbraZimletServerIndexRegex);
    }

    public String getZimbraXCompatibleSemVer() {
        return getAttr(Provisioning.A_zimbraXZimletCompatibleSemVer);
    }

	public boolean checkTarget(String target) {
		Set<String> lTiers = getMultiAttrSet(Provisioning.A_zimbraZimletTarget); 
		return ((lTiers == null) ? false : lTiers.contains(target));
	}

}
