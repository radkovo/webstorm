package org.fit.burgetr.webstorm.bolts;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.search.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;

import org.fit.burgetr.webstorm.util.*;

import cz.vutbr.fit.monitoring.Monitoring;

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
    private int lastMinute;
    private int history;
    private int maxDocuments;
    private int updateInterval;
    private boolean ram;
    private int best;
    private static Integer instances=0;

    /**
     * Creates a new IndexBolt.
     * @param uuid the identifier of actual deployment
     * @throws SQLException 
     * @throws UnknownHostException 
     */
    public IndexBolt(String uuid) throws SQLException {
    	webstormId=uuid;
    	monitor=new Monitoring(webstormId,"knot28.fit.vutbr.cz","webstorm","webstormdb88pass","webstorm");
    	lastMinute=0;
    	updateInterval=1;
    	threshold=0.7F;
    	history=10;
    	maxDocuments=100;
    	ram=false;
    	best=100;
    }
    
    /**
     * Creates a new IndexBolt.
     * @param uuid the identifier of actual deployment
     * @param ui the update interval window in minutes
     * @param t the threshold of image similarity, when records should be updated
     * @param h the history of similarity values that should be kept for similarity computation
     * @param md the maximum of documents, that should be indexed
     * @param ram whether index should be stored in RAM
     * @param best how many best documents should be scored
     * @throws SQLException 
     * @throws UnknownHostException 
     */
    public IndexBolt(String uuid,int ui,int t, int h, int md,boolean ram,int best) throws SQLException{
    	webstormId=uuid;
    	monitor=new Monitoring(webstormId,"knot28.fit.vutbr.cz","webstorm","webstormdb88pass","webstorm");
    	lastMinute=0;
    	updateInterval=ui;
    	threshold=t;
    	history=h;
    	maxDocuments=md;
    	this.ram=ram;
    	this.best=best;
    }

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context,
			OutputCollector collector) 
	{
		int nodeId;
		
		synchronized(instances){
			nodeId=++instances;
		}
		
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
			if (directory==null){
				
				if (ram){
					directory = new RAMDirectory();
				}
				else{
					try {
						String path=System.getProperty("user.home")+"/index/"+String.valueOf(context.getThisTaskIndex())+"/";
						File f=new File(path);
						f.mkdirs();
						for(File file: f.listFiles()) file.delete(); // Clen directory from files of old schedules because of stucked locks
						directory=FSDirectory.open(f);
					} catch (IOException e) {
						directory=new RAMDirectory();
						e.printStackTrace();
					}
				}
			}
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
        int actualMinute=now.getMinuteOfHour();
        boolean updateWeights=(actualMinute!=lastMinute) && ((actualMinute%updateInterval)==0);
        lastMinute=actualMinute;
        
        try {
			ir = IndexReader.open(directory);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        if (updateWeights){
        	for (int i=0; i<ir.maxDoc(); i++) {

        	    Document doc = null;
				try {
					doc = ir.document(i);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				int length=0;
				IndexableField lengthField=doc.getField("length");
				if (lengthField !=null)
					length=lengthField.numericValue().intValue();
				
				String logString="";
				
				for (int counter=length;counter>0;counter--){
					String fieldName=String.valueOf(counter-1);
					/*
					for (IndexableField ifield:doc.getFields()){
						log.info("Field name: "+ifield.name());
					}
					log.info("Target field name: "+fieldName);
					*/
					IndexableField field=doc.getField(fieldName);
					
					
					logString=" "+field.numericValue().floatValue()+logString;
					doc.removeField(fieldName);
					if (counter<history)
						doc.add(new FloatField(String.valueOf(counter),field.numericValue().floatValue(),FloatField.TYPE_STORED));
				}
				doc.add(new FloatField("0",0.0F,FloatField.TYPE_STORED));
				if (length<(history-1))
					length++;
				doc.removeField("length");
				doc.add(new IntField("length", length, IntField.TYPE_STORED));
				
				
				String myid=doc.getValues("myid")[0];
            	Term t=new Term("myid",myid);
            	
            	try {
					iw.updateDocument(t, doc);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

            	log.info("Updating document "+myid+": "+logString);
				
        	}
        	try {
        		ir.close();
				iw.commit();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
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
            	IndexableField f=hits.doc(i).getField("0");
            	
            	float weight=0.0F;
            	if (f!=null){
            		weight=f.numericValue().floatValue();
            	}
            	else{
            		hits.doc(i).add(new IntField("length", 1, IntField.TYPE_STORED));
            	}
            	float actualScore=hits.score(i);
            	totalScore+=actualScore;
            	overThreshold+=1;
            	weight+=actualScore;
            	hits.doc(i).removeField("0");
            	hits.doc(i).add(new FloatField("0",weight, FloatField.TYPE_STORED));
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
        document.add(new Field("image_url",image_url,Field.Store.YES,Field.Index.NOT_ANALYZED));
        document.add(new Field("image", imageData));
        document.add(new Field("myid",UUID.randomUUID().toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        
        
        if (overThreshold>0){
        	float averageScore=totalScore/overThreshold;
        	document.add(new IntField("length", 1, IntField.TYPE_STORED));
        	document.add(new FloatField("0",averageScore, FloatField.TYPE_STORED));
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
        	
        	Query q=new ImagesToDeleteQuery(new MatchAllDocsQuery());
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
        List<Score> allScores=computeScores();  
        
        log.info("Total number of documents currently indexed: "+String.valueOf(ir.maxDoc()));
        
        for (int i=0;i<allScores.size()&&i<best;i++){
        	Document d=null;
        	try {
    			d = ir.document(allScores.get(i).getDocId());
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    		if (d!=null){
    			IndexableField nameField=d.getField(DocumentBuilder.FIELD_NAME_IDENTIFIER);
    			String docName=nameField.stringValue();
    			IndexableField imageUrlField=d.getField("image_url");
    			String imageURL=imageUrlField.stringValue();
    			log.info(String.valueOf(i+1)+". best image -> name: "+docName+", score: "+String.valueOf(allScores.get(i).getScore())+", image url:"+imageURL);
    		}
        	
        }

        try {
			ir.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

        try {
        	Long estimatedTime = System.nanoTime() - startTime;
			monitor.MonitorTuple("IndexBolt", uuid,1, hostname, estimatedTime);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
     * Prints scores of all documents
	 * @return 
     */
	private List<Score> computeScores(){
		List<Score> scores=new ArrayList<Score>();
		for (int i=0; i<ir.maxDoc(); i++) {
			float score=getScore(i);
			scores.add(new Score(score,i));
		}
		Collections.sort(scores, new Comparator<Score>(){
			@Override
			public int compare(Score o1, Score o2) {
				
				return o2.getScore().compareTo(o1.getScore());
			}
			
		});
		return scores;
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
		    
			float score=0.0F;
			
			for (int i=0;i<length;i++){
				String fieldName=String.valueOf(i);
				IndexableField field=d.getField(fieldName);
				score+=(length-i)*field.numericValue().floatValue();
			}
		    
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
