package com.ahumada.fuse.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import com.ahumada.fuse.db.model.Fv29ClienteDatos;
import com.ahumada.fuse.utils.FunctionUtils;

public class DbHelper {

	private static String EMPTY_STR = "";

	public static boolean insertLogExternalService(
			String requestUrl, 
			String requestBody, 
			Short responseHttpStatus,
			String responseBody,
			String exceptionText,
			Short serviceId,
			Connection conn) throws SQLException {

		boolean insertedOnDatabase = false;

		if(conn != null && requestUrl != null) {
			PreparedStatement ps = null;
			try {
				StringBuilder sql = new StringBuilder();
				// SQL statement com parâmetros obrigatórios
				sql.append(" INSERT INTO VENTADOMICILIO_OWN.LOG_SERVICE_CALLS ")
				.append(" (SERVICE_ID, REQUEST_URL");

				// Nome dos campos não obrigatórios
				if(requestBody != null) sql.append(" , REQUEST_BODY ");
				if(responseBody != null) sql.append(" , RESPONSE_BODY ");
				if(responseHttpStatus != null) sql.append(" , RESPONSE_HTTP_STATUS ");
				if(exceptionText != null) sql.append(" , EXCEPTION_TEXT ");
				sql.append(" ) ");

				// Parâmetros obrigatórios 
				sql.append(" VALUES(?, ? ");

				// Parâmetros não obrigatórios
				if(requestBody != null) sql.append(" , ? ");
				if(responseBody != null) sql.append(" , ? ");
				if(responseHttpStatus != null) sql.append(" , ? ");
				if(exceptionText != null) sql.append(" , ? ");
				sql.append(" ) ");

				// Valores dos parâmetros obrigatórios
				ps = conn.prepareStatement(sql.toString());
				ps.setShort(1, (short)1);
				ps.setString(2, requestUrl);

				// Valores dos parâmetros não obrigatórios, inseridos apenas se necessário
				int paramCounter = 3;
				if(requestBody != null) {
					ps.setString(paramCounter, requestBody);
					paramCounter++;
				}
				if(responseBody != null) {
					ps.setString(paramCounter, responseBody);
					paramCounter++;
				}
				if(responseHttpStatus != null) {
					ps.setShort(paramCounter, responseHttpStatus);
					paramCounter++;
				}
				if(exceptionText != null) {
					ps.setString(paramCounter, exceptionText.substring(0, 200));
					paramCounter++;
				}

				if(ps.executeUpdate() > 0) return true;

			} catch (SQLException e) {
				throw e;
			} finally {
				try {
					if(ps != null) ps.close();
				} catch (SQLException e) {
					// Fail silent 
				}
			}
		}
		return insertedOnDatabase;
	}

	public static boolean deleteFvClienteDatos(Fv29ClienteDatos clienteDatos, Connection conn) throws SQLException {

		boolean insertedOnDatabase = false;

		if(conn != null && clienteDatos != null) {
			PreparedStatement ps = null;

			StringBuilder sql = new StringBuilder()
					.append(" DELETE FROM VENTADOMICILIO_OWN.FV29_CLIENTE_DATOS ")
					.append(" WHERE DOC_NUMERO = ? , DOC_ESTADO = ? ");

			try {
				// Set values of mandatory params
				ps = conn.prepareStatement(sql.toString());
				ps.setString(1, clienteDatos.getDocNumero());
				// Used to delete RUTs with same Estado, to make sure we have only one RUT validy in the database
				ps.setString(2, clienteDatos.getDocEstado()); 
				if(ps.executeUpdate() > 0) return true;
				
			} catch (SQLException e) {
				throw e;
			} finally {
				try {
					if(ps != null) ps.close();
				} catch (SQLException e) {
					// Fail silent 
				}
			}

		}

		return insertedOnDatabase;
	}

