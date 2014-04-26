package org.fit.burgetr.webstorm.bolts;

import java.util.Map;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import net.semanticmetadata.lire.imageanalysis.*;

public class ExtractFeaturesBolt implements IRichBolt {
	
	private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ExtractFeaturesBolt.class);
    private OutputCollector collector;
    public static final int MAX_IMAGE_DIMENSION = 1024;
	
	    @SuppressWarnings("rawtypes")
	    @Override
	    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector)
	    {
	        this.collector = collector;
	    }

	    @Override
	    public void execute(Tuple input)
	    {
	    	String name = input.getString(0);
	        String image_url = input.getString(1);
	        byte[] image_data=input.getBinary(2);
	        String uuid=input.getString(3);
	        DateTime now = DateTime.now();
	        String dateString=String.valueOf(now.getYear())+"-"+String.valueOf(now.getMonthOfYear())+"-"+String.valueOf(now.getDayOfMonth())+"-"+String.valueOf(now.getHourOfDay())+"-"+String.valueOf(now.getMinuteOfHour())+"-"+String.valueOf(now.getSecondOfMinute())+"-"+String.valueOf(now.getMillisOfSecond());
	        log.info("DateTime:"+dateString+", Extracting features of image from url: " + image_url+" (originating from document with uuid: "+uuid+")");

	        /*
	    	URL url = null;
	    	BufferedImage image = null;
			try {
				url = new URL(image_url);
			} catch (MalformedURLException e) {
				log.error("Malformed url error: " + e.getMessage());
	            collector.fail(input);
			}
	    	 try {
				image = ImageIO.read(url);
			} catch (IOException e) {
				log.error("Fetch error: " + e.getMessage());
	            collector.fail(input);
			}
	    	*/
	        InputStream in = new ByteArrayInputStream(image_data);
	        BufferedImage image=null;
			try {
				image = ImageIO.read(in);
			} catch (IOException e) {
				log.error("Corrupted image: " + e.getMessage());
	            collector.fail(input);
			}
	    	LireFeature lireFeature = null;
			try {
				lireFeature = CEDD.class.newInstance();
			} catch (InstantiationException e) {
				log.error("Lire instantiation error: " + e.getMessage());
	            collector.fail(input);
			} catch (IllegalAccessException e) {
				log.error("Lire illegal access error: " + e.getMessage());
	            collector.fail(input);
			}
	    	lireFeature.extract(image);
	    	byte[] feature=lireFeature.getByteArrayRepresentation();
	    	collector.emit(new Values(name,feature,image_data,uuid,image_url));
            collector.ack(input);
	    	 
        }

		@Override
		public void cleanup() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			declarer.declare(new Fields("name", "feature","image_data","uuid","image_url"));
			
		}

		@Override
		public Map<String, Object> getComponentConfiguration() {
			// TODO Auto-generated method stub
			return null;
		}
}
