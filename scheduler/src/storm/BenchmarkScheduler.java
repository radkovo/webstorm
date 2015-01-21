package storm;

import java.io.UnsupportedEncodingException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.collections4.CollectionUtils;

import backtype.storm.scheduler.Cluster;
import backtype.storm.scheduler.EvenScheduler;
import backtype.storm.scheduler.ExecutorDetails;
import backtype.storm.scheduler.IScheduler;
import backtype.storm.scheduler.SchedulerAssignment;
import backtype.storm.scheduler.SupervisorDetails;
import backtype.storm.scheduler.Topologies;
import backtype.storm.scheduler.TopologyDetails;
import backtype.storm.scheduler.WorkerSlot;

public class BenchmarkScheduler implements IScheduler {
	// Next rescheduling times by topologies (topology ID)
	private Map<String, Date> reschedulingForTopology = new HashMap<String, Date>();
	// Last rescheduling times by topologies (topology ID)
	private Map<String, Date> lastReschedulingForTopology = new HashMap<String, Date>();
	// End of profiling of topology
	private Map<String, Date> endOfProfilingForTopology = new HashMap<String, Date>();
	// For experiments the worst known case scheduling was done
	private Set<String> worstCaseDone = new HashSet<String>();
	// For experiments the standard scheduling was done
	private Set<String> standardCaseDone = new HashSet<String>();
	// Analysers for topologies (topology ID)
	private Map<String, Analyser> analysers = new HashMap<String, Analyser>();
	// Number of performance schedules done (for cycling benchmarking phase again)
	private Map<String, Integer> performanceRescheduledTimes = new HashMap<String, Integer>();
	
	// Last applicated schedule
	private Map<String, String> lastSchedule = new HashMap<String, String>(); 
	
	// List of JSON placement files
    private Map<String, ArrayList<String>> jsonPlacementFiles = new HashMap<String, ArrayList<String>>(); 
	
	// SQL connection
	private Connection conn = null;
	
