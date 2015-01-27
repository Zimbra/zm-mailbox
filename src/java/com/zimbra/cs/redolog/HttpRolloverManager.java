/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.cs.redolog;

import java.io.IOException;

/**
 *
 * Generally rollover is handled by the redolog servlet instead of the http client, so the primary purpose of this class is to stub out the rollover operations
 *
 */
public class HttpRolloverManager implements RolloverManager {

    @Override
    public void crashRecovery() throws IOException {
    }

    @Override
    public long getCurrentSequence() {
        return 0;
    }

    @Override
    public void initSequence(long seq) {
    }

    @Override
    public long incrementSequence() {
        return 0;
    }

}
