package com.nautilus_technologies.tsubakuro.channel.common;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nautilus_technologies.tsubakuro.channel.common.wire.Response;
import com.nautilus_technologies.tsubakuro.channel.common.wire.ResponseProcessor;
import com.nautilus_technologies.tsubakuro.exception.ServerException;
import com.nautilus_technologies.tsubakuro.util.FutureResponse;
import com.nautilus_technologies.tsubakuro.util.Owner;

/**
 * A {@link FutureResponse} that converts {@link Response} into specific type in foreground.
 * @param <V> the specified response type
 * @see BackgroundFutureResponse
 */
public class ForegroundFutureResponse<V> implements FutureResponse<V> {  // FIXME remove public

    static final Logger LOG = LoggerFactory.getLogger(ForegroundFutureResponse.class);

    private final FutureResponse<? extends Response> delegate;

    private final ResponseProcessor<? extends V> mapper;

    private final AtomicReference<Response> unprocessed = new AtomicReference<>();

    private final AtomicReference<V> result = new AtomicReference<>();

    /**
     * Creates a new instance.
     * @param delegate the decoration target
     * @param mapper the response mapper
     */
    public ForegroundFutureResponse(
            @Nonnull FutureResponse<? extends Response> delegate,
            @Nonnull ResponseProcessor<? extends V> mapper) {
        Objects.requireNonNull(delegate);
        Objects.requireNonNull(mapper);
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public V get() throws InterruptedException, IOException, ServerException {
        var mapped = result.get();
        if (mapped != null) {
            return mapped;
        }
        return processResult(getInternal());
    }

    @Override
    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, IOException, ServerException, TimeoutException {
        var mapped = result.get();
        if (mapped != null) {
            return mapped;
        }
        return processResult(getInternal(timeout, unit));
    }

    private Owner<Response> getInternal() throws InterruptedException, IOException, ServerException {
        if (!mapper.isMainResponseRequired()) {
            return Owner.of(delegate.get());
        }
        try (Owner<Response> response = Owner.of(delegate.get())) {
            response.get().waitForMainResponse();
            return response.move();
        }
    }

    private Owner<Response> getInternal(long timeout, TimeUnit unit)
            throws InterruptedException, IOException, ServerException, TimeoutException {
        if (!mapper.isMainResponseRequired()) {
            return Owner.of(delegate.get(timeout, unit));
        }
        long timeoutMillis = Math.max(unit.toMillis(timeout), 2);
        try (Owner<Response> response = Owner.of(delegate.get(timeoutMillis / 2, TimeUnit.MILLISECONDS))) {
            try {
                response.get().waitForMainResponse(timeoutMillis / 2, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                unprocessed.set(response.release());
                throw e;
            }
            unprocessed.set(null);
            return response.move();
        }
    }

    private V processResult(Owner<Response> response) throws IOException, ServerException, InterruptedException {
        assert response != null;
        try (response) {
            V mapped;
            synchronized (this) {
                mapped = result.get();
                if (mapped != null) {
                    return mapped;
                }
                LOG.trace("mapping response: {}", response.get()); //$NON-NLS-1$
                mapped = mapper.process(response.get());
                LOG.trace("response mapped: {}", mapped); //$NON-NLS-1$
                result.set(mapped);
            }
            // don't close the original response only if mapping was succeeded
            response.release();
            return mapped;
        }
    }

    @Override
    public boolean isDone() {
        return delegate.isDone();
    }

    @Override
    public void close() throws IOException, ServerException, InterruptedException {
        Owner.close(unprocessed.getAndSet(null));
        delegate.close();
    }

    @Override
    public String toString() {
        return String.valueOf(delegate);
    }
}
