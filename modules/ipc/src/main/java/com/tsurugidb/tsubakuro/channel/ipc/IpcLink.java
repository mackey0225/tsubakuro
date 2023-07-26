package com.tsurugidb.tsubakuro.channel.ipc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tsubakuro.channel.common.connection.wire.impl.Link;
import com.tsurugidb.tsubakuro.channel.common.connection.wire.impl.LinkMessage;
import com.tsurugidb.tsubakuro.channel.common.connection.wire.impl.ChannelResponse;
import com.tsurugidb.tsubakuro.channel.common.connection.sql.ResultSetWire;
import com.tsurugidb.tsubakuro.channel.ipc.sql.ResultSetWireImpl;
import com.tsurugidb.tsubakuro.exception.ResponseTimeoutException;

/**
 * IpcLink type.
 */
public final class IpcLink extends Link {
    private long wireHandle = 0;  // for c++
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean serverDown = new AtomicBoolean();
    private Receiver receiver;

    public static final byte RESPONSE_NULL = 0;
    public static final byte RESPONSE_PAYLOAD = 1;
    public static final byte RESPONSE_BODYHEAD = 2;
    public static final byte RESPONSE_CODE = 3;

    private static native long openNative(String name) throws IOException;
    private static native void sendNative(long wireHandle, int slot, byte[] message);
    private static native int awaitNative(long wireHandle) throws IOException;
    private static native int getInfoNative(long wireHandle);
    private static native byte[] receiveNative(long wireHandle);
    private static native boolean isAliveNative(long wireHandle);
    private static native void closeNative(long wireHandle);
    private static native void destroyNative(long wireHandle);

    static final Logger LOG = LoggerFactory.getLogger(IpcLink.class);

    static {
        NativeLibrary.load();
    }

    private class Receiver extends Thread {
        public void run() {
            while (true) {
                if (!pull()) {
                    break;
                }
            }
        }
    }

    /**
     * Class constructor, called from IpcConnectorImpl that is a connector to the SQL server.
     * @param name the name of shared memory for this IpcLink through which the SQL server is connected
     * @param sessionID the id of this session obtained by the connector requesting a connection to the SQL server
     * @throws IOException error occurred in openNative()
     */
    public IpcLink(@Nonnull String name) throws IOException {
        this.wireHandle = openNative(name);
        this.receiver = new Receiver();
        receiver.start();
        LOG.trace("begin Session via shared memory, name = {}", name);
    }

    @Override
    public void send(int s, @Nonnull byte[] frameHeader, @Nonnull byte[] payload, @Nonnull ChannelResponse channelResponse) {
        if (serverDown.get()) {
            channelResponse.setMainResponse(new IOException("Link already closed"));
            return;
        }
        byte[] message = new byte[frameHeader.length + payload.length];
        System.arraycopy(frameHeader, 0, message, 0, frameHeader.length);
        System.arraycopy(payload, 0, message, frameHeader.length, payload.length);

        synchronized (this) {
            if (!closed.get()) {
                sendNative(wireHandle, s, message);
            } else {
                channelResponse.setMainResponse(new IOException("Link already closed"));
                return;
            }
        }
        LOG.trace("send {}", payload);
    }

    private boolean pull() {
        LinkMessage message = null;
        boolean intentionalClose = true;
        try {
            message = receive();
        } catch (IOException e) {
            intentionalClose = false;
        }

        if (Objects.nonNull(message)) {
            try {
                if (message.getInfo() != RESPONSE_NULL) {
                    if (message.getInfo() == RESPONSE_BODYHEAD) {
                        responseBox.pushHead(message.getSlot(), message.getBytes(), createResultSetWire());
                    } else {
                        responseBox.push(message.getSlot(), message.getBytes());
                    }
                    return true;
                }
                return false;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        // link is closed
        if (!intentionalClose) {
            serverDown.set(true);
        }
        responseBox.doClose(intentionalClose);
        return false;
    }

    private LinkMessage receive() throws IOException {
        int slot = awaitNative(wireHandle);
        if (slot >= 0) {
            var info = (byte) getInfoNative(wireHandle);
            return new LinkMessage(info, receiveNative(wireHandle), slot);
        }
        return null;
    }

    @Override
    public ResultSetWire createResultSetWire() throws IOException {
        synchronized (this) {
            if (closed.get()) {
                throw new IOException("Link already closed");
            }
            return new ResultSetWireImpl(wireHandle);
        }
    }

    @Override
    public boolean isAlive() {
        if (closed.get() || (wireHandle == 0)) {
            return false;
        }
        return isAliveNative(wireHandle);
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            if (!closed.getAndSet(true)) {
                closeNative(wireHandle);
                try {
                    if (timeout != 0) {
                        timeUnit.timedJoin(receiver, timeout);
                    } else {
                        receiver.join();
                    }
                    if (receiver.getState() != Thread.State.TERMINATED) {
                        receiver.interrupt();
                        throw new ResponseTimeoutException(new TimeoutException("close timeout in StreamLink"));
                    }
                } catch (InterruptedException e) {
                    throw new IOException(e);
                } finally {
                    destroyNative(wireHandle);
                }
            }
        }
    }
}
