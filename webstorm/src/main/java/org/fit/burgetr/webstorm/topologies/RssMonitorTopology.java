/**
 * RssMonitorTopology.java
 *
 * Created on 28. 2. 2014, 13:24:53 by burgetr
 */
package org.fit.burgetr.webstorm.topologies;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.topology.TopologyBuilder;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

import org.fit.burgetr.webstorm.bolts.FeedReaderBolt;
import org.fit.burgetr.webstorm.spouts.FeedURLSpout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author burgetr
 */
public class RssMonitorTopology
{

    public static void main(String[] args)
    {
        //logging status
        Logger logger = LoggerFactory.getLogger(WebRdfTopology.class);
        logger.debug("TOPOLOGY START");
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusPrinter.print(lc);
        
        
        TopologyBuilder builder = new TopologyBuilder();

        FeedURLSpout urlSpout = new FeedURLSpout("http://www.fit.vutbr.cz/~burgetr/public/rss.txt");
        FeedReaderBolt readerBolt = new FeedReaderBolt(); 
        
        // spout with 5 parallel instances
        builder.setSpout("url_spout", urlSpout, 5);
        builder.setBolt("reader_bolt", readerBolt).shuffleGrouping("url_spout");

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
