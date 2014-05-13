package org.fit.burgetr.webstorm.util;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Monitoring implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(Monitoring.class);
	//Connection conn;
	String webstormId;
	public Monitoring(String uuid) throws SQLException{
		
		webstormId=uuid;
	}
	
	public void MonitorTuple(String bolt_type,String tuple_id,String hostname) throws SQLException{
		
		String url = "jdbc:postgresql://knot28.fit.vutbr.cz/webstorm?user=webstorm&password=webstormdb88pass";
		Connection conn=null;
		try {
			conn = DriverManager.getConnection(url);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			log.info("Monitoring", "Can't get connection");
		}
		conn.setAutoCommit(false);
		
		
		
		Statement stmt = conn.createStatement();
        String sql = "INSERT INTO profiling (deployment_id,bolt_type,tuple_uuid,hostname) "
              + "VALUES ('"+webstormId+"', '"+bolt_type+"', '"+tuple_id+"', '"+hostname+"');";
        stmt.executeUpdate(sql);
        stmt.close();
        conn.commit();
        conn.close();
	}
}
