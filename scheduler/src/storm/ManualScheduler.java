package storm;

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
	// Next rescheduling times
	private Map<TopologyDetails, Date> reschedulingForTopology = new HashMap<TopologyDetails, Date>();
	// Analysers for topologies
	private Map<TopologyDetails, Analyser> analysers = new HashMap<TopologyDetails, Analyser>();
	
	
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
        	if(!analysers.containsKey(topology)){
        		analysers.put(topology, new Analyser(topology));
        	}
        	
        	System.out.println("Assigned workers:" + cluster.getAssignedNumWorkers(topology));
            boolean needsScheduling = cluster.needsScheduling(topology);
            
            // Configuration of topology
            Map conf = topology.getConf(); 
            
            // Find the rescheduling interval
            Long reschedulingInterval = (Long)conf.get("advisor.analysis.rescheduling");
            
            // Schedule if needed
            if (!needsScheduling) {
            	System.out.println("Websotrm topology DOES NOT NEED scheduling.");
            } else {
            	System.out.println("Webstorm topology needs scheduling.");
                
            	// Schedule executors to hosts where we don't have monitoring data
            	scheduleToNotObserved(cluster, topology);
            }
        }
        
        // Let system's even scheduler handle the rest scheduling work
        // you can also use your own other scheduler here.
        System.out.println("EvenScheduler fired...");
        new EvenScheduler().schedule(topologies, cluster);

    }
    
    
    public void scheduleToNotObserved(Cluster cluster, TopologyDetails topology)
    {
    	// Map of already measured executor types per host
    	//Map<String, List<String>> measured = new HashMap<String, List<String>>();
    	//measured.put("knot04.fit.vutbr.cz", Arrays.asList("reader", "downloader"));
    	//measured.put("blade5.blades", Arrays.asList("extractor", "downloader", "analyzer"));
    	Map<String, List<String>> measured = analysers.get(topology).getMeasuredHosts();
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
        	for(int i = 0; i < executorRatio; i++){
        		// Not needed executor types for this host
        		HashSet<String> noNeedEexecTypes = new HashSet<String>();
    			if(measured.size() > 0 && execsToHost.size() > 0){
            		noNeedEexecTypes.addAll(measured.get(host));
        			noNeedEexecTypes.addAll(execsToHost.get(host));
    			}
        		
        		// Look for suitable executor
        		for(String execType : componentToExecutors.keySet()){
        			if(!noNeedEexecTypes.contains(execType) && componentToExecutors.get(execType).size() > 0){
        				// Add executor type to unneeded
        				if(execsToHost.containsKey(host)){
        					execsToHost.get(host).add(execType);
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
        for(SupervisorDetails supervisor : supervisors.values()){
        	List<WorkerSlot> slots = cluster.getAvailableSlots(supervisor);
        	
        	// Skip supervisor without slots
        	if(slots.size() == 0){
        		continue;
        	}
        	
        	System.out.println("Supervisor for additional schedule: " + supervisor.getHost());
        	System.out.println("Slots: " + slots.size());
        	
        	// Find and schedule unscheduled executors
            for(List<ExecutorDetails> executors : componentToExecutors.values()){
            	if(executors.size() > 0){
            		// Find best slot on supervisor
            		WorkerSlot bestSlot = null;
            		Integer bestSlotExecutors = null;
            		for(WorkerSlot slot : slots){
            			int size = toBePlaced.get(slot).size();
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
        
        // Schedule prepared executors to slots
        for(Entry<WorkerSlot, List<ExecutorDetails>> e : toBePlaced.entrySet()){
        	WorkerSlot slot = e.getKey();
        	if(!cluster.isSlotOccupied(slot)) // Just as a last check. If the slot is occupied, scheduling is left to standard scheduler
        		cluster.assign(slot, topology.getId(), e.getValue());
        }
        
        System.out.print("Placed:\n" + toBePlaced.toString() + "\n");
    }
}
