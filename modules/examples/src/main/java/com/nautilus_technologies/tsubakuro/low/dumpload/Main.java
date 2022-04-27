package com.nautilus_technologies.tsubakuro.low.dumpload;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Path;
import com.nautilus_technologies.tsubakuro.low.common.Session;
import com.nautilus_technologies.tsubakuro.low.common.SessionBuilder;
import com.nautilus_technologies.tsubakuro.low.sql.SqlClient;
import com.nautilus_technologies.tsubakuro.low.sql.Transaction;
import com.nautilus_technologies.tsubakuro.low.sql.ResultSet;
import com.nautilus_technologies.tsubakuro.low.sql.PreparedStatement;
import com.nautilus_technologies.tsubakuro.protos.RequestProtos;
import com.nautilus_technologies.tsubakuro.protos.ResponseProtos;
import com.nautilus_technologies.tsubakuro.protos.CommonProtos;
import com.nautilus_technologies.tsubakuro.channel.common.connection.Connector;
import com.nautilus_technologies.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.nautilus_technologies.tsubakuro.exception.ServerException;

public final class Main {
    private Main() {
    }
    
    //    private static String url = "ipc:tateyama";
    private static String url = "tcp://localhost:12345/";
    
    public static void main(String[] args) {

        try (Session session = SessionBuilder.connect(url)
             .withCredential(new UsernamePasswordCredential("user", "pass"))
             .create(10, TimeUnit.SECONDS)) {
            
            try (SqlClient client = SqlClient.attach(session)) {
                Future<Transaction> fTransactionCT = client.createTransaction();
                try (Transaction transaction = fTransactionCT.get()) {
                    // create table
                    var responseCreateTable = transaction.executeStatement("CREATE TABLE dump_load_test(pk INT PRIMARY KEY, c1 INT)").get();
                    if (ResponseProtos.ResultOnly.ResultCase.ERROR.equals(responseCreateTable.getResultCase())) {
                        throw new IOException("error in create table");
                    }
                    if (ResponseProtos.ResultOnly.ResultCase.ERROR.equals(transaction.commit().get().getResultCase())) {
                        throw new IOException("error in commit");
                    }
                } catch (IOException | InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                
                // load
                {
                    var prep = client.prepare("INSERT INTO dump_load_test(pk, c1) VALUES(:pk, :c1)",
                                              RequestProtos.PlaceHolder.newBuilder()
                                              .addVariables(RequestProtos.PlaceHolder.Variable.newBuilder().setName("pk").setType(CommonProtos.DataType.INT4))
                                              .addVariables(RequestProtos.PlaceHolder.Variable.newBuilder().setName("c1").setType(CommonProtos.DataType.INT4))
                                              .build()).get();
                    
                    ArrayList<Path> params = new ArrayList<>();
                    params.add(Path.of("/path/to/load-parameter"));
                    Future<Transaction> fTx = client.createTransaction();
                    try (Transaction tx = fTx.get()) {
                        var fResult = tx.executeLoad(prep, RequestProtos.ParameterSet.newBuilder().build(), params);
                        if (ResponseProtos.ResultOnly.ResultCase.ERROR.equals(fResult.get().getResultCase())) {
                            throw new IOException("error executeLoad");
                        }
                        var fStatus = tx.commit();
                        if (ResponseProtos.ResultOnly.ResultCase.ERROR.equals(fStatus.get().getResultCase())) {
                            throw new IOException("error in commit");
                        }
                    } catch (IOException | InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                
                {
                    // dump
                    var prep = client.prepare("SELECT * FROM dump_load_test", RequestProtos.PlaceHolder.newBuilder().build()).get();
                    var target = Path.of("/path/to/dump-target");
                    
                    Future<Transaction> fTx = client.createTransaction();
                    try (Transaction tx = fTx.get()) {
                        Future<ResultSet> fResults = tx.executeDump(prep, RequestProtos.ParameterSet.newBuilder().build(), target);
                        
                        var results = fResults.get();
                        while (results.nextRecord()) {
                            while (results.nextColumn()) {
                                var s = results.getCharacter();
                                System.out.println(s);
                            }
                        }
                        var fStatus = tx.commit();
                        if (ResponseProtos.ResultOnly.ResultCase.ERROR.equals(fStatus.get().getResultCase())) {
                            throw new IOException("error in commit");
                        }
                    } catch (IOException | InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException | InterruptedException | ExecutionException | ServerException e) {
                e.printStackTrace();
            }
        } catch (IOException | InterruptedException | TimeoutException | ServerException e) {
            e.printStackTrace();
        }
    }
}
