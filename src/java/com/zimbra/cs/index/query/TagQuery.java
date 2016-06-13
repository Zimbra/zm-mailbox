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
package com.zimbra.cs.index.query;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.DBQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Tag;

/**
 * Query by tag.
 *
 * @author tim
 * @author ysasaki
 */
public class TagQuery extends Query {

    private final String name;

    public TagQuery(String name, boolean bool) {
        this.name = name;
        setBool(bool);
    }

    @Override
    public boolean hasTextOperation() {
        return false;
    }

    @Override
    public QueryOperation compile(Mailbox mbox, boolean bool) throws ServiceException {
        DBQueryOperation op = new DBQueryOperation();
        try {
            op.addTag(mbox.getTagByName(null, name), evalBool(bool));
        } catch (MailServiceException mse) {
            if (MailServiceException.NO_SUCH_TAG.equals(mse.getCode())) {
                // Probably clicked on a remote tag for an item in a shared folder which isn't mirrored by a local tag
                // Would be too confusing for UI to then claim that there was no such tag (Bug 77646)
                op.addTag(Tag.createPseudoRemoteTag(mbox, name), evalBool(bool));
            } else {
                throw mse;
            }
        }
        return op;
    }

    @Override
    public void dump(StringBuilder out) {
        out.append("TAG:");
        out.append(name);
    }

    @Override
    public void sanitizedDump(StringBuilder out) {
        out.append("TAG:");
        out.append("$TAG");
    }

}
