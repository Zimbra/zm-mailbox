package com.zimbra.cs.octosync;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents patch manifest, i.e. summary information about references
 * to all files an indvidual patch is making.
 *
 * @author grzes
 */
public class PatchManifest
{
    private final class Reference
    {
        int fileId;
        int version;

        public Reference(int fileId, int version)
        {
            this.fileId = fileId;
            this.version = version;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof Reference) {
                Reference other = (Reference)obj;
                return other.fileId == fileId && other.version == version;
            } else {
                return super.equals(obj);
            }
        }

        @Override
        public int hashCode()
        {
            return fileId ^ version;
        }
    }

    private Map<Reference, Long> references;

    /**
     * Constructor.
     */
    public PatchManifest()
    {
        references = new HashMap<Reference, Long>();
    }

    /**
     * Add reference to the manifest.
     *
     * @param fileId File Id. Must be greater than 0.
     * @param version File version. Must be greater than 0.
     * @param numBytes Number of bytes referenced. Must be greater than 0.
     */
    public void addReference(int fileId, int version, int numBytes)
    {
        assert fileId > 0 : "File id must be > 0";
        assert version > 0 : "Version must be > 0";
        assert numBytes > 0 : "References cannot refer to 0 bytes";

        Reference ref = new Reference(fileId, version);

        long totalBytesSoFar = numBytes;

        Long currentTotal = references.remove(ref);

        if (currentTotal != null) {
            totalBytesSoFar += currentTotal.longValue();
        }

        references.put(ref, new Long(totalBytesSoFar));
    }

    /**
     * Writes the manifest in the binary format
     * to the specified OutputStream
     *
     * @param os OutputStream to write
     *
     * @throws IOException
     */
    public void writeTo(OutputStream os) throws IOException
    {
        BinaryWriterHelper helper = new BinaryWriterHelper(os);

        os.write("OPMANI".getBytes());
        helper.writeShort((short)0);
        helper.writeInt(references.size());

        for (Entry<Reference, Long> entry : references.entrySet())
        {
            helper.writeInt(entry.getKey().fileId);
            helper.writeInt(entry.getKey().version);
            helper.writeLong(entry.getValue().longValue());
        }
    }
}