	public static boolean upsertFv29ClienteDatos(
			Fv29ClienteDatos clienteDatos, 
			boolean isDataUpdate,
			Connection conn) throws SQLException {

		boolean insertedOnDatabase = false;

		if(conn != null && clienteDatos != null) {
			PreparedStatement ps = null;
			try {
				// Columns
				StringBuilder sql = new StringBuilder();
				// Values
				StringBuilder sqlInsertValues = new StringBuilder();

				// Beginning of SQL statement with mandatory parameters
				if(isDataUpdate) {
					sql.append(" UPDATE VENTADOMICILIO_OWN.FV29_CLIENTE_DATOS ")
					.append(" SET FECHA_CONSULTA_DATOS = SYSTIMESTAMP ");
				} else {
					sql.append(" INSERT INTO VENTADOMICILIO_OWN.FV29_CLIENTE_DATOS ")
					.append(" (DOC_NUMERO , DOC_SERIE ");

					sqlInsertValues.append(" VALUES(? , ? ");
				}

				// Non Mandatory columns
				strNullUpsertStatement(sql, sqlInsertValues, "EXISTE", clienteDatos.getIdTransaction(), isDataUpdate);
				strNullUpsertStatement(sql, sqlInsertValues, "ID_TRANSACTION", clienteDatos.getIdTransaction(), isDataUpdate);
				strNullUpsertStatement(sql, sqlInsertValues, "DOC_TIPO", clienteDatos.getDocTipo(), isDataUpdate);
				strNullUpsertStatement(sql, sqlInsertValues, "DOC_ESTADO", clienteDatos.getDocEstado(), isDataUpdate);
				strNullUpsertStatement(sql, sqlInsertValues, "DOC_MOTIVO", clienteDatos.getDocMotivo(), isDataUpdate);
				strNullUpsertStatement(sql, sqlInsertValues, "NOMBRES", clienteDatos.getNombres(), isDataUpdate);
				strNullUpsertStatement(sql, sqlInsertValues, "APATERNO", clienteDatos.getApaterno(), isDataUpdate);
				strNullUpsertStatement(sql, sqlInsertValues, "AMATERNO", clienteDatos.getAmaterno(), isDataUpdate);
				strNullUpsertStatement(sql, sqlInsertValues, "NACIONALIDAD", clienteDatos.getNacionalidad(), isDataUpdate);
				strNullUpsertStatement(sql, sqlInsertValues, "SEXO", clienteDatos.getSexo(), isDataUpdate);
				strNullUpsertStatement(sql, sqlInsertValues, "ECIVIL", clienteDatos.getEcivil(), isDataUpdate);
				strNullUpsertStatement(sql, sqlInsertValues, "FNACIMIENTO", clienteDatos.getFnacimiento(), isDataUpdate);
				strNullUpsertStatement(sql, sqlInsertValues, "FDEFUNCION", clienteDatos.getFdefuncion(), isDataUpdate);
				strNullUpsertStatement(sql, sqlInsertValues, "FMATRIMONIO", clienteDatos.getFmatrimonio(), isDataUpdate);

				// End of SQL statement 
				if(isDataUpdate) {
					sql.append(" WHERE DOC_NUMERO = ? , DOC_SERIE = ?");
				} else {
					// Close sql parts
					sql.append(" ) ");
					sqlInsertValues.append(" ) ");
					// Attach the second piece of SQL to the first one
					sql.append(sqlInsertValues.toString());
				}

				// Set values of mandatory params
				ps = conn.prepareStatement(sql.toString());
				if(!isDataUpdate) {
					ps.setString(1, clienteDatos.getDocNumero());
					ps.setString(2, clienteDatos.getDocSerie());
				}

				// Set values of non mandatory params
				Integer paramCounter = isDataUpdate ? 1 : 3;

				paramCounter = setStrUpsertValues(ps, clienteDatos.getExiste(), paramCounter, isDataUpdate) ? paramCounter + 1 : paramCounter;
				paramCounter = setStrUpsertValues(ps, clienteDatos.getIdTransaction(), paramCounter, isDataUpdate) ? paramCounter + 1 : paramCounter;
				paramCounter = setStrUpsertValues(ps, clienteDatos.getDocTipo(), paramCounter, isDataUpdate) ? paramCounter + 1 : paramCounter;
				paramCounter = setStrUpsertValues(ps, clienteDatos.getDocEstado(), paramCounter, isDataUpdate) ? paramCounter + 1 : paramCounter;
				paramCounter = setStrUpsertValues(ps, clienteDatos.getDocMotivo(),paramCounter,  isDataUpdate) ? paramCounter + 1 : paramCounter;
				paramCounter = setStrUpsertValues(ps, clienteDatos.getNombres(), paramCounter, isDataUpdate) ? paramCounter + 1 : paramCounter;
				paramCounter = setStrUpsertValues(ps, clienteDatos.getApaterno(), paramCounter,  isDataUpdate) ? paramCounter + 1 : paramCounter;
				paramCounter = setStrUpsertValues(ps, clienteDatos.getAmaterno(), paramCounter, isDataUpdate) ? paramCounter + 1 : paramCounter;
				paramCounter = setStrUpsertValues(ps, clienteDatos.getNacionalidad(), paramCounter, isDataUpdate) ? paramCounter + 1 : paramCounter;
				paramCounter = setStrUpsertValues(ps, clienteDatos.getSexo(), paramCounter, isDataUpdate) ? paramCounter + 1 : paramCounter;
				paramCounter = setStrUpsertValues(ps, clienteDatos.getEcivil(), paramCounter, isDataUpdate) ? paramCounter + 1 : paramCounter;
				paramCounter = setStrUpsertValues(ps, clienteDatos.getFnacimiento(), paramCounter, isDataUpdate) ? paramCounter + 1 : paramCounter;
				paramCounter = setStrUpsertValues(ps, clienteDatos.getFdefuncion(), paramCounter, isDataUpdate) ? paramCounter + 1 : paramCounter;
				paramCounter = setStrUpsertValues(ps, clienteDatos.getFmatrimonio(), paramCounter, isDataUpdate) ? paramCounter + 1 : paramCounter;

				if(isDataUpdate) {
					ps.setString(paramCounter, clienteDatos.getDocNumero());
					paramCounter++;
					ps.setString(paramCounter, clienteDatos.getDocSerie());
				}

				// Execute Insert
				if(ps.executeUpdate() > 0) return true;

			} catch (SQLException e) {
				throw e;
			} finally {
				try {
					if(ps != null) ps.close();
				} catch (SQLException e) {
					// Fail silent 
				}
			}
		}
		return insertedOnDatabase;
	}

