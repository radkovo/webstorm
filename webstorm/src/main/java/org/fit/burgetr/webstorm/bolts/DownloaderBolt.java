/**
 * DownloaderBolt.java
 *
 * Created on 3. 3. 2014, 13:39:59 by burgetr
 */
package org.fit.burgetr.webstorm.bolts;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.burgetr.segm.Segmentator;
import org.burgetr.segm.tagging.taggers.PersonsTagger;
import org.burgetr.segm.tagging.taggers.Tagger;
import org.fit.burgetr.webstorm.util.LogicalTagLookup;
import org.fit.cssbox.demo.StyleImport;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;


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
    
    private byte[] downloadUrl(URL toDownload) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int bytesRead;
        InputStream stream = toDownload.openStream();

        while ((bytesRead = stream.read(chunk)) > 0) {
            outputStream.write(chunk, 0, bytesRead);
        }
        return outputStream.toByteArray();
    }

    @Override
    public void execute(Tuple input)
    {
        String urlstring = input.getString(0);
        String title = input.getString(1);
        
        log.info("Downloading url: " + urlstring);
        
        try
        {
        	/*
            StyleImport si = new StyleImport(urlstring);
            StringWriter os = new StringWriter();
            si.dumpTo(new PrintWriter(os));
            os.close();
            */
        	//StringWriter os = new StringWriter();

            HashMap<String,byte[]> allImg=new HashMap<String,byte[]>();

            Document document = (Document) Jsoup.connect(urlstring).get();
            Elements images = document.select("img[src~=(?i)\\.(png|jpe?g|gif)]");

            for (Element image : images) {
                String src=image.attr("src");
                URL u = new URL(src);
                URI uri = new URI(u.getProtocol(), u.getUserInfo(), u.getHost(), u.getPort(), u.getPath(), u.getQuery(), u.getRef());
                String canonical = uri.toString();
                allImg.put(canonical, downloadUrl(u));
            }

            collector.emit(new Values(title, urlstring, document.html(), allImg));
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
        declarer.declare(new Fields("title", "base_url", "html", "images"));

    }

    @Override
    public Map<String, Object> getComponentConfiguration()
    {
        return null;
    }

}
