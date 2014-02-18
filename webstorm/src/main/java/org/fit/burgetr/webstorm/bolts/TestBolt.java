/**
 * 
 */
package org.fit.burgetr.webstorm.bolts;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;

/**
 * @author burgetr
 *
 */
public class TestBolt implements IRichBolt
{
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(TestBolt.class);
    private OutputCollector collector;
    

    @SuppressWarnings("rawtypes")
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector)
    {
        this.collector = collector;
    }

    public void execute(Tuple input)
    {
        String s = input.getString(0);
        collector.ack(input);
        log.info("Processed: " + s);
    }

    public void cleanup()
    {
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer)
    {
    }

    public Map<String, Object> getComponentConfiguration()
    {
        return null;
    }

}
