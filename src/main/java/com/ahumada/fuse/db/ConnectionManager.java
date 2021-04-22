package com.ahumada.fuse.db;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionManager {

	private javax.sql.DataSource datasource;
	private Connection conn;
	
	public javax.sql.DataSource getDatasource() {
		return datasource;
	}
	public void setDatasource(javax.sql.DataSource datasource) {
		this.datasource = datasource;
	}
	
	public ConnectionManager() {}
	
	public Connection getConnection() throws SQLException {
		if(conn == null || conn.isClosed()) {
			conn = datasource.getConnection();
		}
		return conn;
	}

	public boolean closeConnection() throws SQLException {

		// Close connection if it is opened
		if (conn != null && !conn.isClosed()) {
			conn.close();
		}

		return true;
	}
	
	public boolean isConnValid() {
		return getDatasource() != null;
	}

}
