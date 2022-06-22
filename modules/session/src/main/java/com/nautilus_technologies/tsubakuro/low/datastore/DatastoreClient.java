package com.nautilus_technologies.tsubakuro.low.datastore;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.nautilus_technologies.tsubakuro.low.common.Session;
import com.nautilus_technologies.tsubakuro.exception.ServerException;
import com.nautilus_technologies.tsubakuro.util.FutureResponse;
import com.nautilus_technologies.tsubakuro.util.ServerResource;
import com.nautilus_technologies.tsubakuro.impl.low.datastore.DatastoreClientImpl;

/**
 * A datastore service client.
 * @see #attach(Session)
 */
public interface DatastoreClient extends ServerResource {

    /**
     * Attaches to the datastore service in the current session.
     * @param session the current session
     * @return the datastore service client
     */
    static DatastoreClient attach(@Nonnull Session session) {
        Objects.requireNonNull(session);
        return DatastoreClientImpl.attach(session);
    }

    /**
     * Starts backup.
     * @return the future response of started backup session
     * @throws IOException if I/O error was occurred while sending request
     */
    default FutureResponse<Backup> beginBackup() throws IOException {
        return beginBackup(null);
    }

    /**
     * Starts backup.
     * @param label the optional job label
     * @return the future response of started backup session
     * @throws IOException if I/O error was occurred while sending request
     */
    default FutureResponse<Backup> beginBackup(@Nullable String label) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Estimates backup.
     * @return the future response of backup estimation
     * @throws IOException if I/O error was occurred while sending request
     */
    default FutureResponse<BackupEstimate> estimateBackup() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns all available Point-in-Time recovery tags.
     * @return the future response of available tag list
     * @throws IOException if I/O error was occurred while sending request
     */
    default FutureResponse<List<Tag>> listTag() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns all available Point-in-Time recovery tags.
     * @param name the target tag name
     * @return the future response of target tag, or empty if there is no such the tag
     * @throws IOException if I/O error was occurred while sending request
     */
    default FutureResponse<Optional<Tag>> getTag(@Nonnull String name) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Registers a new Point-in-Time recovery tag.
     * @param name the tag name
     * @param comment the tag comment
     * @return the future response of added tag
     * @throws IOException if I/O error was occurred while sending request
     */
    default FutureResponse<Tag> addTag(@Nonnull String name, @Nullable String comment) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns all available Point-in-Time recovery tags.
     * @param name the target tag name
     * @return the future response of target tag, or empty if there is no such the tag
     * @throws IOException if I/O error was occurred while sending request
     */
    default FutureResponse<Boolean> removeTag(@Nonnull String name) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Disposes the underlying server resources.
     * Note that, this never closes the underlying {@link Session}.
     */
    @Override
    default void close() throws ServerException, IOException, InterruptedException {
        // do nothing
    }
}