	public static boolean upsertFaultFv29ClienteDatos(
			Fv29ClienteDatos clienteDatos, 
			boolean isDataUpdate,
			Connection conn) throws SQLException {

		boolean insertedOnDatabase = false;

		if(conn != null && clienteDatos != null) {
			PreparedStatement ps = null;
			try {
				StringBuilder sql = new StringBuilder();

				if(isDataUpdate) {
					sql.append(" UPDATE VENTADOMICILIO_OWN.FV29_CLIENTE_DATOS ")
					.append(" SET FAULT_STRING = ? , FAULT_DETAIL = ? ")
					.append(" , FECHA_CONSULTA_DATOS = SYSTIMESTAMP ")
					.append(" WHERE DOC_NUMERO = ? , DOC_SERIE = ? ");
				} else {
					sql.append(" INSERT INTO VENTADOMICILIO_OWN.FV29_CLIENTE_DATOS ")
					.append(" (DOC_NUMERO , DOC_SERIE , FAULT_STRING , FAULT_DETAIL )")
					.append(" VALUES(? , ? , ? , ? ) ");
				}

				// Set values of mandatory params
				ps = conn.prepareStatement(sql.toString());
				if(isDataUpdate) {
					setStrUpsertValues(ps, clienteDatos.getFaultString(), 1, isDataUpdate);
					setStrUpsertValues(ps, clienteDatos.getFaultDetail(), 2, isDataUpdate);
					ps.setString(3, clienteDatos.getDocNumero());
					ps.setString(4, clienteDatos.getDocSerie());
				} else {
					ps.setString(1, clienteDatos.getDocNumero());
					setStrUpsertValues(ps, clienteDatos.getDocSerie(), 2, isDataUpdate);
					setStrUpsertValues(ps, clienteDatos.getFaultString(), 3, isDataUpdate);
					setStrUpsertValues(ps, clienteDatos.getFaultString(), 4, isDataUpdate);
				}

				// Execute Insert
				if(ps.executeUpdate() > 0) return true;

			} catch (SQLException e) {
				throw e;
			} finally {
				try {
					if(ps != null) ps.close();
				} catch (SQLException e) {
					// Fail silent 
				}
			}
		}
		return insertedOnDatabase;
	}

	/**
	 * Help to build sql statement for Update OR Insert methods (upsert) for String Params
	 * 
	 * @param sql
	 * @param sqlInsertValues
	 * @param columnName
	 * @param columnValue
	 * @param isDataUpdate
	 * @throws SQLException
	 */
	private static void strNullUpsertStatement(
			StringBuilder sql , 
			StringBuilder sqlInsertValues ,
			String columnName , 
			String columnValue ,
			boolean isDataUpdate ) throws SQLException {

		if(isDataUpdate) {
			sql.append(" , ");
			sql.append(columnName);
			sql.append(" = ? ");
		} else if(!FunctionUtils.stringIsNullOrEmpty(columnValue)){ 
			sql.append(" , ").append(columnName);
			sqlInsertValues.append(" , ? ");
		}
	}

