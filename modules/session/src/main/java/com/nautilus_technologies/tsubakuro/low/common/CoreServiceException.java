package com.nautilus_technologies.tsubakuro.low.common;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.nautilus_technologies.tsubakuro.exception.ServerException;

/**
 * An exception which occurs on datastore service.
 */
public class CoreServiceException extends ServerException {

    private static final long serialVersionUID = 1L;

    private final CoreServiceCode code;

    /**
     * Creates a new instance.
     * @param code the diagnostic code
     * @param message the error message
     * @param cause the original cause
     */
    public CoreServiceException(@Nonnull CoreServiceCode code, @Nullable String message, @Nullable Throwable cause) {
        super(buildMessage(code, message), cause);
        this.code = code;
    }

    private static String buildMessage(CoreServiceCode code, @Nullable String message) {
        Objects.requireNonNull(code);
        if (message == null) {
            return code.getStructuredCode();
        }
        return String.format("%s: %s", code.getStructuredCode(), message); //$NON-NLS-1$
    }

    /**
     * Creates a new instance.
     * @param code the diagnostic code
     */
    public CoreServiceException(@Nonnull CoreServiceCode code) {
        this(code, null, null);
    }

    /**
     * Creates a new instance.
     * @param code the diagnostic code
     * @param message the error message
     */
    public CoreServiceException(@Nonnull CoreServiceCode code, @Nullable String message) {
        this(code, message, null);
    }

    /**
     * Creates a new instance.
     * @param code the diagnostic code
     * @param cause the original cause
     */
    public CoreServiceException(@Nonnull CoreServiceCode code, @Nullable Throwable cause) {
        this(code, null, cause);
    }

    @Override
    public CoreServiceCode getDiagnosticCode() {
        return code;
    }
}
