/**
 * LogicalLocalGroupingOperator.java
 *
 * Created on 28. 11. 2013, 11:36:41 by burgetr
 */
package org.burgetr.segm.areas;

import java.util.Vector;

import org.burgetr.segm.AreaNode;
import org.burgetr.segm.AreaNode.LayoutType;
import org.burgetr.segm.AreaTree;
import org.burgetr.segm.LogicalNode;
import org.burgetr.segm.LogicalTree;
import org.burgetr.segm.Rectangular;

/**
 * 
 * @author burgetr
 */
public class LogicalLocalGroupingOperator implements AreaTreeOperator
{
    protected LogicalTree ltree;
    protected int garbageLimit;
    protected boolean changed;

    public LogicalLocalGroupingOperator(LogicalTree ltree, int garbageLimit)
    {
        this.ltree = ltree;
        this.garbageLimit = garbageLimit;
        this.changed = false;
    }
    
    //==============================================================================

    @Override
    public void apply(AreaTree atree)
    {
        apply(atree, atree.getRoot());
    }

    @Override
    public void apply(AreaTree atree, AreaNode root)
    {
        changed = false;
        recursiveProcessAreas(root);
    }
    
    public boolean madeChanges()
    {
        return changed;
    }
    
    //==============================================================================

    protected void recursiveProcessAreas(AreaNode aroot)
    {
        for (int i = 0; i < aroot.getChildCount(); i++)
            recursiveProcessAreas(aroot.getChildArea(i));
        changed |= findLogicalSubareas(aroot);
    }
    
    protected boolean findLogicalSubareas(AreaNode aroot)
    {
        boolean changed = false;
        if (aroot.getChildCount() != 0 && aroot.getLayoutType() == LayoutType.NORMAL) //do not process leaf areas
        {
            //find the corresponding logical subtree
            LogicalNode lroot = ltree.findArea(aroot);
            if (lroot != null && lroot.getChildCount() > 0)
            {
                //find the top logical areas
                for (int i = 0; i < lroot.getChildCount(); i++)
                    changed |= recursiveCreateLogicalAreas(lroot.getChildNode(i), i, 0, 0);
            }
        }
        return changed;
    }
    
    /**
     * Goes through the logical tree and creates corresponding super-areas in the area tree. 
     * @param lroot logical (sub)tree root
     * @param childIndex the index of the root node within its parent
     * @param level current recursion level (increased with each recursion level that makes some changes in the area tree)
     * @param maxlevel maximal recursion level that stops the recursion
     * @return <code>true</code> when some changes have been made in the area subtree
     */
    protected boolean recursiveCreateLogicalAreas(LogicalNode lroot, int childIndex, int level, int maxlevel)
    {
        boolean changed = false;
        int newlevel = level;
        if (lroot.getChildCount() > 1)
        {
            int sub = getBlockSubareaSize(lroot);
            if (sub > 1 /*&& sub > lroot.getChildCount() / 2*/) //at least a half?
            {
                //else return;
                AreaNode aroot = lroot.getFirstAreaNode().getParentArea(); //the area where the new group will be created
                if (aroot != null)
                {
                    Rectangular bounds = null;
                    Vector<AreaNode> region = new Vector<AreaNode>();
                    //find all the child areas that belong to the logical group
                    //findChildAreas(lroot, aroot, region, false);
                    for (int i = 0; i < sub; i++)
                        findChildAreas(lroot.getChildNode(i), aroot, region, true);
                    //determine the grid bounds of the new group
                    for (AreaNode area : region)
                    {
                        //System.out.println("  contains" + area);
                        if (bounds == null)
                            bounds = new Rectangular(area.getGridPosition());
                        else
                            bounds.expandToEnclose(area.getGridPosition());
                    }
                    //System.out.println("  TOTAL " + bounds);
                    //create the super-area
                    AreaNode grp = aroot.createSuperArea(bounds, region, "<areaL>");
                    if (grp != null)
                    {
                        //grp.setFixedMarkedness(lroot.getFirstAreaNode().getMarkedness());
                        
                        //modify the logical subtree accordingly
                        if (lroot.getParentNode() != null)
                        {
                            LogicalNode newroot = new LogicalNode(grp);
                            lroot.addNode(newroot);
                            while (lroot.getChildNode(0) != newroot)
                                newroot.addNode(lroot.getChildNode(0)); //FIXME pridavaji se sem i pocatecni uzly, ktere maji zustat u rodice
                        }
                        
                        changed = true;
                        newlevel++;
                    }
                }
            }
        }
        /*if (newlevel <= maxlevel)
        {
            for (int i = 0; i < lroot.getChildCount(); i++)
                changed |= recursiveCreateLogicalAreas(lroot.getChildNode(i), i, newlevel, maxlevel);
        }*/
        return changed;
    }

    /**
     * Finds all the child areas of the given parent area in a logical subtree and puts them to a collection.
     * @param lroot The root of the logical subtree to search in.
     * @param aparent The parent areas whose children we are looking for.
     * @param ret The collection to put the areas in.
     * @param recursive include the child nodes of lroot recursively
     */
    protected void findChildAreas(LogicalNode lroot, AreaNode aparent, Vector<AreaNode> ret, boolean recursive)
    {
        for (AreaNode area : lroot.getAreaNodes())
        {
            if (area.getParent() == aparent)
                ret.add(area);
        }
        if (recursive)
        {
            for (int i = 0; i < lroot.getChildCount(); i++)
                findChildAreas(lroot.getChildNode(i), aparent, ret, true);
        }
    }

    /**
     * Check if the logical subtree represents a block area. In a logical area, all the child boxes
     * must be placed below each other (no Y overlapping).  
     * @param root the root of the subtree
     * @return the number of leading sub-nodes that are placed below each other
     */
    protected int getBlockSubareaSize(LogicalNode root)
    {
        if (root.getChildCount() > 0)
        {
            int cnt = 1;
            int goodCnt = 1;
            int garbage = 0;
            LogicalNode prev = root.getChildNode(0);
            for (int i = 1; i < root.getChildCount(); i++)
            {
                LogicalNode cur = root.getChildNode(i);
                cnt++;
                if (prev.getLastAreaNode().getArea().getY2() <= cur.getFirstAreaNode().getArea().getY1())
                {
                    goodCnt = cnt;
                    garbage = 0;
                }
                else
                {
                    garbage++;
                    if (garbage > garbageLimit)
                    {
                        return goodCnt; //don't count the previous one
                    }
                }
                prev = cur;
            }
            return cnt;
        }
        else
            return 0;
    }

    
}
