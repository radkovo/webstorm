/**
 * URLSpout.java
 *
 * Created on 13. 2. 2014, 14:57:33 by burgetr
 */
package org.fit.burgetr.webstorm.spouts;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

/**
 * 
 * @author burgetr
 */
public class URLSpout extends BaseRichSpout
{
    private static final long serialVersionUID = 1L;

    private SpoutOutputCollector collector;
    private Vector<String> urls;
    private Iterator<String> urlIterator;
    private String listSourceUrl;
    
    public URLSpout(String listSourceUrl)
    {
        this.listSourceUrl = listSourceUrl;
    }
    
    @SuppressWarnings("rawtypes")
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector)
    {
        this.collector = collector;
        loadList(listSourceUrl);
    }
    
    public void nextTuple()
    {
        if (!urlIterator.hasNext())
            urlIterator = urls.iterator();
        String urlstring = urlIterator.next();
        collector.emit(new Values(urlstring));
    }

    @Override
    public void ack(Object id)
    {
    }

    @Override
    public void fail(Object id)
    {
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer)
    {
        declarer.declare(new Fields("url"));
    }

    //===============================================================================================
    
    private void loadList(String urlstring)
    {
        try {
            URL url = new URL(urlstring);
            URLConnection con = url.openConnection();
            BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()));
            urls = new Vector<String>();
            String line;
            while ((line = r.readLine()) != null)
            {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#"))
                    urls.add(line);
            }
            r.close();
            if (urls.isEmpty())
                urls.add("http://www.fit.vutbr.cz");
            urlIterator = urls.iterator();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
