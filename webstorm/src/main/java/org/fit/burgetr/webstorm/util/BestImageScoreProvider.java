package org.fit.burgetr.webstorm.util;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queries.CustomScoreProvider;
import org.joda.time.DateTime;
import org.joda.time.Minutes;


public class BestImageScoreProvider extends CustomScoreProvider{
	private int history;
	public BestImageScoreProvider(AtomicReaderContext context,int h) {
		super(context);
		history=h;
	}

	public float customScore(int doc, float subQueryScore, float valSrcScores[]) throws IOException       
	{
		DateTime now = DateTime.now();
		
	    IndexReader r = context.reader();
	    int length=0;
	    Document d=r.document(doc);
		IndexableField lengthField=d.getField("length");
		if (lengthField !=null)
			length=lengthField.numericValue().intValue();
		
		int start=0;
		IndexableField startField=d.getField("start");
		if (startField !=null)
			start=startField.numericValue().intValue();
	    
		float score=0.0F;
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
	    return score;
	}
}
