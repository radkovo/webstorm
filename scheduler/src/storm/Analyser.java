package storm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import backtype.storm.scheduler.TopologyDetails;

/**
 * Supports Scheduling advisor's scheduler with analytic data taken from
 * monitoring database.
 * 
 * Works with monitoring data starting with advisor.analysis.startTime in topology config
 * or from NOW.
 * 
 * @author Petr Škoda
 */
public class Analyser {
	
	// Topology being scheduled
	private TopologyDetails topology = null;
	// Monitoring start time in ISO 8601 string
	private String startTime = null;
	// DB connection
	Connection conn = null;
	
	/**
	 * Constructor with topology.
	 * Creates DB connection and prepares the time to monitor from
	 * 
	 * @param TopologyDetails topology	Analysed topology
	 */
	public Analyser(TopologyDetails topology)
	{
		this.topology = topology;
		
		// Configuration of topology
        Map conf = topology.getConf();
		
		// Monitoring start time - if no time given, time is set to now
        if(conf.containsKey("advisor.analysis.startTime")){
        	startTime = (String) conf.get("advisor.analysis.startTime");
        }
        else{
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
			startTime = df.format(new Date());
        }
        
        // Monitoring DB connection
        String url = "jdbc:postgresql://knot28.fit.vutbr.cz/webstorm?user=webstorm&password=webstormdb88pass";
		try {
			try{
				Class.forName("org.postgresql.Driver");
			}
			catch(ClassNotFoundException e){
				System.out.println("Connector not found... "+e.toString());
			}
			
			conn = DriverManager.getConnection(url);
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			System.out.println("Error connecting to DB, exception: " + e.toString());
		}
	}
	
	/**
	 * Get all hosts/executors combinations that have been measured.
	 *  
	 *  @return Map<String, List<String>>	List of measured executors for each host name.
	 */
	public Map<String, List<String>> getMeasuredHosts()
	{
		Map<String, List<String>> measured = new HashMap<String, List<String>>();
		
		try {
			Statement stmt = conn.createStatement();
			// Fond measured hosts and executors
			String sql = "SELECT distinct hostname, bolt_type FROM profiling WHERE timestamp > '" + startTime + "' ORDER BY hostname;";
			System.out.println(sql);
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()){
		         // Feed hosts and executors to Map
		         String host = rs.getString("hostname");
		         String executorType = rs.getString("bolt_type");
		         System.out.println(host + " xxx " + executorType);
		         if(measured.containsKey(host)){
		        	 measured.get(host).add(executorType);
		         }
		         else{
		        	 List<String> executorTypes = new LinkedList<String>();
		        	 executorTypes.add(executorType);
		        	 measured.put(host, executorTypes);
		         }
			}
			rs.close();

		} catch (SQLException e) {
			System.out.println("Error creating statement, exception: " + e.toString());
		}
		
		return measured;
	}
}
