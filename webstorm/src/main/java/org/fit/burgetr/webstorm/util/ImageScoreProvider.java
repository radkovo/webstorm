package org.fit.burgetr.webstorm.util;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queries.CustomScoreProvider;

public class ImageScoreProvider extends CustomScoreProvider{
	public ImageScoreProvider(AtomicReaderContext context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public float customScore(int doc, float subQueryScore, float valSrcScores[]) throws IOException       
	{
	    IndexReader r = context.reader();
	    int length=0;
	    Document d=r.document(doc);
		IndexableField lengthField=d.getField("length");
		if (lengthField !=null)
			length=lengthField.numericValue().intValue();
	    
		float score=0.0F;
		
		for (int i=0;i<length;i++){
			String fieldName=String.valueOf(i);
			IndexableField field=d.getField(fieldName);
			score+=(length-i)*field.numericValue().floatValue();
		}
	    
	    return -score;
	}
}
