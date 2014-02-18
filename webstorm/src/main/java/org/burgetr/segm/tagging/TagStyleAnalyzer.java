/**
 * StyleAnalyzer.java
 *
 * Created on 11.10.2012, 15:57:48 by burgetr
 */
package org.burgetr.segm.tagging;

import java.util.Map;

import org.burgetr.segm.AreaNode;

/**
 * A generic analyzer that is able to assign tags according to the node style.
 * @author burgetr
 */
public interface TagStyleAnalyzer
{

    /**
     * Computes the probabilities of all the possible tags for the given area node.
     * @param area the area to be considered
     * @return A map of tags and their probabilities.
     */
    public Map<Tag, Double> classifyNode(AreaNode area);
    
    /**
     * Computes the probability that the given area looks like a given tag.
     * @param area the area to be considered
     * @param tag the tag to be considered
     * @return the probability from 0.0 to 1.0
     */
    public double getTagProbability(AreaNode area, Tag tag);

}