	// ISO date formatter
	private DateFormat isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	
	public void prepare(Map config) {
		// -----------------------------------
        // For Modifying records in time range with adding the schedule type to each record for easier stats generating
        // WORKS only with one topology scheduled
        // THIS IS NOT suitable for normal usage
        String url = "jdbc:postgresql://knot28.fit.vutbr.cz/webstorm?user=webstorm&password=webstormdb88pass";
		try {
			conn = DriverManager.getConnection(url);
			//conn.setAutoCommit(false); // We do only few inserts, autocommits are welcomed
		} catch (SQLException e) {
			System.out.println("Can't get connection to monitoring DB \n" + e.toString());
		}
		// ----------------------------------
		
		/* END OF CONNECTION - will exit when program is terminated
		// FOR EXPERIMENTS with writting schedule type to monitoring DB
        try {
            conn.commit();
            conn.close();
		} catch (SQLException e) {
			System.out.println(e.toString());
		}
		*/
	} 

	
	/**
	 * Schedules and reschedules first to gather profiling data about hosts/executors
	 * and then, after getting sufficient monitoring data, prepares the "best" possible schedule
	 * based on data gathered during first phase.
	 * 
	 * For unsolved scheduling fires default scheduler.
	 * 
	 * @param topologies
	 * @param cluster
	 */
    public void schedule(Topologies topologies, Cluster cluster) {
    	System.out.println("SchedulingAdvisor: begin scheduling");
        // Gets the topology which we want to schedule
        TopologyDetails topology = topologies.getByName("Webstorm");
        
        // Make sure our topology is submitted
        if (topology != null) {
        	
        	// Check or prepare Analyser
        	if(!analysers.containsKey(topology.getId())){
        		analysers.put(topology.getId(), new Analyser(topology));
        	}
        	
        	System.out.println("Assigned workers:" + cluster.getAssignedNumWorkers(topology));
            
            // Configuration of topology
            Map conf = topology.getConf(); 
            
            System.out.println("Reschedules: " + reschedulingForTopology);
            // Check if the rescheduling is needed, if so, unschedule the topology
            if(reschedulingForTopology.containsKey(topology.getId()) && new Date().after(reschedulingForTopology.get(topology.getId()))){
            	// Unschedule
            	unscheduleTopology(topology, cluster);

            	// Restart from benchmarking after X performance schedules
            	if(performanceRescheduledTimes.containsKey(topology.getId()) && performanceRescheduledTimes.get(topology.getId()) >= 3){
            		System.out.println("Reset - start from benchmarking... performance run times: " + performanceRescheduledTimes.get(topology.getId()).toString());
            		// Reset all
            		worstCaseDone.remove(topology.getId());
            		standardCaseDone.remove(topology.getId());
            		performanceRescheduledTimes.remove(topology.getId());
            		//endOfProfilingForTopology.remove(topology.getId());
            		//analysers.get(topology.getId()).setStartTime(new Date());
            	}
            }
            
            
            boolean needsScheduling = cluster.needsScheduling(topology);

            //
            // Do not schedule (just set reschedule time according to interval)
            //
            if (!needsScheduling) {
            	//System.out.println("Websotrm topology DOES NOT NEED scheduling.");
            	// Set rescheduling if not set for this topology
            	if(!reschedulingForTopology.containsKey(topology.getId())){
	            	// Find the rescheduling interval and set next reschedule
	            	Long reschedulingInterval = Long.parseLong(conf.get("advisor.analysis.rescheduling").toString());
	            	Date nextReschedule = new Date(new Date().getTime() + 1000 * reschedulingInterval);
	            	reschedulingForTopology.put(topology.getId(), nextReschedule);
            	}
            }
            //
            // SCHEDULE
            //
            else {
            	System.out.println("Webstorm topology needs scheduling.");
            	// Find the rescheduling interval and set next reschedule
        		Long reschedulingInterval = Long.parseLong(conf.get("advisor.analysis.rescheduling").toString());
        		Date nextReschedule = new Date(new Date().getTime() + 1000 * reschedulingInterval);
        		reschedulingForTopology.put(topology.getId(), nextReschedule);
        		//System.out.println("Next reschedule set to: " + nextReschedule.toString());
        		String scheduleName = null;
                
            	//
            	// Mark records in monitoring DB by scheduling type (after scheduling we mark entries from last reschedule) 
            	if(lastReschedulingForTopology.containsKey(topology.getId())){
            		//String sql = "UPDATE profiling SET schedule_type = '" + lastSchedule.get(topology.getId()) + "' WHERE "
                	//		+ "timestamp > '" + isoFormatter.format(lastReschedulingForTopology.get(topology.getId())) +
                	//		"' AND timestamp < '" + isoFormatter.format(new Date())+"'";
            		String sql = "INSERT INTO schedules(schedule_type, \"from\", \"to\", \"length\", deployment_id) VALUES ('" + lastSchedule.get(topology.getId()) + "', "
            				+ "'" + isoFormatter.format(lastReschedulingForTopology.get(topology.getId())) + "', "
            				+ "'" + isoFormatter.format(new Date())+"', " + reschedulingInterval.toString() + ", '" + conf.get("advisor.analysis.deploymentId").toString() + "')";
                	System.out.println("Setting the schedule type for old records... " + sql);
            		try {
	                	Statement stmt = conn.createStatement();
		                stmt.executeUpdate(sql);
		                stmt.close();
	        		} catch (SQLException e) {
	        			System.out.println(e.toString());
	        		}
            	}
            	// Write last rescheduling for this topology
            	lastReschedulingForTopology.put(topology.getId(), new Date());
            	
            	// Check if all hosts to executors were measured
            	// Schedule for performance
            	if(endOfProfilingForTopology.containsKey(topology.getId()) || allExecutorsToHostsMeasured(topology)){
            		// Clear rescheduling interval if set
            		//reschedulingForTopology.remove(topology.getId());
            		
            		// Worst case schedule for comparison
            		if(!worstCaseDone.contains(topology.getId())){
            			scheduleName = "Worstcase";
            			lastSchedule.put(topology.getId(), "worstcase");
            			System.out.println("==sched== Worst case scheduling (from, to): timestamp > '" + isoFormatter.format(new Date()) +
            					"' AND timestamp < '" + isoFormatter.format(reschedulingForTopology.get(topology.getId()))+"'");
            			
            			scheduleForPerformance(topology, cluster, true);
            			
            			worstCaseDone.add(topology.getId());
            		}
            		// Even scheduler for comparison
            		else if(!standardCaseDone.contains(topology.getId())){
            			scheduleName = "Standard";
            			lastSchedule.put(topology.getId(), "even_scheduler");
            			System.out.println("==sched== Standard scheduling (from, to): timestamp > '" + isoFormatter.format(new Date()) +
            					"' AND timestamp < '" + isoFormatter.format(reschedulingForTopology.get(topology.getId()))+"'");
            			
            			// Even scheduler as standard scheduling
            			System.out.println("EvenScheduler fired...");
            			new EvenScheduler().schedule(topologies, cluster);
            			System.out.println("EvenScheduler done...");
            			
            			standardCaseDone.add(topology.getId());
            		}
            		// Performance schedule
            		else{
            			scheduleName = "Heterogeneity";
            			lastSchedule.put(topology.getId(), "performance");
            			System.out.println("==sched== Performance scheduling (from, to): timestamp > '" + isoFormatter.format(new Date()) +
            					"' AND timestamp < '" + isoFormatter.format(reschedulingForTopology.get(topology.getId()))+"'");
            			
            			// Clear rescheduling interval if set
                		//reschedulingForTopology.remove(topology.getId()); // not work
                		
                		
                		scheduleForPerformance(topology, cluster, false);
                		
                		// Set the number of performance reschedules
                		Integer scheduledTimes = 0;
                		if(performanceRescheduledTimes.containsKey(topology.getId())){
                			scheduledTimes = performanceRescheduledTimes.get(topology.getId());
                		}
                		performanceRescheduledTimes.put(topology.getId(), scheduledTimes + 1);
            		}
            		
            	}
            	// Schedule for profiling
            	else{
            		scheduleName = "Benchmark";
            		lastSchedule.put(topology.getId(), "benchmark");
            		System.out.println("==sched== Benchmark scheduling (from, to): timestamp > '" + isoFormatter.format(new Date()) +
        					"' AND timestamp < '" + isoFormatter.format(reschedulingForTopology.get(topology.getId()))+"'");
            		
            		// Schedule executors to hosts where we don't have monitoring data
            		scheduleToNotObserved(topology, cluster);
            	}
            	
            	// Save resulting schedule to JSON file
            	this.writeScheduleToJson(cluster, topology, scheduleName);
            }
        }
    }
    
    
    /**
     * Schedule executors to best known suitable hosts. Starts with executor types with most
     * significant performance loss on other types of hardware.
     * 
     * The executors that cannot fit to free slots are paced to nodes running executors with 
     * least significant performance loss.
     * 
     * @param topology
     * @param cluster
     * @param worstPlacement
     */
    public void scheduleForPerformance(TopologyDetails topology, Cluster cluster, boolean worstPlacement)
    {
    	// Get the schedule
		String topologyId = topology.getId();
		Date endTime = endOfProfilingForTopology.get(topologyId);
		LinkedHashMap<String, LinkedList<String>> schedule = 
				analysers.get(topologyId).getBestPlacementsForExecutorTypes(endTime, worstPlacement);
		System.out.println("Schedule: " + schedule);
		
		// Find out all the needs-scheduling components of this topology
		Map<String, List<ExecutorDetails>> componentToExecutors = cluster.getNeedsSchedulingComponentToExecutors(topology);
		
		// Find all available slots and make hosts to slots mapping
        //List<WorkerSlot> allAvailableSlots = cluster.getAvailableSlots();
        
        // Map of to be placed executors per slot
    	HashMap<WorkerSlot, List<ExecutorDetails>> toBePlaced = new HashMap<WorkerSlot, List<ExecutorDetails>>();
		
		//
		// Placing executor types in given order to suitable hosts according the schedule
		//
    	LinkedList<String> lastHosts = null; // For later usage in additional scheduling
    	mainLoop:
		for(Entry<String, LinkedList<String>> e : schedule.entrySet()){
			LinkedList<String> suitableHosts = e.getValue();
			String executorType = e.getKey();
			lastHosts = suitableHosts;
			
			// Executors to be placed
			List<ExecutorDetails> executors = componentToExecutors.get(executorType);
			if(executors == null || executors.isEmpty()){
				continue mainLoop;
			}
			
			// Schedule as much executors as possible to each host ordered by suitability
			for(String host : suitableHosts){
				// Available slots for host
				List<SupervisorDetails> supervisors = cluster.getSupervisorsByHost(host);
				for(SupervisorDetails supervisor : supervisors){
					List<WorkerSlot> slots = cluster.getAvailableSlots(supervisor);
					for(WorkerSlot slot : slots){
						// Continue when all executors of given type were placed
						if(executors.isEmpty()){
							continue mainLoop;
						}
						// Skip already scheduled slots
						if(toBePlaced.containsKey(slot)){
							continue;
						}
						
						//List<ExecutorDetails> slotsExecs = toBePlaced.get(slot);
        				//if(slotsExecs == null){
        				//	slotsExecs = new LinkedList<ExecutorDetails>();
        				//}
						List<ExecutorDetails> slotsExecs = new LinkedList<ExecutorDetails>();
        				slotsExecs.add(executors.remove(0));
						toBePlaced.put(slot, slotsExecs);
					}
				}
			}
		}
    	
    	// Place the rest of executors (use last list of hosts in reverse order
    	noAvailableSlotsLoop:
    	for(int i = 0; i < 100; i++){
	    	ListIterator<String> hostsIterator = lastHosts.listIterator(lastHosts.size());
	    	//System.out.println("Additional scheduling hosts: " + lastHosts);
	        while(hostsIterator.hasPrevious()){
	        	String host = hostsIterator.previous();
	        	//System.out.println("Host: " + host); 
	        	mainLoop:
	        	for(List<ExecutorDetails> executors : componentToExecutors.values()){
	        		int slotsFound = 0;
	        		//System.out.println("Executors to place: " + executors.size());
		        	// Available slots for host
					List<SupervisorDetails> supervisors = cluster.getSupervisorsByHost(host);
					for(SupervisorDetails supervisor : supervisors){
						List<WorkerSlot> slots = cluster.getAvailableSlots(supervisor);
						slotsFound += slots.size();
						
						// Assign executors to slots
						for(WorkerSlot slot : slots){
							// Continue when all executors of given type were placed
							if(executors.isEmpty()){
								continue mainLoop;
							}
							
							List<ExecutorDetails> slotsExecs = toBePlaced.get(slot);
		    				if(slotsExecs == null){
		    					slotsExecs = new LinkedList<ExecutorDetails>();
		    				}
		    				slotsExecs.add(executors.remove(0));
							toBePlaced.put(slot, slotsExecs);
						}
					}
					// If no slots were found, free some (makes the rest of executors run on one host)
					// DOES NOT WORK - we have to first clean slots and then do all the scheduling
					//if(slotsFound == 0){
					//	cluster.freeSlots(cluster.getAssignableSlots(supervisors.get(0)));
					//	continue noAvailableSlotsLoop;
					//}
	        	}
	        }
	        break;
        }
    	
		
		// Schedule prepared executors to slots
        for(Entry<WorkerSlot, List<ExecutorDetails>> e : toBePlaced.entrySet()){
        	WorkerSlot slot = e.getKey();
        	if(!cluster.isSlotOccupied(slot)) // Just as a last check. If the slot is occupied, scheduling is left for now
        		cluster.assign(slot, topology.getId(), e.getValue());
        	else
        		System.out.println("Slot occupied: " + slot + " " + e.getValue());
        }
		
		System.out.println("Placed: " + toBePlaced);
    }
    
    
    /**
     * Schedule executors primarily to hosts where they did not run before.
     * Uses analyser to get information about already measured hosts to executors combinations. 
     * 
     * @param topology
     * @param cluster
     */
    public void scheduleToNotObserved(TopologyDetails topology, Cluster cluster)
    {
    	// Map of already measured executor types per host
    	Map<String, List<String>> measured = analysers.get(topology.getId()).getMeasuredHosts();
    	System.out.println("Measured: " + measured.toString());
    	
    	// Map of to be placed executor types per host
    	Map<String, List<String>> execsToHost = new HashMap<String, List<String>>();
    	
    	// Map of to be placed executors per slot
    	HashMap<WorkerSlot, List<ExecutorDetails>> toBePlaced = new HashMap<WorkerSlot, List<ExecutorDetails>>();
    	
    	
    	// Find out all the needs-scheduling components of this topology
        Map<String, List<ExecutorDetails>> componentToExecutors = cluster.getNeedsSchedulingComponentToExecutors(topology);
        // Find total number of executors
        int numExecutors = 0;
        for(List<ExecutorDetails> l : componentToExecutors.values()){
        	numExecutors += l.size();
        }
        
        // Find all available slots
        List<WorkerSlot> allAvailableSlots = cluster.getAvailableSlots();
        
        // Count how many executors do we have to put to each slot
        int executorRatio = Math.round(allAvailableSlots.size() / numExecutors);
        executorRatio = executorRatio == 0 ? 1 : executorRatio;
        
        System.out.println("Executor ratio: " + new Integer(executorRatio).toString());
        
        // Iterate slots and prepare lists of executors for each slot
        for (WorkerSlot slot : allAvailableSlots){
        	// Host by nodeId
        	String host = cluster.getSupervisorById(slot.getNodeId()).getHost();
        	System.out.println("Host: "+host);
        	// Repeat for each host multiple times according to executor ratio for each slot
        	for(int i = 0; i < executorRatio; i++){
        		// Not needed executor types for this host
        		HashSet<String> noNeedEexecTypes = new HashSet<String>();
    			if(measured.containsKey(host)){
            		noNeedEexecTypes.addAll(measured.get(host));
            		System.out.println("No need execs measured: " + measured.get(host));
    			}
    			if(execsToHost.containsKey(host)){
    				noNeedEexecTypes.addAll(execsToHost.get(host));
    				System.out.println("No need execs to be scheduled: " + execsToHost.get(host));
    			}
    			
    			System.out.println("No need execs: " + noNeedEexecTypes);
        		
        		// Look for suitable executor (first executor type that was not measured on this host)
        		for(String execType : componentToExecutors.keySet()){
        			//System.out.println("Exec to decide: " + execType);
        			if(!noNeedEexecTypes.contains(execType) && !componentToExecutors.get(execType).isEmpty()){
        				// Add executor type to unneeded
        				if(execsToHost.containsKey(host)){
        					execsToHost.get(host).add(execType);
        				}
        				else{
        					List<String> toAdd = new LinkedList<String>();
        					toAdd.add(execType);
        					execsToHost.put(host, toAdd);
        				}
        				// Add one executor of the type to this slot
        				List<ExecutorDetails> slotsExecs = toBePlaced.get(slot);
        				if(slotsExecs == null){
        					slotsExecs = new LinkedList<ExecutorDetails>();
        				}
        				slotsExecs.add(componentToExecutors.get(execType).remove(0));
        				toBePlaced.put(slot, slotsExecs);
        				
        				break;
        			}
        		}
        	}
        	
        }
        
        Map<String, SupervisorDetails> supervisors = cluster.getSupervisors();
        // Schedule the rest of unscheduled executors - host after host
        // These executors do not have any requirements for placement in this schedule
        // We need to do this round-robin over hosts multiple times
        Integer lastNeedSchedulingExecCount = null;
        for(int i = 0; i < 200; i++){
        	int needSchedulingExecCount = 0;
	        for(SupervisorDetails supervisor : supervisors.values()){
	        	List<WorkerSlot> availableSlots = cluster.getAvailableSlots(supervisor);
	        	
	        	// Skip supervisor without slots
	        	if(availableSlots.size() == 0){
	        		continue;
	        	}
	        	
	        	System.out.println("Supervisor for additional schedule: " + supervisor.getHost() + " Slots: " + availableSlots.size());
	        	//System.out.println("Slots: " + availableSlots.size());
	        	
	        	// Find and schedule unscheduled executors
	            for(List<ExecutorDetails> executors : componentToExecutors.values()){
	            	if(!executors.isEmpty()){
	            		needSchedulingExecCount += executors.size();
	            		System.out.println("Executors: " + executors.size());
	            		// Find best slot on supervisor
	            		WorkerSlot bestSlot = null;
	            		Integer bestSlotExecutors = null;
	            		for(WorkerSlot slot : availableSlots){
	            			List<ExecutorDetails> toBePlacedOnSlot = toBePlaced.get(slot);
	            			int size = 0;
	            			if(toBePlacedOnSlot != null){
	            				size = toBePlacedOnSlot.size();
	            			}
	            			if(bestSlotExecutors == null || size < bestSlotExecutors){
	            				bestSlotExecutors = size;
	            				bestSlot = slot;
	            			}
	            		}
	            		// Assign new executor to slot
	    				List<ExecutorDetails> slotsExecs = toBePlaced.get(bestSlot);
	    				if(slotsExecs == null){
	    					slotsExecs = new LinkedList<ExecutorDetails>();
	    				}
	    				slotsExecs.add(executors.remove(0));
	    				toBePlaced.put(bestSlot, slotsExecs);
	    				break;
	            	}
	            }
	        }
	        
	        // Decide if we need to go over supervisors (hosts) again
	        if(needSchedulingExecCount == 0){
	        	break;
	        }
	        /*
	        if(lastNeedSchedulingExecCount != null && needSchedulingExecCount == lastNeedSchedulingExecCount){
	        	System.out.println("Problems with scheduling, the number of unscheduled executors"+
	        			" didn't decrease from last iteration. Unscheduled executors: " + ((Integer)needSchedulingExecCount).toString());
	        	break;
	        }
	        */
	        lastNeedSchedulingExecCount = needSchedulingExecCount;
        }
        
        // Schedule prepared executors to slots
        for(Entry<WorkerSlot, List<ExecutorDetails>> e : toBePlaced.entrySet()){
        	WorkerSlot slot = e.getKey();
        	if(!cluster.isSlotOccupied(slot)) // Just as a last check. If the slot is occupied, scheduling is left to standard scheduler
        		cluster.assign(slot, topology.getId(), e.getValue());
        }
        
        System.out.print("Placed:\n" + toBePlaced.toString() + "\n");
    }
    
