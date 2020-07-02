package com.zimbra.cs.redolog.op;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.activation.DataSource;

public interface BlobRecorder {

    public String getBlobDigest();

    public InputStream getBlobInputStream() throws IOException;

    public void setBlobDataFromDataSource(DataSource ds, long size);

    public void setBlobDataFromFile(File file);

    public long getBlobSize();

    public Set<Integer> getReferencedMailboxIds();
}
