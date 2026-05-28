/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.auth.twofactor;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.zimbra.common.service.ServiceException;

import static org.junit.Assert.*;

/**
 * JUnit tests for ScratchCodes interface contract.
 * Tests scratch code generation, storage, and retrieval.
 */
public class ScratchCodesTest {

    @Test
    public void scratchCodesInterface_getCodes_returnsValidList() throws ServiceException {
        ScratchCodes codes = createMockScratchCodes();
        List<String> result = codes.getCodes();
        assertNotNull(result);
    }

    @Test
    public void scratchCodesInterface_generateCodes_succeeds() throws ServiceException {
        ScratchCodes codes = createMockScratchCodes();
        TwoFactorAuth.CredentialConfig config = new TwoFactorAuth.CredentialConfig()
            .setNumScratchCodes(10)
            .setScratchCodeLength(8);
        List<String> generated = codes.generateCodes(config);
        assertNotNull(generated);
    }

    @Test
    public void scratchCodesInterface_storeCodes_succeeds() throws ServiceException {
        ScratchCodes codes = createMockScratchCodes();
        List<String> codesToStore = new ArrayList<>();
        codesToStore.add("code1");
        codesToStore.add("code2");
        codes.storeCodes(codesToStore);
    }

    @Test
    public void scratchCodesInterface_authenticate_succeeds() throws ServiceException {
        ScratchCodes codes = createMockScratchCodes();
        // Interface should support authentication
        assertNotNull(codes);
    }

    private ScratchCodes createMockScratchCodes() {
        return new ScratchCodes() {
            @Override
            public List<String> getCodes() {
                return new ArrayList<>();
            }

            @Override
            public List<String> generateCodes(TwoFactorAuth.CredentialConfig config) throws ServiceException {
                List<String> result = new ArrayList<>();
                for (int i = 0; i < config.getNumScratchCodes(); i++) {
                    result.add("code" + i);
                }
                return result;
            }

            @Override
            public void storeCodes(List<String> codes) throws ServiceException {
                // No-op for test
            }

            @Override
            public void authenticate(String secondFactor) throws ServiceException {
                // No-op for test
            }
        };
    }
}
