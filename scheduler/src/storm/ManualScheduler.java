package storm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
	
	public void prepare(Map config) {
	} 


    public void schedule(Topologies topologies, Cluster cluster) {
    	System.out.println("ManualScheduler: begin scheduling");
        // Gets the topology which we want to schedule
        TopologyDetails topology = topologies.getByName("Webstorm");


        // Make sure our topology is submitted,
        if (topology != null) {
        	System.out.println("Assigned workers:" + cluster.getAssignedNumWorkers(topology));
            boolean needsScheduling = cluster.needsScheduling(topology);
            
            // Configuration of topology
            Map conf = topology.getConf(); 


            if (!needsScheduling) {
            	System.out.println("Our special topology DOES NOT NEED scheduling.");
            } else {
            	System.out.println("Our special topology needs scheduling.");
                // find out all the needs-scheduling components of this topology
                Map<String, List<ExecutorDetails>> componentToExecutors = cluster.getNeedsSchedulingComponentToExecutors(topology);
                
                /*
                System.out.println("needs scheduling(component->executor): " + componentToExecutors);
                System.out.println("needs scheduling(executor->compoenents): " + cluster.getNeedsSchedulingExecutorToComponents(topology));
                SchedulerAssignment currentAssignment = cluster.getAssignmentById(topologies.getByName("Webstorm").getId());
                if (currentAssignment != null) {
                	System.out.println("current assignments: " + currentAssignment.getExecutorToSlot());
                } else {
                	System.out.println("current assignments: {}");
                }
                */
                
                
                // Iterate Executors that need scheduling and check config for placement requirements
                for (Entry<String, List<ExecutorDetails>> e : componentToExecutors.entrySet()){
                	String confKey = "placement."+e.getKey();
                	List<ExecutorDetails> executors = e.getValue();
                	
                	System.out.println("Looking for config for executor: "+confKey);
                	
                	// Skip the executors without placement defined in configuration
                	if(!conf.containsKey(confKey)){
                		continue;
                	}
                	
                	// Prepare set of required nodes from list in config
                	Set<String> requiredNodes = new HashSet<String>(Arrays.asList(conf.get(confKey).toString().split(",[ ]*")));
                	
                	System.out.println("Hosts for executor '"+e.getKey()+"' :" + requiredNodes.toString());
                	System.out.println("Executors to be placed:" + executors.toString());
                	
                	
                	// Schedule to supervisors
                	Collection<SupervisorDetails> supervisors = cluster.getSupervisors().values();
                	//while(!requiredNodes.isEmpty() && !executors.isEmpty()){
                		boolean hostsFound = false; // flag if there were any suitable hosts found
	                	for (SupervisorDetails supervisor : supervisors) {
	                		if(requiredNodes.contains(supervisor.getHost()) && !executors.isEmpty()){
	                			List<WorkerSlot> availableSlots = cluster.getAvailableSlots(supervisor);
	                			
	                			// Skip this supervisor if there are no more slots
	                			// at the same time we remove the node from list of requiredNodes
	                			if(availableSlots.isEmpty()){
	                				requiredNodes.remove(supervisor.getHost());
	                				continue;
	                			}
	                			
	                			hostsFound = true;
	                			
	                			List<ExecutorDetails> toAssign = new ArrayList<ExecutorDetails>();
	                			toAssign.add(executors.remove(0));
	                			
	                			// Assign one executor to current supervisor
	                			cluster.assign(availableSlots.get(0), topology.getId(), executors);
	                            System.out.println("We assigned executors:" + toAssign + 
	                            		" to slot: [" + availableSlots.get(0).getNodeId() + ", " + availableSlots.get(0).getPort() + "]");
	                            break;
	                		}
	                	}
	                	// exit the while if there were no suitable hosts found
	                	if(!hostsFound){
	                		System.out.println("No available hosts found.");
	                		//break;
	                	}
                	//}
                }
                
                //*
                if (!componentToExecutors.containsKey("special-spout")) {
                	System.out.println("Our special-spout DOES NOT NEED scheduling.");
                } else {
                    System.out.println("Our special-spout needs scheduling.");
                    List<ExecutorDetails> executors = componentToExecutors.get("special-spout");


                    // find out the our "special-supervisor" from the supervisor metadata
                    Collection<SupervisorDetails> supervisors = cluster.getSupervisors().values();
                    SupervisorDetails specialSupervisor = null;
                    for (SupervisorDetails supervisor : supervisors) {
                        Map meta = (Map) supervisor.getSchedulerMeta();


                        if (meta.get("name").equals("special-supervisor")) {
                            specialSupervisor = supervisor;
                            break;
                        }
                    }


                    // found the special supervisor
                    if (specialSupervisor != null) {
                    	System.out.println("Found the special-supervisor");
                        List<WorkerSlot> availableSlots = cluster.getAvailableSlots(specialSupervisor);
                        
                        // if there is no available slots on this supervisor, free some.
                        // TODO for simplicity, we free all the used slots on the supervisor.
                        if (availableSlots.isEmpty() && !executors.isEmpty()) {
                            for (Integer port : cluster.getUsedPorts(specialSupervisor)) {
                                cluster.freeSlot(new WorkerSlot(specialSupervisor.getId(), port));
                            }
                        }


                        // re-get the aviableSlots
                        availableSlots = cluster.getAvailableSlots(specialSupervisor);


                        // since it is just a demo, to keep things simple, we assign all the
                        // executors into one slot.
                        cluster.assign(availableSlots.get(0), topology.getId(), executors);
                        System.out.println("We assigned executors:" + executors + " to slot: [" + availableSlots.get(0).getNodeId() + ", " + availableSlots.get(0).getPort() + "]");
                    } else {
                    	System.out.println("There is no supervisor named special-supervisor!!!");
                    }
                }
                //*/
            }
        }
        
        // let system's even scheduler handle the rest scheduling work
        // you can also use your own other scheduler here, this is what
        // makes storm's scheduler composable.
        System.out.println("EvenScheduler fired...");
        new EvenScheduler().schedule(topologies, cluster);

    }
}
