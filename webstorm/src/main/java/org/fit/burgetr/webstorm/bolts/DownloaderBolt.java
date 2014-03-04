/**
 * DownloaderBolt.java
 *
 * Created on 3. 3. 2014, 13:39:59 by burgetr
 */
package org.fit.burgetr.webstorm.bolts;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.fit.cssbox.demo.StyleImport;
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
 * A bolt that downloads a HTML and the corresponding CSS files and completes a single file.
 * Accepts: (page_url, title)
 * Emits: (title, base_url, html_code)
 * 
 * @author burgetr
 */
public class DownloaderBolt implements IRichBolt
{
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DownloaderBolt.class);
    private OutputCollector collector;
    

    @SuppressWarnings("rawtypes")
    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector)
    {
        this.collector = collector;
    }

    @Override
    public void execute(Tuple input)
    {
        String urlstring = input.getString(0);
        String title = input.getString(1);
        
        log.info("Downloading url: " + urlstring);
        
        try
        {
            StyleImport si = new StyleImport(urlstring);
            StringWriter os = new StringWriter();
            si.dumpTo(new PrintWriter(os));
            os.close();
            
            collector.emit(new Values(title, urlstring, os.toString()));
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
        declarer.declare(new Fields("title", "base_url", "code"));
    }

    @Override
    public Map<String, Object> getComponentConfiguration()
    {
        return null;
    }

}
