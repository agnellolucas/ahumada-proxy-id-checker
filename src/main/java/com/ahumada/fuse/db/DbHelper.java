package com.ahumada.fuse.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DbHelper {

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
	
}