    /**
     * Free all slots used by given topology in cluster.
     * 
     * @param topology
     * @param cluster
     */
    public void unscheduleTopology(TopologyDetails topology, Cluster cluster)
    {
    	// Find all slots from this topology
    	//
    	// Assignment of this topology
    	SchedulerAssignment assignment = cluster.getAssignments().get(topology.getId());
    	// Map of executors to slots
    	Map<ExecutorDetails, WorkerSlot> executorToSlot = assignment.getExecutorToSlot();
    	
    	// Slots to be freed
    	Set<WorkerSlot> slotsToFree = new HashSet<WorkerSlot>(executorToSlot.values());
    	
    	System.out.println("Slots to free: " + slotsToFree.toString());
    	
    	// Free the slots used by this topology
    	cluster.freeSlots(slotsToFree);
    }

    
    /**
     * Checks if each executor were measured on each host.
     * Writes Date of end of profiling into endOfProfillingForTopology Map.
     * 
     * @param topology
     * @return Are all executor to host combinations measured?
     */
    public boolean allExecutorsToHostsMeasured(TopologyDetails topology)
    {
    	Map<String, List<String>> measured = analysers.get(topology.getId()).getMeasuredHosts();
    	
    	if(measured.isEmpty()){
    		return false;
    	}
    	
    	// Compare each hosts's executor types together, if all are the same, we're done
    	Collection<String> commonList = measured.remove(measured.keySet().iterator().next());
    	int size = commonList.size();
        for(List<String> l : measured.values()){
        	commonList = CollectionUtils.retainAll(commonList, l);
        }
    	
        // List of first host is same size as intersection of all lists together (all lists are the same)
        if(size == commonList.size()){
        	endOfProfilingForTopology.put(topology.getId(), new Date());
        	return true;
        }
        else{
        	return false;
        }
    }
    
