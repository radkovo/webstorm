package org.fit.burgetr.webstorm.bolts;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import net.semanticmetadata.lire.DocumentBuilder;
import net.semanticmetadata.lire.ImageSearchHits;
import net.semanticmetadata.lire.ImageSearcher;
import net.semanticmetadata.lire.ImageSearcherFactory;
import net.semanticmetadata.lire.utils.LuceneUtils;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.search.*;
import org.fit.burgetr.webstorm.util.Monitoring;
import org.joda.time.DateTime;
import org.joda.time.Minutes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;

import org.fit.burgetr.webstorm.util.*;

/**
 * A bolt that indexes images
 * Accepts: (name, feature,image_data,uuid,image_url)
 * 
 * @author ikouril
 */
public class IndexBolt implements IRichBolt{
	private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(IndexBolt.class);
    IndexWriterConfig conf=null;
    Directory directory=null;
    IndexWriter iw=null;
    IndexReader ir=null;
    private String webstormId;
    private Monitoring monitor;
    private String hostname;
    //private OutputCollector collector;
    private float threshold;
    private int history;
    private int maxDocuments;
    

    /**
     * Creates a new IndexBolt.
     * @param uuid the identifier of actual deployment
     * @throws SQLException 
     * @throws UnknownHostException 
     */
    public IndexBolt(String uuid) throws SQLException {
    	webstormId=uuid;
    	monitor=new Monitoring(webstormId);
    	threshold=0.7F;
    	history=60;
    	maxDocuments=100;
    }
    
