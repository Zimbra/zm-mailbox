/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.index;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;

/**
 * Suggested query string.
 *
 * @author ysasaki
 */
public final class SuggestQueryInfo implements QueryInfo {
    private final String suggest;

    public SuggestQueryInfo(String suggest) {
        this.suggest = suggest;
    }

    @Override
    public Element toXml(Element parent) {
        return parent.addElement(MailConstants.E_SUGEST).setText(suggest);
    }

    @Override
    public String toString() {
        return suggest;
    }
}
