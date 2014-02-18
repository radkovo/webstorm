/**
 * PersonsTagger.java
 *
 * Created on 11.11.2011, 14:20:49 by burgetr
 */
package org.burgetr.segm.tagging.taggers;

import java.util.List;
import java.util.Vector;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.util.Triple;

import org.burgetr.segm.AreaNode;
import org.burgetr.segm.tagging.Tag;
import org.burgetr.segm.tagging.TreeTagger;

/**
 * NER-based location name area tagger. It tags the areas that contain at least the specified number of location names. 
 * @author burgetr
 */
public class LocationsTagger implements Tagger
{
    private int mincnt;
    private AbstractSequenceClassifier<?> classifier;
    
    /**
     * Construct a new tagger.
     * @param mincnt the minimal count of the personal names detected in the area necessary for tagging this area.
     */
    public LocationsTagger(int mincnt)
    {
        this.mincnt = mincnt;
        classifier = TreeTagger.sharedClassifier;
    }

    public Tag getTag()
    {
        return new Tag("locations", this);
    }

    public double getRelevance()
    {
        return 0.8;
    }
    
    public boolean belongsTo(AreaNode node)
    {
        if (node.isLeaf())
        {
            String text = node.getArea().getBoxText();
            List<Triple<String,Integer,Integer>> list = classifier.classifyToCharacterOffsets(text);
            int cnt = 0;
            for (Triple<String,Integer,Integer> t : list)
            {
                if (t.first().equals("LOCATION"))
                    cnt++;
                if (cnt >= mincnt)
                    return true;
            }
        }
        return false;
    }
    
    public boolean allowsContinuation(AreaNode node)
    {
    	return false;
    }

    public boolean allowsJoining()
    {
        return true;
    }

    public boolean mayCoexistWith(Tag other)
    {
        return true;
    }
    
    public Vector<String> extract(String src)
    {
        Vector<String> ret = new Vector<String>();
        //TODO implement extraction
        ret.add(src);
        return ret;
    }
    
    //=================================================================================================
    
}
