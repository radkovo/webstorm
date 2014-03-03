/**
 * DownloaderBolt.java
 *
 * Created on 3. 3. 2014, 13:39:59 by burgetr
 */
package org.fit.burgetr.webstorm.bolts;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Map;

import org.fit.cssbox.css.CSSNorm;
import org.fit.cssbox.css.DOMAnalyzer;
import org.fit.cssbox.css.NormalOutput;
import org.fit.cssbox.css.Output;
import org.fit.cssbox.io.DOMSource;
import org.fit.cssbox.io.DefaultDOMSource;
import org.fit.cssbox.io.DefaultDocumentSource;
import org.fit.cssbox.io.DocumentSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;

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
        
        log.info("Downloading url: " + urlstring);
        
        try
        {
            //Open the network connection 
            DocumentSource docSource = new DefaultDocumentSource(urlstring);
            
            //Parse the input document
            DOMSource parser = new DefaultDOMSource(docSource);
            Document doc = parser.parse();
            
            //Create the CSS analyzer
            DOMAnalyzer da = new DOMAnalyzer(doc, docSource.getURL());
            da.attributesToStyles(); //convert the HTML presentation attributes to inline styles
            da.addStyleSheet(null, CSSNorm.stdStyleSheet(), DOMAnalyzer.Origin.AGENT); //use the standard style sheet
            da.addStyleSheet(null, CSSNorm.userStyleSheet(), DOMAnalyzer.Origin.AGENT); //use the additional style sheet
            da.getStyleSheets(); //load the author style sheets
            
            //Compute the styles
            log.debug("Computing style...");
            da.stylesToDomInherited();
            
            //Save the output
            PrintStream os = new PrintStream(new FileOutputStream("??")); //TODO string writer?
            Output out = new NormalOutput(doc);
            out.dumpTo(os);
            os.close();
            
            docSource.close();
            
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
