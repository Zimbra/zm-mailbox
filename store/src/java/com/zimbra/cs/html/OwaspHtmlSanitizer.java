/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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

package com.zimbra.cs.html;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

public class OwaspHtmlSanitizer extends AbstractDefang {

    @Override
    public void defang(InputStream is, boolean neuterImages, Writer out) throws IOException {
        // TODO to be implemented later
    }

    @Override
    public void defang(Reader reader, boolean neuterImages, Writer out) throws IOException {
        // TODO to be implemented later
    }
}
