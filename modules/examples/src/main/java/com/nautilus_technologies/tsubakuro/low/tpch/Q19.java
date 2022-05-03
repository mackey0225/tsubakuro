package com.nautilus_technologies.tsubakuro.low.tpch;

import java.io.IOException;
import java.util.Objects;

import com.nautilus_technologies.tsubakuro.exception.ServerException;
import com.nautilus_technologies.tsubakuro.low.common.Session;
import com.nautilus_technologies.tsubakuro.low.sql.PreparedStatement;
import com.nautilus_technologies.tsubakuro.protos.CommonProtos;
import com.nautilus_technologies.tsubakuro.protos.RequestProtos;
import com.nautilus_technologies.tsubakuro.protos.ResponseProtos;

public class Q19 {
    Session session;
    PreparedStatement prepared;

    public Q19(Session session) throws IOException, ServerException, InterruptedException {
        this.session = session;
	prepare();
    }

    public void prepare() throws IOException, ServerException, InterruptedException {
	String sql = 
	    "SELECT SUM(L_EXTENDEDPRICE * (100 - L_DISCOUNT)) AS REVENUE "
	    + "FROM LINEITEM, PART "
	    + "WHERE "
	    + "P_PARTKEY = L_PARTKEY "
	    + "AND (( "
	    + "P_BRAND = :brand1 "
	    //                         "AND P_CONTAINER IN ('SM CASE', 'SM BOX', 'SM PACK', 'SM PKG') "
	    + "AND ( P_CONTAINER = 'SM CASE   ' OR  P_CONTAINER = 'SM BOX    ' OR P_CONTAINER = 'SM PACK   ' OR P_CONTAINER = 'SM PKG    ' ) "
	    + "AND L_QUANTITY >= :quantity1 AND L_QUANTITY <= :quantity1 + 10 "
	    //                         "AND P_SIZE BETWEEN 1 AND 5 "
	    + "AND P_SIZE >= 1 AND P_SIZE <= 5 "
	    //                         "AND L_SHIPMODE IN ('AIR', 'AIR REG') "
	    + "AND ( L_SHIPMODE = 'AIR       ' OR  L_SHIPMODE = 'AIR REG   ' ) "
	    + "AND L_SHIPINSTRUCT = 'DELIVER IN PERSON        ' "
	    + ") OR ( "
	    + "P_BRAND = :brand2 "
	    //                         "AND P_CONTAINER IN ('MED BAG', 'MED BOX', 'MED PKG', 'MED PACK') "
	    + "AND ( P_CONTAINER = 'MED BAG   ' OR  P_CONTAINER = 'MED BOX   ' OR P_CONTAINER = 'MED PKG   ' OR P_CONTAINER = 'MED PACK  ' ) "
	    + "AND L_QUANTITY >= :quantity2 AND L_QUANTITY <= :quantity2 + 10 "
	    //                         "AND P_SIZE BETWEEN 1 AND 10 "
	    + "AND P_SIZE >= 1 AND P_SIZE <= 10 "
	    //                         "AND L_SHIPMODE IN ('AIR', 'AIR REG') "
	    + "AND ( L_SHIPMODE = 'AIR       ' OR  L_SHIPMODE = 'AIR REG   ' ) "
	    + "AND L_SHIPINSTRUCT = 'DELIVER IN PERSON        ' "
	    + ") OR ( "
	    + "P_BRAND = :brand3 "
	    //                         "AND P_CONTAINER IN ('LG CASE', 'LG BOX', 'LG PACK', 'LG PKG') "
	    + "AND ( P_CONTAINER = 'LG CASE   ' OR  P_CONTAINER = 'LG BOX    ' OR P_CONTAINER = 'LG PACK   ' OR P_CONTAINER = 'LG PKG    ' ) "
	    + "AND L_QUANTITY >= :quantity3 AND L_QUANTITY <= :quantity3 + 10 "
	    //                         "AND P_SIZE BETWEEN 1 AND 15 "
	    + "AND P_SIZE >= 1 AND P_SIZE <= 15 "
	    //                         "AND L_SHIPMODE IN ('AIR', 'AIR REG') "
	    + "AND ( L_SHIPMODE = 'AIR       ' OR  L_SHIPMODE = 'AIR REG   ' ) "
	    + "AND L_SHIPINSTRUCT = 'DELIVER IN PERSON        ' "
	    + "))";

	var ph = RequestProtos.PlaceHolder.newBuilder()
            .addVariables(RequestProtos.PlaceHolder.Variable.newBuilder().setName("brand1").setType(CommonProtos.DataType.CHARACTER))
	    .addVariables(RequestProtos.PlaceHolder.Variable.newBuilder().setName("quantity1").setType(CommonProtos.DataType.INT8))
	    .addVariables(RequestProtos.PlaceHolder.Variable.newBuilder().setName("brand2").setType(CommonProtos.DataType.CHARACTER))
            .addVariables(RequestProtos.PlaceHolder.Variable.newBuilder().setName("quantity2").setType(CommonProtos.DataType.INT8))
            .addVariables(RequestProtos.PlaceHolder.Variable.newBuilder().setName("brand3").setType(CommonProtos.DataType.CHARACTER))
	    .addVariables(RequestProtos.PlaceHolder.Variable.newBuilder().setName("quantity3").setType(CommonProtos.DataType.INT8))
	    .build();
	prepared = session.prepare(sql, ph).get();
    }

