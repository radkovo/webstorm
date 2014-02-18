/**
 * LogicalGlobalGroupingOperator.java
 *
 * Created on 8. 11. 2013, 13:23:47 by burgetr
 */
package org.burgetr.segm.areas;

import java.util.Vector;

import org.burgetr.segm.AreaNode;
import org.burgetr.segm.AreaTree;
import org.burgetr.segm.LogicalNode;
import org.burgetr.segm.LogicalTree;
import org.burgetr.segm.Rectangular;

/**
 * Creates "logical groups" in the area tree based on the detected logical structure.
 * The logical tree is analyzed globally independently on the existing areas.
 * 
 * @author burgetr
 */
public class LogicalGlobalGroupingOperator implements AreaTreeOperator
{
    protected final int MAXLEVEL = 999; //maximal recursion depth
    
    protected LogicalTree ltree;
    protected int garbageLimit;
    protected boolean changed;

    public LogicalGlobalGroupingOperator(LogicalTree ltree, int garbageLimit)
    {
        this.ltree = ltree;
        this.garbageLimit = garbageLimit;
        this.changed = false;
    }
    
    //==============================================================================

    @Override
    public void apply(AreaTree atree)
    {
        changed = false;
        recursiveCreateLogicalAreas(ltree.getRoot(), 0, 0);
    }

    @Override
    public void apply(AreaTree atree, AreaNode root)
    {
        System.err.println("Warning: LogicalGlobalGroupingOperator: ignoring specified tree root");
        apply(atree);
    }
    
    public boolean madeChanges()
    {
        return changed;
    }
    
    //==============================================================================

    /**
     * Goes through the logical tree and creates corresponding super-areas in the area tree. 
     * @param lroot logical (sub)tree root
     * @param childIndex the index of the root node within its parent
     * @param level recursion level tracking (increased with each recursion level that makes some changes in the area tree)
     */
    protected void recursiveCreateLogicalAreas(LogicalNode lroot, int childIndex, int level)
    {
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
                    newlevel++;
                    Rectangular bounds = null;
                    Vector<AreaNode> region = new Vector<AreaNode>();
                    //find all the child areas that belong to the logical group
                    findChildAreas(lroot, aroot, region, false);
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
                    AreaNode grp = aroot.createSuperArea(bounds, region, "<area>");
                    if (grp != null)
                    {
                        grp.setFixedMarkedness(lroot.getFirstAreaNode().getMarkedness());
                        
                        //modify the logical subtree accordingly
                        if (lroot.getParentNode() != null)
                        {
                            LogicalNode newroot = new LogicalNode(grp);
                            lroot.getParentNode().insert(newroot, childIndex);
                            newroot.addNode(lroot);
                        }
                    }
                    
                    changed = true;
                }
            }
        }
        if (newlevel <= MAXLEVEL)
        {
            for (int i = 0; i < lroot.getChildCount(); i++)
                recursiveCreateLogicalAreas(lroot.getChildNode(i), i, newlevel);
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
    
}
