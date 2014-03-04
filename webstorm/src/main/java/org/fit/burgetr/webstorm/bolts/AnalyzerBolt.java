/**
 * AnalyzerBolt.java
 *
 * Created on 4. 3. 2014, 11:42:04 by burgetr
 */
package org.fit.burgetr.webstorm.bolts;

import java.net.URL;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import org.burgetr.segm.Segmentator;
import org.fit.burgetr.webstorm.util.RDFProducer;
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
        String urlstring = input.getString(0);
        Model model = processUrl(urlstring);
        if (model != null)
        {
            StmtIterator iter = model.listStatements();
            while (iter.hasNext())
            {
                Statement stmt      = iter.nextStatement();  // get next statement
                Resource  subject   = stmt.getSubject();     // get the subject
                Property  predicate = stmt.getPredicate();   // get the predicate
                RDFNode   object    = stmt.getObject();      // get the object
                String objstring;
                if (object instanceof Resource) {
                    objstring = object.toString();
                 } else {
                     // object is a literal
                     objstring = "\"" + object.toString() + "\"";
                 }
                
                collector.emit(new Values(subject.toString(), predicate.toString(), objstring));
                log.info("emit (" + subject.toString() + " : " + predicate.toString() + " : " + objstring + ")");
            } 
        }
        collector.ack(input);
        log.info("Processed: " + urlstring);
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
    
    private Model processUrl(String urlstring)
    {
        try
        {
            URL url = new URL(urlstring);
            Segmentator segm = new Segmentator();
            segm.segmentURL(url);
            
            RDFProducer rdf = new RDFProducer(segm.getBoxTree(), segm.getAreaTree(), url);
            return rdf.getModel();
        } catch (Exception e)
        {
            //e.printStackTrace();
            log.error(e.getMessage());
            return null;
        }
    }

}
