package com.zimbra.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ZMailbox.class, HttpClientUtil.class })
@SuppressStaticInitializationFor("com.zimbra.client.ZMailbox")
public class ZMailboxTest {

    private int originalThreshold;

    @Before
    public void setUp() {
        originalThreshold = LC.zimbra_resturl_export_threshold.intValue();
    }

    @After
    public void tearDown() {
        LC.zimbra_resturl_export_threshold.setDefault(originalThreshold);
    }

    @Test
    public void testGetResourceReturnsInMemoryStreamForSmallPayload() throws Exception {
        LC.zimbra_resturl_export_threshold.setDefault(1);

        byte[] payload = "small payload".getBytes(StandardCharsets.UTF_8);
        Object mailbox = newSpyMailbox();
        URI uri = new URI("http://localhost/small");

        HttpClientBuilder builder = PowerMockito.mock(HttpClientBuilder.class);
        CloseableHttpClient client = PowerMockito.mock(CloseableHttpClient.class);
        HttpResponse response = PowerMockito.mock(HttpResponse.class);
        StatusLine statusLine = PowerMockito.mock(StatusLine.class);
        HttpEntity entity = PowerMockito.mock(HttpEntity.class);

        PowerMockito.doReturn(builder).when((ZMailbox) mailbox).getHttpClientBuilder(uri);
        PowerMockito.when(builder.build()).thenReturn(client);

        PowerMockito.mockStatic(HttpClientUtil.class);
        PowerMockito.when(HttpClientUtil.executeMethod(
                        eq(client),
                        any(org.apache.http.client.methods.HttpGet.class)))
                .thenReturn(response);
        PowerMockito.when(response.getStatusLine()).thenReturn(statusLine);
        PowerMockito.when(statusLine.getStatusCode()).thenReturn(HttpServletResponse.SC_OK);
        PowerMockito.when(response.getStatusLine()).thenReturn(statusLine);
        PowerMockito.when(response.getEntity()).thenReturn(entity);
        PowerMockito.when(entity.getContent()).thenReturn(new ByteArrayInputStream(payload));

        InputStream result = invokeGetResource(mailbox, uri, -1);
        byte[] actual = readAll(result);

        assertArrayEquals(payload, actual);
        assertTrue(result instanceof ByteArrayInputStream);
    }

    @Test
    public void testDeletingFileInputStreamDeletesFileOnClose() throws Exception {
        File dir = new File("/Users/gopal.moolchandani/");
        File temp = File.createTempFile("zmailbox-test-", ".tmp", dir);
        Files.write(temp.toPath(), "temp-data".getBytes(StandardCharsets.UTF_8));
        assertTrue(temp.exists());

        Class<?> deletingStreamClass = Class.forName("com.zimbra.client.ZMailbox$DeletingFileInputStream");
        Constructor<?> ctor = deletingStreamClass.getDeclaredConstructor(File.class);
        ctor.setAccessible(true);

        InputStream in = (InputStream) ctor.newInstance(temp);
        in.close();

        assertFalse(temp.exists());
    }

    private static Object newSpyMailbox() throws Exception {
        Constructor<?> ctor = ZMailbox.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object mailbox = ctor.newInstance();
        return PowerMockito.spy((ZMailbox) mailbox);
    }

    private static InputStream invokeGetResource(Object mailbox, URI uri, int timeout) throws Exception {
        Method method = ZMailbox.class.getDeclaredMethod("getResource", URI.class, int.class);
        method.setAccessible(true);
        return (InputStream) method.invoke(mailbox, uri, timeout);
    }

    private static byte[] readAll(InputStream in) throws Exception {
        try {
            byte[] buffer = new byte[1024];
            int read;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
