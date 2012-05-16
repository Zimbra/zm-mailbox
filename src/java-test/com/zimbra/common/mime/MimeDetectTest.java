package com.zimbra.common.mime;

import org.junit.Assert;
import org.junit.Test;

public class MimeDetectTest {

    @Test
    public void testFileName() {
        Assert.assertEquals("image/jpeg", MimeDetect.getMimeDetect().detect("2011.07.19 089+.JPG"));
        Assert.assertEquals("image/jpeg", MimeDetect.getMimeDetect().detect("2011.07.18 706+.jpg"));
        Assert.assertEquals("image/jpeg", MimeDetect.getMimeDetect().detect("2011.07.18 706+.jPg"));
    }
}
