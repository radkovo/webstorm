package org.fit.burgetr.webstorm.util;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.queries.CustomScoreProvider;
import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.search.Query;

public class ImagesToDeleteQuery extends CustomScoreQuery {

    public ImagesToDeleteQuery(Query subQuery) {
		super(subQuery);
		// TODO Auto-generated constructor stub
	}

    protected CustomScoreProvider getCustomScoreProvider(
            AtomicReaderContext context) throws IOException {
        return new ImageScoreProvider(context);
    }
}
