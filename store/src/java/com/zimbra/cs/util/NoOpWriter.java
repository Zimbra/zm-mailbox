package com.zimbra.cs.util;

import com.zimbra.cs.imap.ImapFolder;
import org.ehcache.spi.loaderwriter.BulkCacheLoadingException;
import org.ehcache.spi.loaderwriter.BulkCacheWritingException;
import org.ehcache.spi.loaderwriter.CacheLoaderWriter;

import java.util.Collections;
import java.util.Map;

public class NoOpWriter implements CacheLoaderWriter<String, ImapFolder> {

    @Override public void write(String key, ImapFolder value) {
        // No-Op: Data stays in Ehcache tiers only
    }

    @Override public void writeAll(Iterable<? extends Map.Entry<? extends String, ? extends ImapFolder>> entries) {
        // No-Op: Data stays in Ehcache tiers only
    }

    @Override public void delete(String key) {
        // No-Op: Data stays in Ehcache tiers only
    }

    @Override
    public void deleteAll(Iterable<? extends String> iterable) throws BulkCacheWritingException, Exception {
        // No-Op: Data stays in Ehcache tiers only
    }

    @Override public ImapFolder load(String key) {
        // No-Op: Data stays in Ehcache tiers only
        return null;
    }

    @Override
    public Map<String, ImapFolder> loadAll(Iterable<? extends String> iterable)
            throws BulkCacheLoadingException, Exception {
        // No-Op: Data stays in Ehcache tiers only
        return Collections.emptyMap();
    }
}
