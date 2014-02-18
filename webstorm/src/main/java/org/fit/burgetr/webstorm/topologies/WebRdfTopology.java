/**
 * 
 */
package org.fit.burgetr.webstorm.topologies;

import org.fit.burgetr.webstorm.bolts.RDFStorageBolt;
import org.fit.burgetr.webstorm.bolts.WebRdfBolt;
import org.fit.burgetr.webstorm.spouts.URLSpout;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.topology.TopologyBuilder;

/**
 *
 * @author burgetr
 */
public class WebRdfTopology
{

    public static void main(String[] args)
    {
        TopologyBuilder builder = new TopologyBuilder();

        URLSpout urlSpout = new URLSpout("http://www.fit.vutbr.cz/~burgetr/public/urls.txt");
        WebRdfBolt rdfBolt = new WebRdfBolt();
        RDFStorageBolt storageBolt = new RDFStorageBolt("/tmp/storage");
        
        // spout with 5 parallel instances
        builder.setSpout("url_spout", urlSpout, 5);
        builder.setBolt("rdf_bolt", rdfBolt).shuffleGrouping("url_spout");
        builder.setBolt("storage_bolt", storageBolt).globalGrouping("rdf_bolt");

        Config conf = new Config();
        conf.setDebug(true);

        final LocalCluster cluster = new LocalCluster();
        
        /*Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                System.out.println("Shutting down");
                cluster.shutdown();
            }
        });*/
        
        cluster.submitTopology("webstorm", conf, builder.createTopology());
    }

}
