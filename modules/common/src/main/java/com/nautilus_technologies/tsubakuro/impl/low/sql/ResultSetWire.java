package com.nautilus_technologies.tsubakuro.impl.low.sql;

import java.util.concurrent.Future;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import com.nautilus_technologies.tsubakuro.low.sql.SchemaProtos;

/**
 * ResultSetWire type.
 */
public interface ResultSetWire extends Closeable {
    SchemaProtos.RecordMeta receiveSchemaMetaData() throws IOException;

    abstract class MessagePackInputStream extends InputStream {
	public abstract void dispose(long length);
    }

    MessagePackInputStream getMessagePackInputStream();
}
