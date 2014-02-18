/**
 * FindLineOperator.java
 *
 * Created on 24. 10. 2013, 9:56:16 by burgetr
 */
package org.burgetr.segm.areas;

import org.burgetr.segm.AreaNode;
import org.burgetr.segm.AreaTree;
import org.burgetr.segm.Rectangular;

/**
 * Detects the basic lines in the area tree and joins the appropriate areas so that a line
 * is the smallest visual area. 
 * @author burgetr
 */
public class FindLineOperator implements AreaTreeOperator
{
    /** Should the lines have a consistent visual style? */
    protected boolean useConsistentStyle;
    
    /** The maximal distance of two areas allowed within a single line (in 'em' units) */
    protected float maxLineEmSpace;
    
    
    public FindLineOperator(boolean useConsistentStyle, float maxLineEmSpace)
    {
        this.useConsistentStyle = useConsistentStyle;
        this.maxLineEmSpace = maxLineEmSpace;
    }
    
    //==============================================================================

    @Override
    public void apply(AreaTree atree)
    {
        recursiveJoinAreas(atree.getRoot());
    }

    @Override
    public void apply(AreaTree atree, AreaNode root)
    {
        recursiveJoinAreas(root);
    }
    
    //==============================================================================
    
    
    /**
     * Goes through all the areas in the tree and tries to join their sub-areas into single
     * areas.
     */
    protected void recursiveJoinAreas(AreaNode root)
    {
        joinAreas(root);
        for (int i = 0; i < root.getChildCount(); i++)
            recursiveJoinAreas(root.getChildArea(i));
    }
    
    /**
     * Goes through the grid of areas and joins the adjacent visual areas that are not
     * separated by anything
     */
    protected void joinAreas(AreaNode a)
    {
        //TODO: detekce radku by asi mela brat v uvahu separatory
        if (a.getGrid() == null) //a gird is necessary for this
            a.createGrid();
        
        boolean change = true;
        while (change)
        {
            change = false;
            for (int i = 0; i < a.getChildCount(); i++)
            {
                AreaNode node = a.getChildArea(i);
                int ny1 = node.getGridPosition().getY1();
                int nx2 = node.getGridPosition().getX2();
                int ny2 = node.getGridPosition().getY2();
                
                //try to expand to the right - find a neighbor
                AreaNode neigh = null;
                int dist = 1;
                while (neigh == null && nx2 + dist < a.getGrid().getWidth())
                {
                    //try to find some node at the right in the given distance
                    for (int y = ny1; neigh == null && y <= ny2; y++)
                    {
                        neigh = a.getGrid().getNodeAt(nx2 + dist, y);
                        if (neigh != null) //something found
                        {
                            if (useConsistentStyle || node.hasSameStyle(neigh))
                            {
                                if (horizontalJoin(a, node, neigh, true)) //try to join
                                {
                                    node.createGrid();
                                    change = true;
                                }
                            }
                            else
                            {
                                if (horizontalJoin(a, node, neigh, false)) //check if the nodes could be joined
                                {
                                    node.setNextOnLine(neigh);
                                    neigh.setPreviousOnLine(node);
                                }
                            }
                        }
                    }
                    dist++;
                }
                if (change) break; //something changed, repeat
            }
        }
    }

    /**
     * Joins two boxes horizontally into one area if the node heights are equal or they 
     * can be aligned to a rectangle using free spaces.
     * @param n1 left node to be aligned
     * @param n2 right node to be aligned
     * @param affect when set to <code>true</code>, the two nodes are joined and n2 is removed from the tree.
     *        When set to <code>false</code>, no changes are performed (only checking)
     * @return <code>true</code> when succeeded
     */
    private boolean horizontalJoin(AreaNode parent, AreaNode n1, AreaNode n2, boolean affect)
    {
        //System.out.println("HJoin: " + n1.toString() + " + " + n2.toString());
        //check the maximal distance between the nodes
        int dist = Math.min(Math.abs(n2.getX() - n1.getX2()), Math.abs(n1.getX() - n2.getX2()));
        if (dist > n1.getArea().getAverageFontSize() * maxLineEmSpace)
            return false;
        //check if there is no separating border or background
        if (n1.getArea().hasRightBorder() || 
            n2.getArea().hasLeftBorder() ||
            !n1.getArea().hasSameBackground(n2.getArea()))
            return false; //separated, give up
        //align the start
        int sy1 = n1.getGridPosition().getY1();
        int sy2 = n2.getGridPosition().getY1();
        while (sy1 != sy2)
        {
            if (sy1 < sy2) //n1 starts earlier, try to expand n2 up
            {
                if (sy2 > 0 && canExpandY(parent, n2, sy2-1, n1))
                    sy2--;
                else
                    return false; //cannot align - give up
            }
            else if (sy1 > sy2) //n2 starts earlier, try to expand n1 up
            {
                if (sy1 > 0 && canExpandY(parent, n1, sy1-1, n2))
                    sy1--;
                else
                    return false; //cannot align - give up
            }
        }
        //System.out.println("sy1="+sy1);
        //align the end
        int ey1 = n1.getGridPosition().getY2(); //last
        int ey2 = n2.getGridPosition().getY2();
        while (ey1 != ey2)
        {
            if (ey1 < ey2) //n1 ends earlier, try to expand n1 down
            {
                if (ey1 < parent.getGrid().getWidth()-1 && canExpandY(parent, n1, ey1+1, n2))
                    ey1++;
                else
                    return false; //cannot align - give up
            }
            else if (ey1 > ey2) //n2 ends earlier, try to expand n2 down
            {
                if (ey2 < parent.getGrid().getWidth()-1 && canExpandY(parent, n2, ey2+1, n1))
                    ey2++;
                else
                    return false; //cannot align - give up
            }
        }
        //System.out.println("ey1="+ey1);
        //align succeeded, join the areas
        if (affect)
        {
            Rectangular newpos = new Rectangular(n1.getGridPosition().getX1(), sy1,
                                                 n2.getGridPosition().getX2(), ey1);
            n1.joinArea(n2, newpos, true);
            parent.remove(n2);
        }
        return true;
    }
    
    
    /**
     * Checks if the area can be vertically expanded to the given 
     * Y coordinate, i.e. there is a free space in the space on this Y coordinate
     * for the whole width of the area.
     * @param node the area node that should be expanded
     * @param y the Y coordinate to that the area should be expanded
     * @param except an area that shouldn't be considered for conflicts (e.g. an overlaping area)
     * @return <code>true</code> if the area can be expanded
     */
    private boolean canExpandY(AreaNode parent, AreaNode node, int y, AreaNode except)
    {
        for (int x = node.getGridX(); x < node.getGridX() + node.getGridWidth(); x++)
        {
            AreaNode cand = parent.getGrid().getNodeAt(x, y);
            if (cand != null && cand != except)
                return false; //something found - cannot expand
        }
        return true;
    }

}
