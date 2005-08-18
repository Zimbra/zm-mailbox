package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.cs.util.TimeoutMap;

import junit.framework.TestCase;

public class TestTimeoutMap extends TestCase {
    
    public void testTimeoutMap()
    throws Exception {
        ZimbraLog.test.debug("testTimeoutMap()");
        TimeoutMap map = new TimeoutMap(500);
        
        // Add values 1-99, which should all time out.  Test both the put()
        // and putAll methods().
        Map timeouts = new HashMap();
        for (int i = 1; i <= 49; i++) {
            timeouts.put(new Integer(i), new Integer(i));
        }
        map.putAll(timeouts);
        for (int i = 50; i <= 99; i++) {
            map.put(new Integer(i), new Integer(i));
        }
        
        Integer oneHundred = new Integer(100);

        for (int i = 1; i <= 99; i++) {
            Integer test = new Integer(i);
            assertTrue("1: map does not contain key " + test, map.containsKey(test));
            assertTrue("1: map does not contain value " + test, map.containsValue(test));
            assertEquals("1: value for key " + test + " does not match", test, map.get(test));
        }
        
        assertEquals("1: Map size is incorrect", 99, map.size());
        assertFalse("1: map contains key 100", map.containsKey(oneHundred));
        assertFalse("1: map contains value 100", map.containsValue(oneHundred));
        assertNull("1: map value for key 100 is not null", map.get(oneHundred));
        
        Thread.sleep(600);
        map.put(oneHundred, oneHundred);
        
        assertEquals("Map size is incorrect", 1, map.size());
        
        for (int i = 1; i <= 99; i++) {
            Integer test = new Integer(i);
            assertFalse("2: map contains key " + test, map.containsKey(test));
            assertFalse("2: map contains value " + test, map.containsValue(test));
            assertNull("2: value for key " + test + " is not null", map.get(test));
        }
        
        assertTrue("2: map does not contain key 100", map.containsKey(oneHundred));
        assertTrue("2: map does not contain value 100", map.containsValue(oneHundred));
        assertEquals("2: value for key 100 does not match", oneHundred, map.get(oneHundred));
    }
}
