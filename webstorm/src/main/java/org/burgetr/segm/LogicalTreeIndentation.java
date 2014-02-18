/**
 * LogicalTreeIndentation.java
 *
 * Created on 6. 11. 2013, 16:26:41 by burgetr
 */
package org.burgetr.segm;

/**
 * A logical tree class that takes mutual x-positions of the areas into account.
 * 
 * @author burgetr
 */
public class LogicalTreeIndentation extends LogicalTreeSeparated
{

    public LogicalTreeIndentation(AreaTree atree, FeatureAnalyzer fa, LayoutAnalyzer la)
    {
        super(atree, fa, la);
    }
    
    //==========================================================================================================
    
    @Override
    protected LogicalNode findParentForNode(TreeCreationStatus curstat, TreeCreationStatus substat, LogicalNode root)
    {
        //TODO: should the children be sorted in any way?
        //TODO: better flow break detection?
        if (substat.pos.getY1() < curstat.pos.getY1() - 10)  //flow break - go to the parent, 10 pixels tollerance for now
        {
            //System.out.println("break: 1=" + current + " 2=" + substat.node);
            return root;
        }
        else //normal flow, compare the levels
        {
            LogicalNode candParent;
            
            LogicalNode cparent = curstat.node.getParentNode();
            double plevel = getMarkedness(cparent);
            double pcur = Math.abs(substat.level - curstat.level); //price of remaining here or going down
            double ppar = Math.abs(substat.level - plevel); //price for going up (to the parent)
            
            if (pcur <= ppar) //remain here or go down
            {
                int c = compareMarkedness(substat.level, curstat.level);
                if (c < 0) //substat.level < clevel
                {
                    candParent = curstat.node;
                }
                else //substat.level == clevel
                {
                    candParent = cparent;
                }
            }
            else //parent is closer, search for the topmost applicable parent
            {
                LogicalNode parent = curstat.node;
                while (compareMarkedness(getMarkedness(parent), substat.level) <= 0 && parent.getParentNode() != null)
                {
                    parent = parent.getParentNode();
                }
                candParent = parent;
            }
            
            //consider the allowed indentation
            while (!allowedIndentation(substat.node, candParent) && candParent.getParentNode() != null)
                candParent = candParent.getParentNode();
            
            return candParent;
        }
    }
    
    /**
     * Checks whether a potential child node may be a logical child of the given parent based on mutual
     * positions of the corresponding areas.
     * @param child the potential child
     * @param parent the porential parent
     * @return true when yes
     */
    protected boolean allowedIndentation(LogicalNode child, LogicalNode parent)
    {
        //TODO nebere v potaz centrovane nadpisy, to je nezbytne
        int px = parent.getFirstAreaNode().getX();
        int cx = child.getFirstAreaNode().getX();
        return cx - px >= -2; //allow 2px to the left to overcome some errors during computations and rendering
        //return false;
    }
    
}
