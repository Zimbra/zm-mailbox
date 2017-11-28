package com.zimbra.cs.zimlet;

import static org.junit.Assert.*;

import org.junit.Test;

import com.zimbra.common.localconfig.LC;

public class ZimletUtilTest {
    @Test
    public void testZimletRootDir() {
        try {
            ZimletUtil.getZimletRootDir("../conf/file.ext");
            fail("Should throw ZimletException for bad zimlet name");
        } catch (ZimletException e) {
            
        }

        try {
            ZimletUtil.getZimletRootDir("$somenonsense");
            fail("Should throw ZimletException for bad zimlet name");
        } catch (ZimletException e) {
            
        }

        try {
            ZimletUtil.getZimletRootDir("%otherNonsense.ext");
            fail("Should throw ZimletException for bad zimlet name");
        } catch (ZimletException e) {
            
        }

        try {
            ZimletUtil.getZimletRootDir("//\\/'%");
            fail("Should throw ZimletException for bad zimlet name");
        } catch (ZimletException e) {
            
        }

        try {
            assertEquals(LC.zimlet_directory.value() + "/org_my_zimlet", ZimletUtil.getZimletRootDir("org_my_zimlet").getPath());
        } catch (ZimletException e) {
            fail("Should not throw ZimletException for good zimlet name");
        }

        try {
            assertEquals(LC.zimlet_directory.value() + "/myzimlet", ZimletUtil.getZimletRootDir("myzimlet").getPath());
        } catch (ZimletException e) {
            fail("Should not throw ZimletException for good zimlet name");
        }

        try {
            assertEquals(LC.zimlet_directory.value() + "/my.zimlet", ZimletUtil.getZimletRootDir("my.zimlet").getPath());
        } catch (ZimletException e) {
            fail("Should not throw ZimletException for good zimlet name");
        }
    }
}