    /**
     * Creates a new IndexBolt.
     * @param uuid the identifier of actual deployment
     * @param t the threshold of image similarity, when records should be updated
     * @param h the history (in minutes) of similarity values that should be kept for similarity computation
     * @param md the maximum of documents, that should be indexed
     * @throws SQLException 
     * @throws UnknownHostException 
     */
    public IndexBolt(String uuid,int t, int h, int md) throws SQLException{
    	webstormId=uuid;
    	monitor=new Monitoring(webstormId);
    	threshold=t;
    	history=h;
    	maxDocuments=md;
    }

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context,
			OutputCollector collector) 
	{

		//this.collector=collector;
		try{
			hostname=InetAddress.getLocalHost().getHostName();
		}
		catch(UnknownHostException e){
			hostname="-unknown-";
		}
		
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
			try {
				iw.commit();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
	}

	@SuppressWarnings("deprecation")
	@Override
	public void execute(Tuple input)
	{
		long startTime = System.nanoTime();
		
		String name = input.getString(0);
        byte[] feature = input.getBinary(1);
        String image_url=input.getString(4);
        String uuid=input.getString(3);
        
        byte[] imageData=input.getBinary(2);
        BufferedImage image=null;
		try {
			image = ImageIO.read(new ByteArrayInputStream(imageData));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        DateTime now = DateTime.now();
        String dateString=String.valueOf(now.getYear())+"-"+String.valueOf(now.getMonthOfYear())+"-"+String.valueOf(now.getDayOfMonth())+"-"+String.valueOf(now.getHourOfDay())+"-"+String.valueOf(now.getMinuteOfHour())+"-"+String.valueOf(now.getSecondOfMinute())+"-"+String.valueOf(now.getMillisOfSecond());
        log.info("DateTime:"+dateString+", Indexing image from url: " + image_url+" (originating from document with uuid: "+uuid+")");

        try {
			ir = IndexReader.open(directory);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        //compare threshold with 10 best matches
        ImageSearcher searcher = ImageSearcherFactory.createCEDDImageSearcher(10);
        ImageSearchHits hits=null;
        try {
			hits = searcher.search(image, ir);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();

		}
        
        float totalScore=0.0F;
        int overThreshold=0;
        
        for (int i = 0; i < hits.length(); i++) {
            
            if (hits.score(i)>threshold){
            	String fileName = hits.doc(i).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0];
                log.info("Document similarity with "+fileName+" is "+hits.score(i));
                
                float actualScore=hits.score(i);
                overThreshold+=1;

            	
            	totalScore+=actualScore;
                IndexableField f=hits.doc(i).getField("length");
                int length=0;
                if (f!=null){
                	length=f.numericValue().intValue();
                }
                String lengthString=String.valueOf(length);
                hits.doc(i).add(new FloatField(lengthString,hits.score(i), FloatField.TYPE_STORED));
                hits.doc(i).add(new Field("t"+lengthString,dateString,Field.Store.YES, Field.Index.NOT_ANALYZED));
                length++;
                hits.doc(i).removeField("length");
				hits.doc(i).add(new IntField("length", length, IntField.TYPE_STORED));
				
				
				//field from where
				IndexableField s=hits.doc(i).getField("start");
				int start=0;
                if (s!=null){
                	start=s.numericValue().intValue();
                }
                
                boolean cont=true;
                int index=start;
                while (cont){
                	String startVal=String.valueOf(index);
                	Field val=(Field) hits.doc(i).getField("t"+startVal);
                	String fieldTimestamp=val.stringValue();
                	if (!meetRequirements(fieldTimestamp,now)){
                		hits.doc(i).removeField(startVal);
                		hits.doc(i).removeField("t"+startVal);
                		index++;
                	}
                	else{
                		cont=false;
                		
                		
                	}
                }
                hits.doc(i).removeField("start");
                hits.doc(i).add(new IntField("start", index, IntField.TYPE_STORED));
                
                
            	String myid=hits.doc(i).getValues("myid")[0];
            	Term t=new Term("myid",myid);
            	
            	try {
					iw.updateDocument(t, hits.doc(i));
					iw.commit();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

            }
        }
        
        
        
        Document document = new Document();
        document.add(new Field(DocumentBuilder.FIELD_NAME_CEDD, feature));
        document.add(new Field(DocumentBuilder.FIELD_NAME_IDENTIFIER, name, Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field("image", imageData));
        document.add(new Field("myid",UUID.randomUUID().toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        
        
        if (overThreshold>0){
        	float averageScore=totalScore/overThreshold;
        	document.add(new IntField("length", 1, IntField.TYPE_STORED));
        	document.add(new FloatField("0",averageScore, FloatField.TYPE_STORED));
        	document.add(new Field("t0",dateString, Field.Store.YES, Field.Index.NOT_ANALYZED));
        }
        
        
        
        try {
			iw.addDocument(document);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        try {
        	ir.close();
			iw.commit();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

        try {
			ir = IndexReader.open(directory);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
            
            
        int total=ir.numDocs();
        if (total>maxDocuments){
        	int documentsToDelete=total-maxDocuments;
        	
        	Query q=new ImagesToDeleteQuery(new MatchAllDocsQuery(),history);
        	IndexSearcher s = new IndexSearcher(ir);
            TopScoreDocCollector collector = TopScoreDocCollector.create(documentsToDelete, true);
            try {
				s.search(q, collector);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            ScoreDoc[] h = collector.topDocs().scoreDocs;
            
            for (ScoreDoc toDelete:h){
            	IndexableField f=null;
            	try {
					f=ir.document(toDelete.doc).getField("myid");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	String myid=f.stringValue();
            	Term t=new Term("myid",myid);
            	try {
					iw.deleteDocuments(t);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	log.info("Deleting document: "+myid+" with score: "+getScore(myid));
            	
            }
            try {
				iw.commit();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        }
        dumpAllScores();  

        try {
			ir.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

        try {
        	Long estimatedTime = System.nanoTime() - startTime;
			monitor.MonitorTuple("IndexBolt", uuid, hostname, estimatedTime);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private boolean meetRequirements(String fieldTimestamp, DateTime now) {
		String[] values1=fieldTimestamp.split("-");
		DateTime from=new DateTime(Integer.parseInt(values1[0]),Integer.parseInt(values1[1]),Integer.parseInt(values1[2]),Integer.parseInt(values1[3]),Integer.parseInt(values1[4]),Integer.parseInt(values1[5]),Integer.parseInt(values1[6]));

		Minutes diff=Minutes.minutesBetween(from, now);
		if (diff.getMinutes()<=history)
			return true;
		return false;
		
	}

	/**
     * Prints scores of all documents
     */
	private void dumpAllScores(){
		for (int i=0; i<ir.maxDoc(); i++) {
			
			log.info("Document score: "+getScore(i));
		}
	}
	
	/**
     * Computes score of given document
     * @param docId - integer number of document
     * @return Score of document 
     */
	private float getScore(int docNumber){
		Document d = null;
		try {
			d = ir.document(docNumber);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (d!=null){
			IndexableField lengthField=d.getField("length");
			int length=0;
			if (lengthField !=null)
				length=lengthField.numericValue().intValue();
			
			int start=0;
			IndexableField startField=d.getField("start");
			if (startField !=null)
				start=startField.numericValue().intValue();
		    
			float score=0.0F;
			DateTime now=DateTime.now();
			for (int i=start;i<length;i++){
				String fieldName=String.valueOf(i);
				IndexableField field=d.getField(fieldName);
				Field timeStampField=(Field) d.getField("t"+fieldName);
				String[] values1=timeStampField.stringValue().split("-");
				DateTime from=new DateTime(Integer.parseInt(values1[0]),Integer.parseInt(values1[1]),Integer.parseInt(values1[2]),Integer.parseInt(values1[3]),Integer.parseInt(values1[4]),Integer.parseInt(values1[5]),Integer.parseInt(values1[6]));
				int diff=Minutes.minutesBetween(from, now).getMinutes();
				int scoreCoef=history-diff;
				if (scoreCoef>0)
					score+=scoreCoef*field.numericValue().floatValue();
				
			}
			int numRecords=length-start;
			if (numRecords>0)
				return score/numRecords;
		    
		    return score;
		}
			
		
		return -1.0F;
	}
	
	/**
     * Computes score of given document
     * @param docId - String id of indexed document
     * @return Score of document 
     */
	private float getScore(String docId){
		
		IndexSearcher is=new IndexSearcher(ir);
		TermQuery q=new TermQuery(new Term("myid",docId));
		TopDocs sc = null;
		try {
			sc = is.search(q, 1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (sc==null || sc.totalHits==0){
			return -1.0F;
		}
		return getScore(sc.scoreDocs[0].doc);
		
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
