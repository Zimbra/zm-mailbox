package com.zimbra.qa.unittest;

import com.zimbra.cs.util.LiquidLog;
import com.zimbra.cs.util.TimeoutMap;

import junit.framework.TestCase;

public class TestTimeoutMap extends TestCase {
    
    public void testTimeoutMap()
    throws Exception {
        LiquidLog.test.debug("testTimeoutMap()");
        TimeoutMap map = new TimeoutMap(200);
        
        Integer one = new Integer(1);
        Integer two = new Integer(2);
        String oneString = "one";
        String twoString = "two";
        
        map.put(one, oneString);
        assertTrue("1: map does not contain key 1", map.containsKey(one));
        assertTrue("1: map does not contain value 'one'", map.containsValue(oneString));
        assertEquals("1: value for key 1 does not match", oneString, map.get(one));
        assertFalse("1: map contains key 2", map.containsKey(two));
        assertFalse("1: map contains value 'two'", map.containsValue(twoString));
        assertNull("1: map value for 2 is not null", map.get(two));
        
        Thread.sleep(300);
        map.put(two, twoString);
        
        assertFalse("2: map contains key 1", map.containsKey(one));
        assertFalse("2: map contains value 'one'", map.containsValue(oneString));
        assertNull("2: value for key 1 is not null", map.get(one));
        assertTrue("2: map does not contain key 2", map.containsKey(two));
        assertTrue("2: map does not contain value 'two'", map.containsValue(twoString));
        assertEquals("2: value for key 2 does not match", twoString, map.get(two));
    }
}