	/**
	 * Help set String values on upsert methods
	 * 
	 * @param ps
	 * @param columnValue
	 * @param paramCounter
	 * @param isDataUpdate
	 * @throws SQLException
	 */
	private static boolean setStrUpsertValues(
			PreparedStatement ps ,
			String columnValue ,
			int paramCounter ,
			boolean isDataUpdate) throws SQLException {

		if(!FunctionUtils.stringIsNullOrEmpty(columnValue)) {
			ps.setString(paramCounter, columnValue);
			return true;
		} else if(isDataUpdate) {
			ps.setString(paramCounter, EMPTY_STR);
			return true;
		}
		return false;
	}

	public static Fv29ClienteDatos getBasicFv29ClienteDatos(
			String rut, 
			String serie,
			Integer daysOfRecordValidity,
			Connection conn) throws SQLException {

		Fv29ClienteDatos clienteDatos = null;

		if(conn != null && !FunctionUtils.stringIsNullOrEmpty(rut)) {

			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				StringBuilder sql = new StringBuilder();

				// SQL statement with mandatory parameters
				sql.append(" SELECT ")
				.append(" EXISTE, FECHA_CONSULTA_DATOS ")
				.append(", (FECHA_CONSULTA_DATOS + ").append(daysOfRecordValidity).append(" )")
				.append(" , CURRENT_TIMESTAMP , DOC_ESTADO")
				.append(" FROM VENTADOMICILIO_OWN.FV29_CLIENTE_DATOS")
				.append(" WHERE DOC_NUMERO = ? , DOC_SERIE = ? ");

				// Set values of mandatory params
				ps = conn.prepareStatement(sql.toString());
				ps.setString(1, rut);

				rs = ps.executeQuery();
				if(rs != null && rs.next()){
					clienteDatos = new Fv29ClienteDatos(rut, rs.getString(1));
					clienteDatos.setDocSerie(serie);
					clienteDatos.setFechaConsultaDatos(new Date(rs.getTimestamp(2).getTime()));
					clienteDatos.setFechaConsultaDatosValidez(new Date(rs.getTimestamp(3).getTime()));
					clienteDatos.setCurrentTimestampDatabase(new Date(rs.getTimestamp(4).getTime()));
					clienteDatos.setDocEstado(rs.getString(5));
				}

			} catch (SQLException e) {
				throw e;
			} finally {
				try {
					if(ps != null) ps.close();
				} catch (SQLException e) {
					// Fail silent 
				}
			}
		}
		return clienteDatos;
	}


	public static Date getValidityFv29ClienteDatos(
			String rut, 
			String serie,
			Integer daysOfRecordValidity,
			Connection conn) throws SQLException {


		Date validity = null;

		if(conn != null && !FunctionUtils.stringIsNullOrEmpty(rut)) {

			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				StringBuilder sql = new StringBuilder();

				// SQL statement with mandatory parameters
				sql.append(" SELECT ")
				.append(" (FECHA_CONSULTA_DATOS + ").append(daysOfRecordValidity).append(" )")
				.append(" FROM VENTADOMICILIO_OWN.FV29_CLIENTE_DATOS")
				.append(" WHERE DOC_NUMERO = ? , DOC_SERIE = ? ");

				// Set values of mandatory params
				ps = conn.prepareStatement(sql.toString());
				ps.setString(1, rut);
				ps.setString(2, serie);

				rs = ps.executeQuery();
				if(rs != null && rs.next()){
					validity = new Date(rs.getTimestamp(1).getTime());
				}

			} catch (SQLException e) {
				throw e;
			} finally {
				try {
					if(ps != null) ps.close();
				} catch (SQLException e) {
					// Fail silent 
				}
			}
		}
		return validity;
	}

	public static boolean existsFv29ClienteDatos(
			String rut, 
			Connection conn) throws SQLException {

		boolean existsClienteDatos = false;

		if(conn != null && !FunctionUtils.stringIsNullOrEmpty(rut)) {

			PreparedStatement ps = null;
			ResultSet rs = null;

			try {
				StringBuilder sql = new StringBuilder();

				// SQL statement with mandatory parameters
				sql.append(" SELECT COUNT(1) FROM VENTADOMICILIO_OWN.FV29_CLIENTE_DATOS  ")
				.append(" WHERE DOC_NUMERO = ? ");

				// Set values of mandatory params
				ps = conn.prepareStatement(sql.toString());
				ps.setString(1, rut);

				rs = ps.executeQuery();
				if(rs != null && rs.next()){
					if(rs.getInt(1) > 0) existsClienteDatos = true;
				}

			} catch (SQLException e) {
				throw e;
			} finally {
				try {
					if(ps != null) ps.close();
				} catch (SQLException e) {
					// Fail silent 
				}
			}
		}
		return existsClienteDatos;
	}
}
