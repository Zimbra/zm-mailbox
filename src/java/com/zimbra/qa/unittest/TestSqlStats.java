/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Joiner;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.StatTrackingPreparedStatement;
import com.zimbra.cs.stats.ActivityTracker;

/**
 * Testing that {@link StatTrackingPreparedStatement} can properly track the number of
 * SELECT, INSERT, DELETE, UPDATE, and other queries.
 * @author iraykin
 *
 */
public class TestSqlStats {
   //the file sql_test.csv should't actually get created since we're never dumping the data
    ActivityTracker tracker = new ActivityTracker("sql_test.csv");
    DbConnection conn;

    @Before
    public void startup() throws Exception  {
        DbPool.startup();
        conn = DbPool.getConnection();
    }

    @Test
    public void test() throws Exception {
        //create the table (tracks "other")
        StatTrackingPreparedStatement create = (StatTrackingPreparedStatement) conn.prepareStatement("CREATE TABLE stats_test(col1 INTEGER)");
        create.setTracker(tracker);
        create.execute();

        //track INSERT
        StatTrackingPreparedStatement insert = (StatTrackingPreparedStatement) conn.prepareStatement("INSERT INTO stats_test(col1) VALUES(?)");
        insert.setInt(1, 1);
        insert.setTracker(tracker);
        insert.execute();

        //track SELECT
        StatTrackingPreparedStatement select = (StatTrackingPreparedStatement) conn.prepareStatement("SELECT * FROM stats_test");
        select.setTracker(tracker);
        select.execute();

        //track UPDATE
        StatTrackingPreparedStatement update = (StatTrackingPreparedStatement) conn.prepareStatement("UPDATE stats_test SET col1 = ?");
        update.setInt(1, 10);
        update.setTracker(tracker);
        update.execute();

        //track DELETE
        StatTrackingPreparedStatement delete = (StatTrackingPreparedStatement) conn.prepareStatement("DELETE FROM stats_test");
        delete.setTracker(tracker);
        delete.execute();

        //delete the table (tracks "other" again)
        StatTrackingPreparedStatement drop = (StatTrackingPreparedStatement) conn.prepareStatement("DROP TABLE stats_test");
        drop.setTracker(tracker);
        drop.execute();

        //get the statistics
        ArrayList<String> datalines = (ArrayList<String>) tracker.getDataLines();

        //drop the last column, since average run time can vary. this leaves only name and count.
        ArrayList<String> results = new ArrayList<String>();
        for (String line: datalines) {
            results.add(Joiner.on(",").join(Arrays.asList(line.split(",")).subList(0, 2)));
        }
        //sort alphabetically, since the lines are not returned in any particular order
        Collections.sort(results);

        //the expected lines, also in alphabetical order
        List<String> expected = Arrays.asList(new String[] {"DELETE,1", "INSERT,1", "SELECT,1", "UPDATE,1", "other,2"});
        assertEquals(expected, results);
    }

    @After
    public void shutdown() throws Exception {
        DbPool.shutdown();
    }
}
