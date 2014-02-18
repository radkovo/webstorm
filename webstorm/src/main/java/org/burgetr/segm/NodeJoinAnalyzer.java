/**
 * NodeJoinAnalyzer.java
 *
 * Created on 19.10.2012, 11:48:17 by burgetr
 */
package org.burgetr.segm;


/**
 * An interface of an analyzer that is able to decide whether two logical nodes may be joined to a single one.
 * @author burgetr
 */
public interface NodeJoinAnalyzer
{

    /**
     * Checks if two logical nodes are joinable. The particular implementations depends on the particular analyzer.
     * 
     * @param l1 the first logical node
     * @param l2 the second logical node
     * @return <code>true</code> if the nodes may be joined
     */
    public boolean isJoinable(LogicalNode l1, LogicalNode l2);

    
}
