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
public class RssMonitorTopology
{

    public static void main(String[] args)
    {
        //logging status
        Logger logger = LoggerFactory.getLogger(RssMonitorTopology.class);
        logger.debug("TOPOLOGY START");
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusPrinter.print(lc);
        
        //create spouts and bolt
        FeedURLSpout urlSpout = new FeedURLSpout("http://www.fit.vutbr.cz/~burgetr/public/rss.txt");
        FeedReaderBolt reader = new FeedReaderBolt();
        DownloaderBolt downloader = new DownloaderBolt();
        AnalyzerBolt analyzer = new AnalyzerBolt("kw","img");
        ExtractFeaturesBolt extractor = new ExtractFeaturesBolt();
        IndexBolt indexer=new IndexBolt();
        //NKStoreBolt nkstore = new NKStoreBolt();
        
        //create the topology
        TopologyBuilder builder = new TopologyBuilder();

        builder.setSpout("url_spout", urlSpout, 5);
        builder.setBolt("reader", reader).shuffleGrouping("url_spout");
        builder.setBolt("downloader", downloader, 1).shuffleGrouping("reader");
        builder.setBolt("analyzer", analyzer, 1).shuffleGrouping("downloader");
        builder.setBolt("extractor", extractor,1).globalGrouping("analyzer", "img");
        builder.setBolt("indexer", indexer,1).shuffleGrouping("extractor");
        //builder.setBolt("nkstore", nkstore, 1).globalGrouping("analyzer", "kw");

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
