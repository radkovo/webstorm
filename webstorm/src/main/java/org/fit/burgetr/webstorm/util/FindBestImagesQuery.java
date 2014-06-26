package org.fit.burgetr.webstorm.util;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.queries.CustomScoreProvider;
import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.search.Query;

public class FindBestImagesQuery extends CustomScoreQuery {

	private int history;
    public FindBestImagesQuery(Query subQuery,int h) {
		super(subQuery);
		history=h;
		
	}

    protected CustomScoreProvider getCustomScoreProvider(
            AtomicReaderContext context) throws IOException {
        return new BestImageScoreProvider(context,history);
    }
}
