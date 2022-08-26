package com.nautilus_technologies.tsubakuro.impl.low.datastore;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nautilus_technologies.tsubakuro.low.common.Session;
import com.nautilus_technologies.tsubakuro.channel.common.connection.wire.MainResponseProcessor;
import com.nautilus_technologies.tateyama.proto.DatastoreCommonProtos;
import com.nautilus_technologies.tateyama.proto.DatastoreRequestProtos;
import com.nautilus_technologies.tateyama.proto.DatastoreResponseProtos;
import com.nautilus_technologies.tsubakuro.low.datastore.Backup;
// import com.nautilus_technologies.tsubakuro.low.datastore.BackupEstimate;
import com.nautilus_technologies.tsubakuro.low.datastore.DatastoreService;
import com.nautilus_technologies.tsubakuro.low.datastore.DatastoreServiceCode;
import com.nautilus_technologies.tsubakuro.low.datastore.DatastoreServiceException;
import com.nautilus_technologies.tsubakuro.low.datastore.Tag;
import com.nautilus_technologies.tsubakuro.exception.BrokenResponseException;
import com.nautilus_technologies.tsubakuro.exception.ServerException;
import com.nautilus_technologies.tsubakuro.util.FutureResponse;
import com.nautilus_technologies.tsubakuro.util.ServerResourceHolder;
import com.nautilus_technologies.tsubakuro.util.ByteBufferInputStream;
import com.google.protobuf.Message;

/**
 * An implementation of {@link DatastoreService} communicate to the datastore service.
 */
public class DatastoreServiceStub implements DatastoreService {

    static final Logger LOG = LoggerFactory.getLogger(DatastoreServiceStub.class);

    /**
     * The datastore service ID.
     */
    public static final int SERVICE_ID = Constants.SERVICE_ID_BACKUP;

    private final Session session;

    private final ServerResourceHolder resources = new ServerResourceHolder();

    /**
     * Creates a new instance.
     * @param session the current session
     */
    public DatastoreServiceStub(@Nonnull Session session) {
        Objects.requireNonNull(session);
        this.session = session;
    }

    static DatastoreServiceException newUnknown(@Nonnull DatastoreResponseProtos.UnknownError message) {
        assert message != null;
        return new DatastoreServiceException(DatastoreServiceCode.UNKNOWN, message.getMessage());
    }

    static BrokenResponseException newResultNotSet(
            @Nonnull Class<? extends Message> aClass, @Nonnull String name) {
        assert aClass != null;
        assert name != null;
        return new BrokenResponseException(MessageFormat.format(
                "{0}.{1} is not set",
                aClass.getSimpleName(),
                name));
    }

    static Tag convert(@Nonnull DatastoreCommonProtos.Tag tag) {
        assert tag != null;
        return new Tag(
                tag.getName(),
                optional(tag.getComment()),
                optional(tag.getAuthor()),
                Instant.ofEpochMilli(tag.getTimestamp()));
    }

    private static @Nullable String optional(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value;
    }

    class BackupBeginProcessor implements MainResponseProcessor<Backup> {
        @Override
        public Backup process(ByteBuffer payload) throws IOException, ServerException, InterruptedException {
            var message = DatastoreResponseProtos.BackupBegin.parseDelimitedFrom(new ByteBufferInputStream(payload));
            LOG.trace("receive: {}", message); //$NON-NLS-1$
            switch (message.getResultCase()) {
            case SUCCESS:
                var backupId = message.getSuccess().getId();
                var files = new ArrayList<Path>();
                for (var f : message.getSuccess().getFilesList()) {
                    files.add(Path.of(f));
                }
                return resources.register(new BackupImpl(DatastoreServiceStub.this, resources, backupId, files));

            case UNKNOWN_ERROR:
                throw newUnknown(message.getUnknownError());

            case RESULT_NOT_SET:
                throw newResultNotSet(message.getClass(), "result"); //$NON-NLS-1$

            default:
                break;
            }
            throw new AssertionError(); // may not occur
        }
    }

