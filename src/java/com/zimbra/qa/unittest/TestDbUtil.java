package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import com.zimbra.cs.db.DbUtil;

/**
 * @author bburtin
 */
public class TestDbUtil extends TestCase
{
    public void testNormalizeSql()
    throws Exception {
        String sql = " \t SELECT a, 'b', 1, '', ',', NULL, '\\'' FROM table1\n\nWHERE c IN (1, 2, 3) ";
        String normalized = DbUtil.normalizeSql(sql);
        String expected = "SELECT a, XXX, XXX, XXX, XXX, XXX, XXX FROM tableXXX WHERE c IN (...)";
        assertEquals(expected, normalized);
    }
}
