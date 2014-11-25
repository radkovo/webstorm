package org.fit.burgetr.webstorm.bolts;

import java.sql.SQLException;
import java.util.Map;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.vutbr.fit.monitoring.Monitoring;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.imageio.ImageIO;

import net.semanticmetadata.lire.imageanalysis.*;

/**
 * A bolt that extracts features from image
 * Accepts: (name, image_url, image_data, tuple_uuid)
 * Emits: (name,feature,image_data,tuple_uuid,image_url)
 * 
 * @author ikouril
 */
public class ExtractFeaturesBolt implements IRichBolt {
	
	private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ExtractFeaturesBolt.class);
    private OutputCollector collector;
    public static final int MAX_IMAGE_DIMENSION = 1024;
    private String webstormId;
    private Monitoring monitor;
    private String hostname;
	
    
    /**
     * Creates a new ExtractFeaturesBolt.
     * @param uuid the identifier of actual deployment
     * @throws SQLException 
     * @throws UnknownHostException 
     */
	public ExtractFeaturesBolt(String uuid) throws SQLException{
		webstormId=uuid;
		monitor=new Monitoring(webstormId,"knot28.fit.vutbr.cz","webstorm","webstormdb88pass","webstorm");
	}

    @SuppressWarnings("rawtypes")
    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector)
    {
        this.collector = collector;
        try{
			hostname=InetAddress.getLocalHost().getHostName();
		}
		catch(UnknownHostException e){
			hostname="-unknown-";
		}
    }

    @Override
    public void execute(Tuple input)
    {
    	long startTime = System.nanoTime();
    	
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
    	try {
    		Long estimatedTime = System.nanoTime() - startTime;
			monitor.MonitorTuple("ExtractFeaturesBolt", uuid,1, hostname, estimatedTime);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
