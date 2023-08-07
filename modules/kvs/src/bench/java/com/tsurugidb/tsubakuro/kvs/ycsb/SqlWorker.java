package com.tsurugidb.tsubakuro.kvs.ycsb;

import java.net.URI;

import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.SqlClient;

/**
 * benchmark worker using SqlClient
 */
public class SqlWorker extends Worker {

    SqlWorker(RunManager mgr, URI endpoint, boolean createDB, int numClient, int clientId, int rratio, long runMsec)
            throws Exception {
        super(mgr, endpoint, createDB, numClient, clientId, rratio, runMsec);
    }

    @Override
    protected Long benchmark() throws Exception {
        int optId = 0;
        long numTx = 0;
        try (var session = SessionBuilder.connect(endpoint).withCredential(NullCredential.INSTANCE).create();
            var client = SqlClient.attach(session)) {
            mgr.addReadyWorker();
            while (!mgr.isAllWorkersReady()) {
                // finite loop
            }
            while (true) {
                optId = 0;
                while (optId < operations.size()) {
                    if (mgr.isQuit()) {
                        return Long.valueOf(numTx);
                    }
                    try (var tx = client.createTransaction().await()) {
                        for (int i = 0; i < Constants.OPS_PER_TX; i++, optId++) {
                            var op = operations.get(optId % operations.size());
                            if (op.isGet()) {
                                String sql = String.format("SELECT * FROM %s WHERE %s=%d", tableName,
                                        Constants.KEY_NAME, op.key());
                                try (var rs = tx.executeQuery(sql).await()) {
                                    while (rs.nextRow()) {
                                        while (rs.nextColumn()) {
                                            rs.fetchInt8Value();
                                        }
                                    }
                                }
                            } else {
                                String sql = String.format("UPDATE %s SET %s=%d WHERE %s=%d", tableName,
                                        Constants.VALUE_NAME, Long.valueOf(100L * i), Constants.KEY_NAME, op.key());
                                tx.executeStatement(sql).await();
                            }
                        }
                        tx.commit().await();
                    }
                    numTx++;
                }
            }
        }
    }

}
