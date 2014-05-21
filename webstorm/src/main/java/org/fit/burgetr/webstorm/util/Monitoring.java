package org.fit.burgetr.webstorm.util;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import java.lang.ClassNotFoundException;

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
	
	/**
	 * 
	 * 
	 * @param bolt_type
	 * @param tuple_id
	 * @param hostname
	 * @param estimatedNanoS
	 * @throws SQLException
	 */
	public void MonitorTuple(String bolt_type, String tuple_id,String hostname, Long estimatedNanoS) throws SQLException 
	{
		int estimatedMs = 0;
		if(estimatedNanoS!=null){
			estimatedMs = new Long(estimatedNanoS/1000000).intValue();
		}
		
		try{
			Class.forName("org.postgresql.Driver");
		}
		catch(ClassNotFoundException e){
			log.info("Monitoring", "Connection not found... "+e.toString());
		}
		
		String url = "jdbc:postgresql://knot28.fit.vutbr.cz/webstorm?user=webstorm&password=webstormdb88pass";
		Connection conn=null;
		//try {
			conn = DriverManager.getConnection(url);
		//} catch (SQLException e) {
			// TODO Auto-generated catch block
		//	log.info("Monitoring", "Can't get connection");
		//}
		conn.setAutoCommit(false);
		
		
		
		Statement stmt = conn.createStatement();
        String sql = "INSERT INTO profiling (deployment_id,bolt_type,tuple_uuid,hostname, time) "
              + "VALUES ('"+webstormId+"', '"+bolt_type+"', '"+tuple_id+"', '"+hostname+"',"+(estimatedNanoS!=null?estimatedMs:"null")+");";
        stmt.executeUpdate(sql);
        stmt.close();
        conn.commit();
        conn.close();
	}
	
	/**
	 * @param bolt_type
	 * @param tuple_id
	 * @param hostname
	 * @throws SQLException
	 */
	public void MonitorTuple(String bolt_type, String tuple_id,String hostname) throws SQLException {
		MonitorTuple(bolt_type, tuple_id, hostname, null);
	}
}

