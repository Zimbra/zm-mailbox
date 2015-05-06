/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap.entry;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mime.MimeTypeInfo;

/**
 *
 * @author pshao
 *
 */
public abstract class LdapMimeTypeBase extends Entry implements LdapEntry, MimeTypeInfo {

    protected String mDn;
    protected String entryCSN;

    protected LdapMimeTypeBase(Map<String,Object> attrs, Map<String,Object> defaults, Provisioning provisioning) {
        super(attrs, defaults, provisioning);
    }

    @Override
    public EntryType getEntryType() {
        return EntryType.MIMETYPE;
    }

    @Override
    public String getLabel() {
        return mDn;
    }

    @Override
    public String getDN() {
        return mDn;
    }

    @Override
    public String getEntryCSN() {
        return entryCSN;
    }

    @Override
    public String[] getMimeTypes() {
        return super.getMultiAttr(Provisioning.A_zimbraMimeType);
    }

    @Override
    public String getHandlerClass() {
        return super.getAttr(Provisioning.A_zimbraMimeHandlerClass, null);
    }

    @Override
    public boolean isIndexingEnabled() {
        return super.getBooleanAttr(Provisioning.A_zimbraMimeIndexingEnabled, true);
    }

    @Override
    public String getDescription() {
        return super.getAttr(Provisioning.A_description, "");
    }

    @Override
    public Set<String> getFileExtensions() {
        String[] extensions = super.getMultiAttr(Provisioning.A_zimbraMimeFileExtension);
        Set<String> extSet = new TreeSet<String>();
        for (String ext : extensions) {
            if (ext != null) {
                extSet.add(ext.toLowerCase());
            }
        }
        return extSet;
    }

    @Override
    public String getExtension() {
        return super.getAttr(Provisioning.A_zimbraMimeHandlerExtension, null);
    }

    @Override
    public int getPriority() {
        return super.getIntAttr(Provisioning.A_zimbraMimePriority, 0);
    }
}