    /**
     * Writes the current schedule to a JSON file. 
     */
    public void writeScheduleToJson(Cluster cluster, TopologyDetails topology, String scheduleName)
    {
    	SchedulerAssignment assignment = cluster.getAssignmentById(topology.getId());
    	Map<ExecutorDetails, WorkerSlot> executorToSlot = assignment.getExecutorToSlot();
    	
    	// Name of the bolt (component) from the executor details
    	Map<ExecutorDetails, String> executorToComponent = topology.getExecutorToComponent();
    	
    	Map<String, List<String>> placementMap = new HashMap<String, List<String>>();
    	
    	System.out.println("Placement JSON:");
        for(Entry<ExecutorDetails, WorkerSlot> e : executorToSlot.entrySet()){
        	WorkerSlot slot = e.getValue();
        	SupervisorDetails supervisor = cluster.getSupervisorById(slot.getNodeId());
        	
        	String componentName = executorToComponent.get(e.getKey());
        	String host = supervisor.getHost();
        	
        	// We have to skip the "__acker" components
        	if(componentName.equals("__acker")){
        		continue;
        	}
        	
        	if(!placementMap.containsKey(host)){
        		placementMap.put(host, new ArrayList<String>());
        	}
        	placementMap.get(host).add(componentName);
        }
        
        // Create JSON from the map
        String placementJson = "{"; //placementMap.toString().replace('=', ':'); // replace "=" to ":" and get JSON structure...
        for(Entry<String, List<String>> e : placementMap.entrySet()){
        	placementJson += "\"" + e.getKey() + "\":[";
        	for(String h : e.getValue()){
        		placementJson += "\"" + h + "\",";
        	}
        	placementJson += "],";
        }
        placementJson += "}";
        // Correct the wrong "," in JSON
        placementJson = placementJson.replace(",]", "]").replace(",}", "}");
        
        
        
        Date currentTimestamp = new Date();
        
        // The rest of the JSON data (info and timestamp)
        placementJson = "{\"info\":\"" + scheduleName + "\",\"timestamp\":\""+ isoFormatter.format(currentTimestamp) +"\",\"placement\":" 
        		+ placementJson + "}";
        
        // Write to file
        DateFormat timestampForFilenameFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String path=System.getProperty("user.home")+"/public_html/";
        String filename = "placement-" + scheduleName + "-" + timestampForFilenameFormatter.format(currentTimestamp) + ".json";
        try{
	        PrintWriter writer = new PrintWriter(path + filename, "UTF-8");
	        writer.print(placementJson);
	        writer.close();
        }
        catch(FileNotFoundException e){
        	System.out.println(e.toString());
        }
        catch(UnsupportedEncodingException e){
        	System.out.println(e.toString());
        }
        
        // Save placement to list of JSON placements and write the list to file
        if(!jsonPlacementFiles.containsKey(topology.getId())){
        	jsonPlacementFiles.put(topology.getId(), new ArrayList<String>());
    	}
        jsonPlacementFiles.get(topology.getId()).add(filename);
        // Prepare list in JSON
        String placementListJson = "{\"data\":[";
        for(String file : jsonPlacementFiles.get(topology.getId())){
        	placementListJson += "\"" + file + "\",";
        }
        placementListJson += "]}";
        // Correct the wrong "," in JSON
        placementListJson = placementListJson.replace(",]", "]").replace(",}", "}");
        
        try{
	        PrintWriter writer = new PrintWriter(path + "list.json", "UTF-8");
	        writer.print(placementListJson);
	        writer.close();
        }
        catch(FileNotFoundException e){
        	System.out.println(e.toString());
        }
        catch(UnsupportedEncodingException e){
        	System.out.println(e.toString());
        }
    }
}
