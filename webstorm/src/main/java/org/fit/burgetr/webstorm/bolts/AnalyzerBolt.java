/**
 * AnalyzerBolt.java
 *
 * Created on 4. 3. 2014, 11:42:04 by burgetr
 */
package org.fit.burgetr.webstorm.bolts;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.burgetr.segm.Segmentator;
import org.burgetr.segm.tagging.taggers.PersonsTagger;
import org.burgetr.segm.tagging.taggers.Tagger;
import org.fit.burgetr.webstorm.util.LogicalTagLookup;
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
 * A bolt that analyzes a web page and emits the discovered name-keyword and name-image relationships.
 * Accepts: (title, base_url, html_code)
 * Emits: (name, keyword, base_url)+
 * 
 * @author burgetr
 */
public class AnalyzerBolt implements IRichBolt
{
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(AnalyzerBolt.class);
    private OutputCollector collector;
    

    @SuppressWarnings("rawtypes")
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector)
    {
        this.collector = collector;
    }

    public void execute(Tuple input)
    {
        String baseurl = input.getString(1);
        String html = input.getString(2);
        
        Map<String, Set<String>> keywords;
        try
        {
            keywords = processUrl(html, new URL(baseurl));
            if (keywords != null)
            {
                for (Map.Entry<String, Set<String>> entry : keywords.entrySet())
                {
                    String name = entry.getKey();
                    for (String keyword : entry.getValue())
                    {
                        if (!keyword.equals(name))
                            collector.emit(new Values(name, keyword, baseurl));
                    }
                }
                collector.ack(input);
            }
            else
                collector.fail(input);
        }
        catch (MalformedURLException e)
        {
            collector.fail(input);
        }
    }

    public void cleanup()
    {
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer)
    {
        declarer.declare(new Fields("subject", "predicate", "object"));
    }

    public Map<String, Object> getComponentConfiguration()
    {
        return null;
    }
    
    //===========================================================================================
    
    private Map<String, Set<String>> processUrl(String html, URL baseurl)
    {
        try
        {
            InputStream is = new ByteArrayInputStream(html.getBytes("UTF-8"));
            Segmentator segm = new Segmentator();
            segm.segmentInputStream(is, baseurl);
            
            Tagger p = new PersonsTagger(1);
            LogicalTagLookup lookup = new LogicalTagLookup(segm.getLogicalTree());
            Map<String, List<String>> related = lookup.findRelatedText(p);
            Map<String, Set<String>> keywords = lookup.extractRelatedKeywords(related);
            
            return keywords;
            
        } catch (Exception e)
        {
            //e.printStackTrace();
            log.error(e.getMessage());
            return null;
        }
    }

}
