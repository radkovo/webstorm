/**
 * FeedReaderBolt.java
 *
 * Created on 28. 2. 2014, 11:34:14 by burgetr
 */
package org.fit.burgetr.webstorm.bolts;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

import org.fit.burgetr.webstorm.util.Monitoring;
import org.rometools.fetcher.FeedFetcher;
import org.rometools.fetcher.impl.HttpURLFeedFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * A bolt that decodes a feed at the given URL and extracts new entries
 * Accepts: (feed_url, time_of_last_fetch)
 * Emits: (extracted_url, title)+
 * 
 * @author burgetr
 */
public class FeedReaderBolt implements IRichBolt
{
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(FeedReaderBolt.class);
    private OutputCollector collector;
    private String webstormId;
    private Monitoring monitor;
    private String hostname;
    
    
    public FeedReaderBolt(String uuid) throws SQLException {
    	webstormId=uuid;
    	monitor=new Monitoring(webstormId);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector)
    {
        this.collector = collector;
        try{
			hostname=InetAddress.getLocalHost().getHostName();
		}
		catch(UnknownHostException e){
			hostname="-unknown-";
		}
    }

    @Override
    public void execute(Tuple input)
    {
    	long startTime = System.nanoTime();
    	
        String urlstring = input.getString(0);
        Date date = new Date(input.getLong(1));
        String uuid=input.getString(2);
        
        log.info("Processing url: " + urlstring + " last modified on " + date);
        
        try
        {
            FeedFetcher feedFetcher = new HttpURLFeedFetcher();
            SyndFeed feed = feedFetcher.retrieveFeed(new URL(urlstring));
            
            List<?> entries = feed.getEntries();
            for (Object e : entries)
            {
                if (e instanceof SyndEntry)
                {
                    SyndEntry entry = (SyndEntry) e;
                    if (date.compareTo(entry.getPublishedDate()) <= 0)
                    {
                        log.info("New entry: " + entry.getTitle() + " " + entry.getUri() + " " + entry.getPublishedDate());
                        Long estimatedTime = System.nanoTime() - startTime;
                        monitor.MonitorTuple("FeedReaderBolt", uuid, hostname, estimatedTime);
                        collector.emit(new Values(entry.getUri(), entry.getTitle(),uuid));
                    }
                }
            }
            
            collector.ack(input);
        } 
        catch (Exception e)
        {
            log.error("Fetch error: " + e.getMessage());
            collector.fail(input);
        }
        
    }

    @Override
    public void cleanup()
    {
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer)
    {
        declarer.declare(new Fields("url", "title","uuid"));
    }

    @Override
    public Map<String, Object> getComponentConfiguration()
    {
        return null;
    }

}
