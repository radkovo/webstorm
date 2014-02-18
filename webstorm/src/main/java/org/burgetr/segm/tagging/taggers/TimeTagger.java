/**
 * TimeTagger.java
 *
 * Created on 11.11.2011, 11:20:20 by burgetr
 */
package org.burgetr.segm.tagging.taggers;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.burgetr.segm.AreaNode;
import org.burgetr.segm.tagging.Tag;

/**
 * This tagger tags the areas that contain some time expressions.
 * 
 * @author burgetr
 */
public class TimeTagger implements Tagger
{
    protected Pattern[] timeexpr = {Pattern.compile("[0-2]?[0-9][:\\.][0-5][0-9]([ap])?m?")};
    
    public Tag getTag()
    {
        return new Tag("time", this);
    }

    public double getRelevance()
    {
        return 0.95;
    }
    
    public boolean belongsTo(AreaNode node)
    {
        if (node.isLeaf())
        {
            String text = node.getArea().getBoxText();
            String[] words = text.split("\\s+");
            for (String s : words)
            {
                for (Pattern p : timeexpr)
                {
                    if (p.matcher(s).lookingAt()) 
                        return true;
                }
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
        return false;
    }
    
    public boolean mayCoexistWith(Tag other)
    {
        return true;
    }
    
    public Vector<String> extract(String src)
    {
        Vector<String> ret = new Vector<String>();
        
        String[] words = src.toLowerCase().split("[^0-9:\\.apm]");
        for (String s : words)
        {
            for (Pattern p : timeexpr)
            {
                Matcher match = p.matcher(s);
                if (match.lookingAt())
                {
                    ret.add(match.group());
                }
            }
        }
        
        return ret;
    }
    
    //=================================================================================================
}
