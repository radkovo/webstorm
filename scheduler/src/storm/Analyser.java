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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

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
	private Connection conn = null;
	// ISO date formatter
	private DateFormat isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	
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
			startTime = isoFormatter.format(new Date()); // Not work! - seems that the new Analyser object is crated after each benchmark reschedule
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
	 * Uses the start time preconfigured for this topology...
	 *  
	 *  @return Map<String, List<String>>	List of measured executors for each host name.
	 */
	public Map<String, List<String>> getMeasuredHosts()
	{
		return getMeasuredHosts(startTime);
	}
	
	/**
	 * Get all hosts/executors combinations that have been measured.
	 *  
	 *  @param	String						ISO timestamp - Start time to check the data
	 *  @return Map<String, List<String>>	List of measured executors for each host name.
	 */
	public Map<String, List<String>> getMeasuredHosts(String startTime)
	{
		Map<String, List<String>> measured = new HashMap<String, List<String>>();
		
		try {
			Statement stmt = conn.createStatement();
			// Find measured hosts and executors
			String sql = "SELECT distinct hostname, bolt_type FROM profiling WHERE timestamp > '" + startTime + "' ORDER BY hostname;";
			System.out.println(sql);
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()){
		         // Feed hosts and executors to Map
		         String host = rs.getString("hostname");
		         String executorType = rs.getString("bolt_type");
		         //System.out.println(host + " xxx " + executorType);
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
	
	
	/**
	 * Gets the ordered hosts for each executor type where first executor types are 
	 * most important to be scheduled to most suitable hosts. The order of hosts
	 * means its suitability (from best suitable to worst)
	 * 
	 * @param endtime End time for monitoring data analysis
	 * @param worstPlacement Get the worst placement instead? (reverse ordered)
	 * @return Executor type to list of hosts all sorted from most suitable to least suitable.
	 */
	public LinkedHashMap<String, LinkedList<String>> getBestPlacementsForExecutorTypes(Date endtime, boolean worstPlacement)
	{
		String endTimeStr = isoFormatter.format(endtime);
		
		// Order clauses for best and worst placements
		String bestOrder = "ORDER BY scheduling_order DESC NULLS LAST, max(h1loss_norm) DESC NULLS LAST\r\n";
		String worstOrder = "ORDER BY scheduling_order ASC NULLS LAST, max(h1loss_norm) ASC NULLS LAST\r\n";
		
		// SQL query finding the right order of scheduling
		String sql = "SELECT\r\n" + 
				"	bolt_type, h2, max(h1loss_norm) AS h1loss_norm, max(max(h1loss_norm)) OVER (PARTITION BY bolt_type) AS scheduling_order\r\n" + 
				"FROM (\r\n" + 
				"	SELECT\r\n" + 
				"		h1.bolt_type, h1.hostname h1, h2.hostname h2, h1.count h1count, h2.count h2count, \r\n" + 
				"		h1.avgtime - h2.avgtime AS h1loss,\r\n" + 
				"		round(((sqrt(h1.count/(max(h1.count) OVER ())::real)+1) * (h1.avgtime - h2.avgtime))::numeric, 3) AS h1loss_norm  -- normalized loss using number of tuples\r\n" + 
				"	FROM (\r\n" + 
				"			SELECT bolt_type, hostname, round(avg(time), 3) as avgtime, count(id) \r\n" + 
				"			FROM profiling \r\n" + 
				"			WHERE timestamp > '"+ startTime +"' AND timestamp < '"+ endTimeStr +"' \r\n" + 
				"			GROUP BY bolt_type, hostname \r\n" + 
				"			ORDER BY bolt_type, avgtime\r\n" + 
				"		) h1\r\n" + 
				"		-- Join once more the same data to get all combinations\r\n" + 
				"		JOIN (\r\n" + 
				"			SELECT bolt_type, hostname, round(avg(time), 3) as avgtime, count(id) \r\n" + 
				"			FROM profiling \r\n" + 
				"			WHERE timestamp > '"+ startTime +"' AND timestamp < '"+endTimeStr+"' \r\n" + 
				"			GROUP BY bolt_type, hostname \r\n" + 
				"			ORDER BY bolt_type, avgtime\r\n" + 
				"		) h2 ON h1.bolt_type = h2.bolt_type AND h1.hostname != h2.hostname\r\n" + 
				"	\r\n" + 
				"	--WHERE h1.avgtime - h2.avgtime > 0\r\n" +
				"	WHERE h1.avgtime > 0\r\n" +
				"	--GROUP BY h1.bolt_type, h1.hostname, h2.hostname, h1.count, h2.count, h1.avgtime, h2.avgtime\r\n" + 
				"	ORDER BY bolt_type, h1loss DESC\r\n" + 
				"	) AS x\r\n" + 
				"GROUP BY bolt_type, h2\r\n" + 
				// Worst or best placement order...
				(worstPlacement ? worstOrder : bestOrder)+ 
				";";
		
		LinkedHashMap<String, LinkedList<String>> schedule = new LinkedHashMap<String, LinkedList<String>>();
		
		try {
			Statement stmt = conn.createStatement();
			System.out.println("Scheduling order:" + sql);
			ResultSet rs = stmt.executeQuery(sql);
			while(rs.next()){
		         // Executors to hosts
		         String host = rs.getString("h2");
		         String executorType = rs.getString("bolt_type");
		         if(schedule.containsKey(executorType)){
		        	 schedule.get(executorType).add(host);
		         }
		         else{
		        	 LinkedList<String> hosts = new LinkedList<String>();
		        	 hosts.add(host);
		        	 schedule.put(executorType, hosts);
		         }
			}
			rs.close();

		} catch (SQLException e) {
			System.out.println("Error creating statement, exception: " + e.toString());
		}
		
		return schedule;
	}
	
	/**
	 * Set start time
	 *  
	 *  @return Map<String, List<String>>	List of measured executors for each host name.
	 */
	public void setStartTime(Date st)
	{
		startTime = isoFormatter.format(st);
	}
}
