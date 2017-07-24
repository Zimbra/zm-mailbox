package com.zimbra.cs.redolog;

import com.zimbra.common.service.ServiceException;
import junit.framework.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class TransactionIdTest {
    @Test
    public void defaultId() throws Exception {
        TransactionId id = new TransactionId();
        Assert.assertEquals(id, id);
        Assert.assertEquals(0, id.hashCode());
        Assert.assertEquals(0, id.getTime());
        Assert.assertEquals(0, id.getCounter());
        Assert.assertEquals("0-0", id.encodeToString());
    }

    public void id() throws Exception {
        TransactionId id = new TransactionId(1112, 5);
        Assert.assertEquals(id, id);
        Assert.assertEquals(5, id.hashCode());
        Assert.assertEquals(1112, id.getTime());
        Assert.assertEquals(5, id.getCounter());
        Assert.assertEquals("1112-5", id.encodeToString());
    }

    @Test
    public void stringEncodeDecode() throws Exception {
        TransactionId id = new TransactionId(5, 188);
        String encoded = id.encodeToString();
        Assert.assertEquals("5-188", encoded);
        Assert.assertEquals("mismatch on decode.",
                            id, TransactionId.decodeFromString(encoded));
    }

    @Test(expected = ServiceException.class)
    public void stringBadDecode() throws Exception {
        TransactionId.decodeFromString("not-valid");
    }

    @Test
    public void streamEncodeDecode() throws Exception {
        TransactionId id = new TransactionId(5, 188);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        RedoLogOutput redoOut = new RedoLogOutput(os);

        id.serialize(redoOut);
        Assert.assertEquals(8, os.size());

        RedoLogInput redoIn =
            new RedoLogInput(new ByteArrayInputStream(os.toByteArray()));

        TransactionId newId = new TransactionId();
        newId.deserialize(redoIn);

        Assert.assertEquals("mismatch on deserialize", id, newId);
    }
}
