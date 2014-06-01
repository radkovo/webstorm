package storm;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import backtype.storm.scheduler.Cluster;
import backtype.storm.scheduler.EvenScheduler;
import backtype.storm.scheduler.ExecutorDetails;
import backtype.storm.scheduler.IScheduler;
import backtype.storm.scheduler.SchedulerAssignment;
import backtype.storm.scheduler.SupervisorDetails;
import backtype.storm.scheduler.Topologies;
import backtype.storm.scheduler.TopologyDetails;
import backtype.storm.scheduler.WorkerSlot;

public class ManualScheduler implements IScheduler {
	// Next rescheduling times by topologies (topology ID)
	private Map<String, Date> reschedulingForTopology = new HashMap<String, Date>();
	// Analysers for topologies (topology ID)
	private Map<String, Analyser> analysers = new HashMap<String, Analyser>();
	
	
	public void prepare(Map config) {
		
	} 

	
	/**
	 * Schedule and reschedule first to gather profiling data about host/executor
	 * and after getting sufficient monitoring data prepare the "best" possible schedule
	 * based on data gathered during first phase.
	 * 
	 * For unsolved scheduling fires default scheduler.
	 */
    public void schedule(Topologies topologies, Cluster cluster) {
    	System.out.println("ManualScheduler: begin scheduling");
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
            // Check if the rescheduling is needed, if so, unschedule topology
            if(reschedulingForTopology.containsKey(topology.getId()) && new Date().after(reschedulingForTopology.get(topology.getId()))){
            	// Find all slots from this topology
            	// Assignment of this topology
            	SchedulerAssignment assignment = cluster.getAssignments().get(topology.getId());
            	// Map of executors to slots
            	Map<ExecutorDetails, WorkerSlot> executorToSlot = assignment.getExecutorToSlot();
            	
            	Set<WorkerSlot> slotsToFree = new HashSet<WorkerSlot>(executorToSlot.values());
            	
            	System.out.println("Slots to free: " + slotsToFree.toString());
            	
            	// Free the slots used by this topology
            	cluster.freeSlots(slotsToFree);
            }
            
            boolean needsScheduling = cluster.needsScheduling(topology);

            // Schedule if needed
            if (!needsScheduling) {
            	System.out.println("Websotrm topology DOES NOT NEED scheduling.");
            	
            	// Set rescheduling if not set for this topology
            	if(!reschedulingForTopology.containsKey(topology.getId())){
	            	// Find the rescheduling interval and set next reschedule
	            	Long reschedulingInterval = Long.parseLong(conf.get("advisor.analysis.rescheduling").toString());
	            	Date nextReschedule = new Date(new Date().getTime() + 1000 * reschedulingInterval);
	            	reschedulingForTopology.put(topology.getId(), nextReschedule);
            	}
            } else {
            	System.out.println("Webstorm topology needs scheduling.");
                
            	// Find the rescheduling interval and set next reschedule
            	Long reschedulingInterval = Long.parseLong(conf.get("advisor.analysis.rescheduling").toString());
            	Date nextReschedule = new Date(new Date().getTime() + 1000 * reschedulingInterval);
            	reschedulingForTopology.put(topology.getId(), nextReschedule);
            	//System.out.println("Next reschedule set to: " + nextReschedule.toString());
            	
            	// Schedule executors to hosts where we don't have monitoring data
            	scheduleToNotObserved(cluster, topology);
            }
        }
        
        // Let system's even scheduler handle the rest scheduling work
        // you can also use your own other scheduler here.
        System.out.println("EvenScheduler fired...");
        new EvenScheduler().schedule(topologies, cluster);

    }
    
    
    /**
     * Schedule executors primarily to hosts where they did not run before.
     * Uses analyser to get information about already measured hosts to executors combinations. 
     * 
     * @param cluster
     * @param topology
     */
    public void scheduleToNotObserved(Cluster cluster, TopologyDetails topology)
    {
    	// Map of already measured executor types per host
    	//Map<String, List<String>> measured = new HashMap<String, List<String>>();
    	//measured.put("knot04.fit.vutbr.cz", Arrays.asList("reader", "downloader"));
    	//measured.put("blade5.blades", Arrays.asList("extractor", "downloader", "analyzer"));
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
}