    public void run(Profile profile) throws IOException, ServerException, InterruptedException {
	long start = System.currentTimeMillis();
	var transaction = session.createTransaction(profile.transactionOption.build()).get();

	var ps = RequestProtos.ParameterSet.newBuilder();
	if (profile.queryValidation) {
	    ps.addParameters(RequestProtos.ParameterSet.Parameter.newBuilder().setName("brand1").setCharacterValue("Brand#12  ")).
		addParameters(RequestProtos.ParameterSet.Parameter.newBuilder().setName("brand2").setCharacterValue("Brand#23  ")).
		addParameters(RequestProtos.ParameterSet.Parameter.newBuilder().setName("brand3").setCharacterValue("Brand#34  ")).
		addParameters(RequestProtos.ParameterSet.Parameter.newBuilder().setName("quantity1").setInt8Value(1)).
		addParameters(RequestProtos.ParameterSet.Parameter.newBuilder().setName("quantity2").setInt8Value(10)).
		addParameters(RequestProtos.ParameterSet.Parameter.newBuilder().setName("quantity3").setInt8Value(20));
        } else {
	    ps.addParameters(RequestProtos.ParameterSet.Parameter.newBuilder().setName("brand1").setCharacterValue("Brand#43  ")).
		addParameters(RequestProtos.ParameterSet.Parameter.newBuilder().setName("brand2").setCharacterValue("Brand#41  ")).
		addParameters(RequestProtos.ParameterSet.Parameter.newBuilder().setName("brand3").setCharacterValue("Brand#35  ")).
		addParameters(RequestProtos.ParameterSet.Parameter.newBuilder().setName("quantity1").setInt8Value(5)).
		addParameters(RequestProtos.ParameterSet.Parameter.newBuilder().setName("quantity2").setInt8Value(11)).
		addParameters(RequestProtos.ParameterSet.Parameter.newBuilder().setName("quantity3").setInt8Value(21));
        }

	var future = transaction.executeQuery(prepared, ps.build());
	var resultSet = future.get();

	try {
	    if (Objects.nonNull(resultSet)) {
		if (resultSet.nextRecord()) {
		    resultSet.nextColumn();
		    if (!resultSet.isNull()) {
			System.out.println("REVENUE " + resultSet.getInt8());
		    } else {
			System.out.println("REVENUE is null");
		    }
		} else {
		    throw new IOException("no record");
		}
		if (!ResponseProtos.ResultOnly.ResultCase.SUCCESS.equals(resultSet.getResponse().get().getResultCase())) {
		    throw new IOException("SQL error");
		}
	    } else {
		throw new IOException("no resultSet");
	    }

	    var commitResponse = transaction.commit().get();
	    if (ResponseProtos.ResultOnly.ResultCase.ERROR.equals(commitResponse.getResultCase())) {
		throw new IOException("commit error");
	    }
	} catch (ServerException e) {
	    throw new IOException(e);
	} finally {
	    if (!Objects.isNull(resultSet)) {
		resultSet.close();
	    }
	}
	profile.q19 = System.currentTimeMillis() - start;
    }
}
