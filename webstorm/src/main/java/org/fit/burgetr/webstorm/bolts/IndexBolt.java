package org.fit.burgetr.webstorm.bolts;

import java.io.IOException;
import java.util.Map;

import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.utils.LuceneUtils;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;

public class IndexBolt implements IRichBolt{
	private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(IndexBolt.class);
    private OutputCollector collector;
    IndexWriterConfig conf=null;
    Directory directory=null;
    IndexWriter iw=null;

	@Override
	public void prepare(Map stormConf, TopologyContext context,
			OutputCollector collector) {
		this.collector=collector;
		if (conf==null){
			conf = new IndexWriterConfig(LuceneUtils.LUCENE_VERSION,
	                new WhitespaceAnalyzer(LuceneUtils.LUCENE_VERSION));
			}
			if (directory==null)
				directory = new RAMDirectory();
			if (iw==null){
		        try {
					iw = new IndexWriter(directory, conf);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	}

	@Override
	public void execute(Tuple input) {
		
		
		String name = input.getString(0);
        byte[] feature = input.getBinary(1);
        String image_url=input.getString(4);
        String uuid=input.getString(3);
        DateTime now = DateTime.now();
        String dateString=String.valueOf(now.getYear())+"-"+String.valueOf(now.getMonthOfYear())+"-"+String.valueOf(now.getDayOfMonth())+"-"+String.valueOf(now.getHourOfDay())+"-"+String.valueOf(now.getMinuteOfHour())+"-"+String.valueOf(now.getSecondOfMinute())+"-"+String.valueOf(now.getMillisOfSecond());
        log.info("DateTime:"+dateString+", Indexing image from url: " + image_url+" (originating from document with uuid: "+uuid+")");

        
        Document document = new Document();
        document.add(new Field(DocumentBuilder.FIELD_NAME_CEDD, feature));
        document.add(new Field(DocumentBuilder.FIELD_NAME_IDENTIFIER, name, Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field("image", input.getBinary(2)));

        try {
			iw.addDocument(document);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void cleanup() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Object> getComponentConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}

}