    @Override
    public FutureResponse<Backup> send(@Nonnull DatastoreRequestProtos.BackupBegin request) throws IOException {
        LOG.trace("send: {}", request); //$NON-NLS-1$
        return session.send(
            SERVICE_ID,
            toDelimitedByteArray(DatastoreRequestProtos.Request.newBuilder()
                                 .setMessageVersion(Constants.MESSAGE_VERSION)
                                 .setBackupBegin(request)
                                 .build()),
            new BackupBeginProcessor().asResponseProcessor());
    }

    static class BackupEndProcessor implements MainResponseProcessor<Void> {
        @Override
        public Void process(ByteBuffer payload) throws IOException, ServerException, InterruptedException {
            var message = DatastoreResponseProtos.BackupEnd.parseDelimitedFrom(new ByteBufferInputStream(payload));
            LOG.trace("receive: {}", message); //$NON-NLS-1$
            switch (message.getResultCase()) {
            case SUCCESS:
                return null;

            case EXPIRED:
                throw new DatastoreServiceException(DatastoreServiceCode.BACKUP_EXPIRED);

            case UNKNOWN_ERROR:
                throw newUnknown(message.getUnknownError());

            case RESULT_NOT_SET:
                throw newResultNotSet(message.getClass(), "result"); //$NON-NLS-1$

            default:
                break;
            }
            throw new AssertionError(); // may not occur
        }
    }

    @Override
    public FutureResponse<Void> send(@Nonnull DatastoreRequestProtos.BackupEnd request) throws IOException {
        LOG.trace("send: {}", request); //$NON-NLS-1$
        return session.send(
            SERVICE_ID,
            toDelimitedByteArray(DatastoreRequestProtos.Request.newBuilder()
                                 .setMessageVersion(Constants.MESSAGE_VERSION)
                                 .setBackupEnd(request)
                                 .build()),
            new BackupEndProcessor().asResponseProcessor());
    }

    @Override
    public FutureResponse<Void> updateExpirationTime(long time, @Nonnull TimeUnit unit) throws IOException {
        return session.updateExpirationTime(time, unit);
    }

    static class TagListProcessor implements MainResponseProcessor<List<Tag>> {
        @Override
        public List<Tag> process(ByteBuffer payload) throws IOException, ServerException, InterruptedException {
            var message = DatastoreResponseProtos.TagList.parseFrom(payload);
            LOG.trace("receive: {}", message); //$NON-NLS-1$
            switch (message.getResultCase()) {
            case SUCCESS:
                var list = new ArrayList<Tag>(message.getSuccess().getTagsCount());
                for (var tag : message.getSuccess().getTagsList()) {
                    list.add(convert(tag));
                }
                return Collections.unmodifiableList(list);

            case UNKNOWN_ERROR:
                throw newUnknown(message.getUnknownError());

            case RESULT_NOT_SET:
                throw newResultNotSet(message.getClass(), "result"); //$NON-NLS-1$

            default:
                break;
            }
            throw new AssertionError(); // may not occur
        }
    }

    @Override
    public FutureResponse<List<Tag>> send(@Nonnull DatastoreRequestProtos.TagList request) throws IOException {
        LOG.trace("send: {}", request); //$NON-NLS-1$
        return session.send(
                SERVICE_ID,
                toDelimitedByteArray(DatastoreRequestProtos.Request.newBuilder()
                                     .setTagList(request)
                                     .build()),
                new TagListProcessor().asResponseProcessor());
    }

    static class TagAddProcessor implements MainResponseProcessor<Tag> {
        @Override
        public Tag process(ByteBuffer payload) throws IOException, ServerException, InterruptedException {
            var message = DatastoreResponseProtos.TagAdd.parseFrom(payload);
            LOG.trace("receive: {}", message); //$NON-NLS-1$
            switch (message.getResultCase()) {
            case SUCCESS:
                return convert(message.getSuccess().getTag());

            case ALREADY_EXISTS:
                throw new DatastoreServiceException(
                        DatastoreServiceCode.TAG_ALREADY_EXISTS,
                        MessageFormat.format(
                                "tag is already exists: '{0}'",
                                message.getAlreadyExists().getName()));

            case TOO_LONG_NAME:
                throw new DatastoreServiceException(
                        DatastoreServiceCode.TAG_NAME_TOO_LONG,
                        MessageFormat.format(
                                "tag name length is exceeded (max {1} characters): '{0}'",
                                message.getTooLongName().getName(),
                                message.getTooLongName().getMaxCharacters()));

            case UNKNOWN_ERROR:
                throw newUnknown(message.getUnknownError());

            case RESULT_NOT_SET:
                throw newResultNotSet(message.getClass(), "result"); //$NON-NLS-1$

            default:
                break;
            }
            throw new AssertionError(); // may not occur
        }
    }

