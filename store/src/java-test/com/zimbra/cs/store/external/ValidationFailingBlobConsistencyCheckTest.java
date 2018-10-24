/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.store.external;

import org.junit.After;
import org.junit.Ignore;

import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.file.BlobConsistencyChecker;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class ValidationFailingBlobConsistencyCheckTest extends ExternalBlobConsistencyCheckTest {

    private MockValidationFailingStore storeManager = new MockValidationFailingStore();

    @Override
    protected StoreManager getStoreManager() {
        return storeManager;
    }

    @Override
    protected BlobConsistencyChecker getChecker() {
        //need to suppress validation errors until after test data is setup
        //so we set it here, and unset it in teardown
        //careful here if refactoring the tests; as order matters
        storeManager.setFailOnValidate(true);
        return super.getChecker();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        storeManager.setFailOnValidate(false);
        super.tearDown();
    }

}
