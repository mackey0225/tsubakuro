package com.nautilus_technologies.tsubakuro.channel.stream.connection;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nautilus_technologies.tsubakuro.channel.common.connection.Connector;
import com.nautilus_technologies.tsubakuro.channel.common.connection.Credential;
import com.nautilus_technologies.tsubakuro.channel.common.sql.SessionWire;
import com.nautilus_technologies.tsubakuro.channel.stream.StreamWire;
import com.nautilus_technologies.tsubakuro.util.FutureResponse;

/**
 * StreamConnectorImpl type.
 */
public final class StreamConnectorImpl implements Connector {

    private static final Logger LOG = LoggerFactory.getLogger(StreamConnectorImpl.class);

    public static final int DEFAULT_PORT = 12345;
    private final String hostname;
    private final int port;

    public StreamConnectorImpl(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    @Override
    public FutureResponse<SessionWire> connect(Credential credential) throws IOException {
        LOG.trace("will connect to {}:{}", hostname, port); //$NON-NLS-1$

        var streamWire = new StreamWire(hostname, port);
        streamWire.hello();
        return new FutureSessionWireImpl(streamWire);
    }
}