    @Override
    public FutureResponse<Tag> send(@Nonnull DatastoreRequestProtos.TagAdd request) throws IOException {
        LOG.trace("send: {}", request); //$NON-NLS-1$
        return session.send(
                SERVICE_ID,
                toDelimitedByteArray(DatastoreRequestProtos.Request.newBuilder()
                                     .setTagAdd(request)
                                     .build()),
                new TagAddProcessor().asResponseProcessor());
    }

    static class TagGetProcessor implements MainResponseProcessor<Optional<Tag>> {
        @Override
        public Optional<Tag> process(ByteBuffer payload) throws IOException, ServerException, InterruptedException {
            var message = DatastoreResponseProtos.TagGet.parseFrom(payload);
            LOG.trace("receive: {}", message); //$NON-NLS-1$
            switch (message.getResultCase()) {
            case SUCCESS:
                return Optional.of(convert(message.getSuccess().getTag()));

            case NOT_FOUND:
                return Optional.empty();

            case UNKNOWN_ERROR:
                throw newUnknown(message.getUnknownError());

            case RESULT_NOT_SET:
                throw newResultNotSet(message.getClass(), "result"); //$NON-NLS-1$

            default:
                break;
            }
            throw new AssertionError(); // may not occur
        }
    }

    @Override
    public FutureResponse<Optional<Tag>> send(@Nonnull DatastoreRequestProtos.TagGet request) throws IOException {
        LOG.trace("send: {}", request); //$NON-NLS-1$
        return session.send(
                SERVICE_ID,
                toDelimitedByteArray(DatastoreRequestProtos.Request.newBuilder()
                                     .setTagGet(request)
                                     .build()),
                new TagGetProcessor().asResponseProcessor());
    }

    static class TagRemoveProcessor implements MainResponseProcessor<Boolean> {
        @Override
        public Boolean process(ByteBuffer payload) throws IOException, ServerException, InterruptedException {
            var message = DatastoreResponseProtos.TagRemove.parseFrom(payload);
            LOG.trace("receive: {}", message); //$NON-NLS-1$
            switch (message.getResultCase()) {
            case SUCCESS:
                return Boolean.TRUE;

            case NOT_FOUND:
                return Boolean.FALSE;

            case UNKNOWN_ERROR:
                throw newUnknown(message.getUnknownError());

            case RESULT_NOT_SET:
                throw newResultNotSet(message.getClass(), "result"); //$NON-NLS-1$

            default:
                break;
            }
            throw new AssertionError(); // may not occur
        }
    }

    @Override
    public FutureResponse<Boolean> send(@Nonnull DatastoreRequestProtos.TagRemove request) throws IOException {
        LOG.trace("send: {}", request); //$NON-NLS-1$
        return session.send(
                SERVICE_ID,
                toDelimitedByteArray(DatastoreRequestProtos.Request.newBuilder()
                                     .setTagRemove(request)
                                     .build()),
                new TagRemoveProcessor().asResponseProcessor());
    }

    @Override
    public void close() throws ServerException, IOException, InterruptedException {
        LOG.trace("closing underlying resources"); //$NON-NLS-1$
        resources.close();
    }

// FIXME should process at transport layer
    private byte[] toDelimitedByteArray(DatastoreRequestProtos.Request request) throws IOException {
        try (var buffer = new ByteArrayOutputStream()) {
            request.writeDelimitedTo(buffer);
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new IOException(e);
        }
    }
}
