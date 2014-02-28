/**
 * FeedURLSpout.java
 *
 * Created on 28. 2. 2014, 11:39:44 by burgetr
 */
package org.fit.burgetr.webstorm.spouts;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

/**
 * A spouts that reads a list of feed urls and emits the urls repeatedly together with last fetch date.
 * Emits: (url, last_fetch_date[unix_time])
 * 
 * @author burgetr
 */
public class FeedURLSpout extends BaseRichSpout
{

    private static final long serialVersionUID = 1L;

    private SpoutOutputCollector collector;
    private Map<String, Date> urls;
    private Iterator<Entry<String, Date>> urlIterator;
    private String listSourceUrl;
    
    public FeedURLSpout(String listSourceUrl)
    {
        this.listSourceUrl = listSourceUrl;
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector)
    {
        this.collector = collector;
        loadList(listSourceUrl);
    }
    
    @Override
    public void nextTuple()
    {
        if (urlIterator == null || !urlIterator.hasNext())
        {
            if (urlIterator != null)
            {
                try
                {
                    Thread.sleep(2000); //wait 2 seconds in order not to repeat the whole list with the same times
                } catch (InterruptedException e) {}
            }
            urlIterator = urls.entrySet().iterator();
        }
        Entry<String, Date> entry = urlIterator.next();
        Date now = new Date();
        collector.emit(new Values(entry.getKey(), entry.getValue().getTime()));
        entry.setValue(now);
    }

    @Override
    public void ack(Object id)
    {
    }

    @Override
    public void fail(Object id)
    {
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer)
    {
        declarer.declare(new Fields("url", "lastfetch"));
    }

    //===============================================================================================
    
    private void loadList(String urlstring)
    {
        try {
            URL url = new URL(urlstring);
            URLConnection con = url.openConnection();
            BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()));
            urls = new HashMap<String, Date>();
            
            Date initDate = new Date(0); //initial date
            String line;
            while ((line = r.readLine()) != null)
            {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#"))
                    urls.put(line, initDate); 
            }
            r.close();
            
            //fallback - empty list
            if (urls.isEmpty())
                urls.put("http://rss.cnn.com/rss/cnn_latest.rss", initDate);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
