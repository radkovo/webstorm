/**
 * RssMonitorTopology.java
 *
 * Created on 28. 2. 2014, 13:24:53 by burgetr
 */
package org.fit.burgetr.webstorm.topologies;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.UUID;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.topology.TopologyBuilder;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;


import org.fit.burgetr.webstorm.bolts.AnalyzerBolt;
import org.fit.burgetr.webstorm.bolts.DownloaderBolt;
import org.fit.burgetr.webstorm.bolts.ExtractFeaturesBolt;
import org.fit.burgetr.webstorm.bolts.FeedReaderBolt;
import org.fit.burgetr.webstorm.bolts.IndexBolt;
import org.fit.burgetr.webstorm.bolts.NKStoreBolt;
import org.fit.burgetr.webstorm.spouts.FeedURLSpout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author burgetr
 */
public class RssMonitorTopologyDistr
{

    public static void main(String[] args) throws SQLException, UnknownHostException, AlreadyAliveException, InvalidTopologyException
    {
        //logging status
        Logger logger = LoggerFactory.getLogger(RssMonitorTopology.class);
        logger.debug("TOPOLOGY START");
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusPrinter.print(lc);
        
        String uuid=UUID.randomUUID().toString();
        
        
        //create spouts and bolt
        FeedURLSpout urlSpout = new FeedURLSpout("http://www.fit.vutbr.cz/~burgetr/public/rss.txt",uuid);
        FeedReaderBolt reader = new FeedReaderBolt(uuid);
        DownloaderBolt downloader = new DownloaderBolt(uuid);
        AnalyzerBolt analyzer = new AnalyzerBolt("kw","img",uuid);
        ExtractFeaturesBolt extractor = new ExtractFeaturesBolt(uuid);
        IndexBolt indexer=new IndexBolt(uuid);
        //NKStoreBolt nkstore = new NKStoreBolt();
        
        //create the topology
        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("FeedUrlSpout", urlSpout, 4);
        builder.setBolt("FeedReaderBolt", reader, 1).shuffleGrouping("FeedUrlSpout");
        builder.setBolt("DownloaderBolt", downloader, 4).shuffleGrouping("FeedReaderBolt");
        builder.setBolt("AnalyzerBolt", analyzer, 3).shuffleGrouping("DownloaderBolt");
        builder.setBolt("ExtractFeaturesBolt", extractor, 1).globalGrouping("AnalyzerBolt", "img");
        builder.setBolt("IndexBolt", indexer,1).shuffleGrouping("ExtractFeaturesBolt");
        //builder.setBolt("nkstore", nkstore, 1).globalGrouping("analyzer", "kw");

        Config conf = new Config();
        conf.setDebug(true);
        conf.put(Config.TOPOLOGY_DEBUG, true);
        conf.setNumWorkers(8);
        conf.setMaxSpoutPending(5000);
        
        // Configure supervisors for spout and bolt types
        //conf.put("placement.analyzer", "blade6.blades");
        //conf.put("placement.reader", "knot27.fit.vutbr.cz");
        //conf.put("placement.downloader", "blade5.blades");
        
        // Configure the start time for analysis in ISO 8601
        conf.put("advisor.analysis.startTime", "2014-05-31 17:30:00");
        // Rescheduling interval in seconds
        conf.put("advisor.analysis.rescheduling", 30);
        
        // Submit topology
        StormSubmitter.submitTopology("Webstorm", conf, builder.createTopology());

        //final LocalCluster cluster = new LocalCluster();
        
        /*Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                System.out.println("Shutting down");
                cluster.shutdown();
            }
        });*/
        
        //cluster.submitTopology("webstorm", conf, builder.createTopology());
    }

}
