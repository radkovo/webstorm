/**
 * RDFStorageBolt.java
 *
 * Created on 14. 2. 2014, 14:54:59 by burgetr
 */
package org.fit.burgetr.webstorm.bolts;

import java.util.Map;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.tdb.TDBFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;

/**
 * 
 * @author burgetr
 */
public class RDFStorageBolt implements IRichBolt
{
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(RDFStorageBolt.class);
    
    private String storageDir;
    private OutputCollector collector;
    private Model model; 
    private Dataset dataset;
    
    public RDFStorageBolt(String storageDir)
    {
        this.storageDir = storageDir;
    }

    @SuppressWarnings("rawtypes")
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector)
    {
        this.collector = collector;
        
        dataset = TDBFactory.createDataset(storageDir);
        dataset.begin(ReadWrite.WRITE);
        model = dataset.getDefaultModel();
    }

    public void execute(Tuple input)
    {
        Resource subj = model.createResource(input.getString(0));
        Property pred = model.createProperty(input.getString(1));
        RDFNode obj;
        String os = input.getString(2);
        if (os.startsWith("\""))
        {
            obj = model.createLiteral(os, false);
        }
        else
        {
            obj = model.createResource(os);
        }
        
        Statement stmt = model.createStatement(subj, pred, obj);
        model.add(stmt);
        
        collector.ack(input);
        log.info("Stored: " + stmt);
    }

    public void cleanup()
    {
        dataset.end();
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer)
    {
    }

    public Map<String, Object> getComponentConfiguration()
    {
        return null;
    }

}
