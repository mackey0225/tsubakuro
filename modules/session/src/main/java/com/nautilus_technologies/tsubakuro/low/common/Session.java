package com.nautilus_technologies.tsubakuro.low.common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import com.nautilus_technologies.tsubakuro.channel.common.SessionWire;  // FIXME shoule delete
// import com.nautilus_technologies.tsubakuro.channel.common.wire.Wire;  // FIXME shoule use it
import com.nautilus_technologies.tsubakuro.util.ServerResource;
import com.nautilus_technologies.tsubakuro.channel.common.wire.Response;
import com.nautilus_technologies.tsubakuro.channel.common.wire.ResponseProcessor;
import com.nautilus_technologies.tsubakuro.channel.common.ForegroundFutureResponse;  // FIXME move Session.java to com.nautilus_technologies.tsubakuro.channel.common
import com.nautilus_technologies.tsubakuro.channel.common.BackgroundFutureResponse;  // FIXME same
import com.nautilus_technologies.tsubakuro.util.FutureResponse;
import com.nautilus_technologies.tsubakuro.util.Timeout;

/**
 * Represents a connection to Tsurugi server.
 */
@ThreadSafe
public class Session implements ServerResource {

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {

        private final AtomicInteger serial = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName(String.format("tsubakuro-worker-%04d", serial.incrementAndGet())); //$NON-NLS-1$
            t.setDaemon(true);
            return t;
        }
    };

//    private final Wire wire;
    public SessionWire wire;  // FIXME use Wire

    private final ExecutorService executor;

    /**
     * Creates a new instance.
     * @param wire the underlying wire
     */
    public Session(@Nonnull SessionWire wire) {
        this(wire, Executors.newCachedThreadPool(THREAD_FACTORY));
    }

    /**
     * Creates a new instance.
     * @param wire the underlying wire
     * @param executor worker threads to process responses
     */
    public Session(@Nonnull SessionWire wire, @Nonnull ExecutorService executor) {
        Objects.requireNonNull(wire);
        Objects.requireNonNull(executor);
        this.wire = wire;
        this.executor = executor;
    }

    /**
     * Sends a message to the destination server.
     * @param <R> the result value type
     * @param serviceId the destination service ID
     * @param payload the message payload
     * @param processor the response processor
     * @return the future of response
     * @throws IOException if I/O error was occurred while requesting
     */
    public <R> FutureResponse<R> send(
            int serviceId,
            @Nonnull byte[] payload,
            @Nonnull ResponseProcessor<R> processor) throws IOException {
        return send(serviceId, payload, processor, false);
    }

    /**
     * Sends a message to the destination server.
     * @param <R> the result value type
     * @param serviceId the destination service ID
     * @param payload the message payload
     * @param processor the response processor
     * @return the future of response
     * @throws IOException if I/O error was occurred while requesting
     */
    public <R> FutureResponse<R> send(
            int serviceId,
            @Nonnull ByteBuffer payload,
            @Nonnull ResponseProcessor<R> processor) throws IOException {
        return send(serviceId, payload, processor, false);
    }

    /**
     * Sends a message to the destination server.
     * @param <R> the result value type
     * @param serviceId the destination service ID
     * @param payload the message payload
     * @param processor the future response processor
     * @param background whether or not process responses in back ground
     * @return the future of response
     * @throws IOException if I/O error was occurred while requesting
     */
    public <R> FutureResponse<R> send(
            int serviceId,
            @Nonnull byte[] payload,
            @Nonnull ResponseProcessor<R> processor,
            boolean background) throws IOException {
        Objects.requireNonNull(payload);
        Objects.requireNonNull(processor);

        if (Objects.isNull(wire)) {
            System.out.println("wire is null");
        }
        FutureResponse<? extends Response> future = wire.send(serviceId, payload);
        return convert(future, processor, background);
    }

    /**
     * Sends a message to the destination server.
     * @param <R> the result value type
     * @param serviceId the destination service ID
     * @param payload the message payload
     * @param processor the future response processor
     * @param background whether or not process responses in back ground
     * @return the future of response
     * @throws IOException if I/O error was occurred while requesting
     */
    public <R> FutureResponse<R> send(
            int serviceId,
            @Nonnull ByteBuffer payload,
            @Nonnull ResponseProcessor<R> processor,
            boolean background) throws IOException {
        Objects.requireNonNull(payload);
        Objects.requireNonNull(processor);
        FutureResponse<? extends Response> future = wire.send(serviceId, payload);
        return convert(future, processor, background);
    }

    private <R> FutureResponse<R> convert(
            @Nonnull FutureResponse<? extends Response> response,
            @Nonnull ResponseProcessor<R> processor,
            boolean background) {
        assert response != null;
        assert processor != null;
        if (background) {
            var f = new BackgroundFutureResponse<>(response, processor);
            executor.execute(f); // process in background thread
            return f;
        }
        return new ForegroundFutureResponse<>(response, processor);
    }

    @Override
    public void setCloseTimeout(@Nonnull Timeout timeout) {
        Objects.requireNonNull(timeout);
    }

    @Override
    public void close() throws IOException, InterruptedException {
        // FIXME: close timeout
        if (Objects.nonNull(executor)) {
            executor.shutdownNow();
        }
        if (Objects.nonNull(wire)) {
            wire.close();
        }
    }



    /**
     * Creates a new instance, exist for SessionImpl.
     */
    public Session() {
        wire = null;
        executor = null;
    }

    /**
     * Connect this session to the Database
     * @param sessionWire the wire that connects to the Database
     */
    public void connect(SessionWire sessionWire) {
    }

    /**
     * Send a request via sessionWire
     * @param id identifies the service
     * @param request the request to the service
     * @return a FutureInputStream for response
     * @throws IOException error occurred in send
     */
    public FutureResponse<? extends Response> send(long id, byte[] request) throws IOException {
        return null;
    }

    /**
     * Provides wire to tha caller, exists as a temporal measure for sessionLink
     * @return the wire that this session uses
     */
    public SessionWire getWire() {
        return wire;
    }
}
