/**
 * GenericAreaTree.java
 *
 * Created on 13.5.2011, 15:17:08 by burgetr
 */
package org.burgetr.segm;

/**
 * A basic tree of areas with the possibility of searching the areas.
 * 
 * @author burgetr
 */
public interface GenericAreaTree
{
    
    /**
     * Finds the bottom-most area node present at the given coordinates in the area tree.
     * @param x the X coordinate
     * @param y the Y coordinate
     * @return the area node present at the given coordinates or <code>null</code> when no node is found.
     */
    abstract public AreaNode getAreaAt(int x, int y);
    
    /**
     * Finds an area by the given name.
     * @param name the name of the area
     * @return the area with the given name or <code>null</code> when nothing is found.
     */
    abstract public AreaNode getAreaByName(String name);
    
}
