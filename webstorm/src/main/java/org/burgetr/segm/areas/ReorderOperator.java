/**
 * ReorderOperator.java
 *
 * Created on 7. 11. 2013, 13:30:41 by burgetr
 */
package org.burgetr.segm.areas;

import org.burgetr.segm.Area;
import org.burgetr.segm.AreaNode;
import org.burgetr.segm.AreaTree;

/**
 * Reorders the areas in the tree so that they can be read from top to bottom and from left to right.
 * Only local issues are solved, i.e. the solved boxes must overlap at least partially.
 * This fixes the irregularities caused mainly by floating boxes.
 * @author burgetr
 */
public class ReorderOperator implements AreaTreeOperator
{

    public ReorderOperator()
    {
    }
    
    //==============================================================================

    @Override
    public void apply(AreaTree atree)
    {
        recursiveReorderAreas(atree.getRoot());
    }

    @Override
    public void apply(AreaTree atree, AreaNode root)
    {
        recursiveReorderAreas(root);
    }
    
    //==============================================================================

    protected void recursiveReorderAreas(AreaNode root)
    {
        if (root.getChildCount() > 1)
        {
            boolean change;
            do
            {
                change = false;
                AreaNode prev = root.getChildArea(0);
                for (int i = 1; i < root.getChildCount() && !change; i++)
                {
                    AreaNode cur = root.getChildArea(i);
                    if (!isAllowedOrder(prev, cur))
                    {
                        root.insert(cur, i - 1); //swap previous and current
                        change = true;
                    }
                    prev = cur;
                }
            } while (change);
        }
        for (int i = 0; i < root.getChildCount(); i++)
            recursiveReorderAreas(root.getChildArea(i));
    }

    protected boolean isAllowedOrder(AreaNode an1, AreaNode an2)
    {
        Area a1 = an1.getArea();
        Area a2 = an2.getArea();
        if ((a1.getY1() <= a2.getY1() && a1.getY2() >= a2.getY1()) || //Y overlap - we solve the positions
            (a1.getY1() <= a2.getY2() && a1.getY2() >= a2.getY2()) ||
            (a2.getY1() <= a1.getY1() && a2.getY2() >= a1.getY1()) ||
            (a2.getY1() <= a1.getY2() && a2.getY2() >= a1.getY2()))
        {
            if ((a1.getX1() <= a2.getX1() && a1.getX2() >= a2.getX1()) || //X overlap
                    (a1.getX1() <= a2.getX2() && a1.getX2() >= a2.getX2()) ||
                    (a2.getX1() <= a1.getX1() && a2.getX2() >= a1.getX1()) ||
                    (a2.getX1() <= a1.getX2() && a2.getX2() >= a1.getX2()))
            {
                return true; //we don't know
            }
            else
                return (a1.getX2() <= a2.getX1());
        }
        else //no Y overlap - we don't solve the positions and allow everything
            return true;
    }
    
}
