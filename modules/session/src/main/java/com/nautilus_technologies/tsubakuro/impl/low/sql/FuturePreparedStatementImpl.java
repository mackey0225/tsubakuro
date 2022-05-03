package com.nautilus_technologies.tsubakuro.impl.low.sql;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.nautilus_technologies.tsubakuro.exception.ServerException;
import com.nautilus_technologies.tsubakuro.impl.low.common.SessionLinkImpl;
import com.nautilus_technologies.tsubakuro.low.sql.PreparedStatement;
import com.nautilus_technologies.tsubakuro.protos.ResponseProtos;
import com.nautilus_technologies.tsubakuro.util.FutureResponse;

/**
 * FuturePreparedStatementImpl type.
 */
public class FuturePreparedStatementImpl extends AbstractFutureResponse<PreparedStatement> {

    private final FutureResponse<ResponseProtos.Prepare> delegate;
    private final SessionLinkImpl sessionLinkImpl;

    /**
     * Class constructor, called from SessionLinkImpl that is connected to the SQL server.
     * @param future the Future of ResponseProtos.Prepare
     * @param sessionLinkImpl the caller of this constructor
     */
    public FuturePreparedStatementImpl(FutureResponse<ResponseProtos.Prepare> future, SessionLinkImpl sessionLinkImpl) {
        this.delegate = future;
        this.sessionLinkImpl = sessionLinkImpl;
    }

    @Override
    protected PreparedStatement getInternal() throws IOException, ServerException, InterruptedException {
        ResponseProtos.Prepare response = delegate.get();
        return resolve(response);
    }

    @Override
    protected PreparedStatement getInternal(long timeout, TimeUnit unit)
            throws IOException, ServerException, InterruptedException, TimeoutException {
        ResponseProtos.Prepare response = delegate.get(timeout, unit);
        return resolve(response);
    }

    private PreparedStatement resolve(ResponseProtos.Prepare response) throws IOException {
        if (ResponseProtos.Prepare.ResultCase.ERROR.equals(response.getResultCase())) {
            // FIXME: throw structured exception
            throw new IOException("prepare error");
        }
        return new PreparedStatementImpl(response.getPreparedStatementHandle(), sessionLinkImpl);
    }

    @Override
    public void close() throws IOException, ServerException, InterruptedException {
        delegate.close();
    }
}
