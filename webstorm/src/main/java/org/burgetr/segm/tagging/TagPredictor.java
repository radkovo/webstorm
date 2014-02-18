/**
 * TagPredictor.java
 *
 * Created on 15.10.2012, 11:23:02 by burgetr
 */
package org.burgetr.segm.tagging;

import java.util.HashMap;
import java.util.Map;

import org.burgetr.segm.AreaNode;
import org.burgetr.segm.Config;
import org.burgetr.segm.LogicalNode;
import org.burgetr.segm.NodeJoinAnalyzer;

/**
 * This class represents a predictor that takes information from different sources (e.g. NER classification, style classification)
 * and produces the final probabilities of the tags for the given area.
 * 
 * @author burgetr
 */
public class TagPredictor implements NodeJoinAnalyzer
{
    /** The used tag style analyzer */
    private TagStyleAnalyzer sa;
    
    /**
     * Creates a new predictor using a style analyzer.
     * @param sa Rhe style analyzer to be used.
     */
    public TagPredictor(TagStyleAnalyzer sa)
    {
        this.sa = sa;
    }
    

    public Map<Tag, Double> getTagProbabilities(AreaNode node)
    {
        Map<Tag, Double> src = sa.classifyNode(node);
        if (src != null)
        {
            Map<Tag, Double> ret = new HashMap<Tag, Double>(src);
            for (Tag tag : ret.keySet())
            {
                double tp = tag.getSource().getRelevance();
                Double sp = ret.get(tag); 
                if (sp == null) sp = 0.0;
                ret.put(tag, compoundProbability(node.isLeaf(), node.hasTag(tag), tp, sp));
            }
            return ret;
        }
        else
            return null; 
    }
    
    /**
     * Computes the probability that the given area looks like a given tag.
     * @param tag the tag to be considered
     * @return the probability from 0.0 to 1.0
     */
    public double getTagProbability(AreaNode node, Tag tag)
    {
        double tp = tag.getSource().getRelevance();
        if (sa != null)
        {
            double sp = sa.getTagProbability(node, tag); 
            return compoundProbability(node.isLeaf(), node.hasTag(tag), tp, sp);
        }
        else
            return tp;
    }
    
    /**
     * Computes the probability that the given logical node looks like a given tag.
     * The probability is computed as the maximal probability of all the contained visual areas.
     * @param tag the tag to be considered
     * @return the probability from 0.0 to 1.0
     */
    public double getTagProbability(LogicalNode node, Tag tag)
    {
        double max = 0.0;
        for (AreaNode area : node.getAreaNodes())
        {
            double p = getTagProbability(area, tag);
            if (p > max) max = p;
        }
        return max;
    }
    
    /**
     * Obtains the most probable tag for the given area.
     * @param area the area to be considered
     * @return the tag
     */
    public Tag getMostProbableTag(AreaNode area)
    {
        double max = 0.0;
        Tag ret = null;
        Map<Tag, Double> probs = getTagProbabilities(area);
        for (Map.Entry<Tag, Double> entry : probs.entrySet())
        {
            if (entry.getValue() > max)
            {
                max = entry.getValue();
                ret = entry.getKey();
            }
        }
        return ret;
    }
    
    //===========================================================================================
    
    /**
     * Computes the overall probability that the node has a given tag
     * @param isLeaf is this a leaf node?
     * @param tagPresent does the node have the given tag?
     * @param tp the tag assignment confidence (0..1)
     * @param sp the tag probability obtained from style classification (0..1)
     * @return the overall probability (0..1)
     */
    private double compoundProbability(boolean isLeaf, boolean tagPresent, double tp, double sp)
    {
        if (isLeaf)
        {
            //TODO pouze nastrel
            double p1 = tp / 2.0;
            double p2 = tagPresent ? 0.5 + p1 : 0.5 - p1;
            return (p2 + sp) / 2.0;
        }
        else
            return 0.0;
    }
    
    //===========================================================================================
    
    
    /**
     * Checks if two logical nodes are joinable. For this, the must
     * <ul>
     * <li>Made of neighboring area nodes (nothing between them)
     * <li>Have the same style
     * <li>Have the same tags or the second one may be a continuation of the first one
     * <li>None of the tags of the second node may refuse joining
     * </ul> 
     * @param l1 the first logical node
     * @param l2 the second logical node
     * @return <code>true</code> if the nodes may be joined
     */
    public boolean isJoinable(LogicalNode l1, LogicalNode l2)
    {
        final double IMPORTANT_TAG_THRESHOLD = Config.TAG_PROBABILITY_THRESHOLD;
        
        AreaNode a1 = l1.getLastAreaNode();
        AreaNode a2 = l2.getFirstAreaNode();
        
        Map<Tag, Double> map1 = getTagProbabilities(a1); 
        Map<Tag, Double> map2 = getTagProbabilities(a2);
        
        if ((a1 != null && a2 != null && a1.getNextSibling() == a2) &&  //must be adjacent areas
                l1.getFirstAreaNode().hasSameStyle(l2.getFirstAreaNode())) //require the same style
        {
            //check if any important tag in a2 refuses joining
            for (Map.Entry<Tag, Double> entry : map2.entrySet()) 
                if (entry.getValue() > IMPORTANT_TAG_THRESHOLD && !entry.getKey().allowsJoining())
                    return false;
            
            //check for shared important tags
            for (Map.Entry<Tag, Double> entry : map1.entrySet())
            {
                if (entry.getValue() > IMPORTANT_TAG_THRESHOLD
                        && entry.getKey().allowsJoining()
                        && map2.get(entry.getKey()) > IMPORTANT_TAG_THRESHOLD)
                    return true;
            }
        }
        return false;
    }    
    
}
