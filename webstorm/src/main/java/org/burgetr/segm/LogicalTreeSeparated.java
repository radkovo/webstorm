/**
 * LogicalTreeSeparated.java
 *
 * Created on 6. 11. 2013, 16:13:35 by burgetr
 */
package org.burgetr.segm;

/**
 * A logical tree created using the "separated" algorithm:
 * The areas are analyzed separately. The structure of sub-areas within an area is determined
 * only by comparing the markedness of the areas with applying cretain threshold.
 * @author burgetr
 */
public class LogicalTreeSeparated extends LogicalTree
{
    protected FeatureAnalyzer fa;
    protected LayoutAnalyzer la;

    public LogicalTreeSeparated(AreaTree atree, FeatureAnalyzer fa, LayoutAnalyzer la)
    {
        super(atree);
        this.fa = fa;
        this.la = la;
        computeAreaMarkedness(atree.getRoot());
        root = recursiveCreateLogicalStructure(atree.getRoot());
    }
    
    //==========================================================================================================
    
    /**
     * Creates the logical structure tree from the source area tree recursively.
     * @param src the area tree root node
     * @return the root of the new tree
     */
    protected LogicalNode recursiveCreateLogicalStructure(AreaNode src)
    {
        if (src.getChildCount() == 0)
            return new LogicalNode(src);
        else
        {
            //System.out.println("Processing: " + src);
            LogicalNode newroot = new LogicalNode(src);
            LogicalNode firstnode = recursiveCreateLogicalStructure(src.getChildArea(0));
            newroot.add(firstnode);
            
            TreeCreationStatus curstat = new TreeCreationStatus();
            curstat.node = firstnode;
            curstat.level = getMarkedness(curstat.node); //current markedness level
            curstat.pos = curstat.node.getFirstAreaNode().getArea().getBounds();
            
            for (int i = 1; i < src.getChildCount(); i++)
            {
                AreaNode child = src.getChildArea(i);
                
                if (!child.getArea().isSeparator()) //skip areas used for separation only
                {
                    TreeCreationStatus substat = new TreeCreationStatus();
                    substat.node = recursiveCreateLogicalStructure(child);
                    substat.level = getMarkedness(substat.node);
                    substat.pos = substat.node.getFirstAreaNode().getArea().getBounds();
                    
                    //find the appropriate parent
                    LogicalNode candParent = findParentForNode(curstat, substat, newroot);
                    candParent.addNode(substat.node);
                    curstat.replaceWith(substat);
                }
            }
            
            //collapse the logical node if it is too simple (no internal structure)
            if (newroot.getLeafCount() == 1)
            {
                newroot.setContentTree(newroot.getChildNode(0));
                newroot.removeAllChildren();
            }
            
            return newroot;
        }
    }
    
    //==========================================================================================================
    
    /**
     * Locates an appropriate parent in the current tree for the new node
     * @param curstat Current position in the tree
     * @param substat The new candidate position
     * @param root the root node of the current logical subtree used as "reset" parent in case of detected flow break
     * @return the parent node where the candidate should be added to
     */
    protected LogicalNode findParentForNode(TreeCreationStatus curstat, TreeCreationStatus substat, LogicalNode root)
    {
        //TODO: should the children be sorted in any way?
        //TODO: better flow break detection?
        if (substat.pos.getY1() < curstat.pos.getY2() - 10)  //flow break - go to the parent, 10 pixels tollerance for now
        {
            //System.out.println("break: 1=" + current + " 2=" + substat.node);
            return root;
        }
        else //normal flow, compare the levels
        {
            LogicalNode candParent;
            
            LogicalNode cparent = curstat.node.getParentNode();
            double plevel = getMarkedness(cparent);
            double pcur = Math.abs(substat.level - curstat.level); //price for going up (to the parent)
            double ppar = Math.abs(substat.level - plevel); //price of remaining here or going down
            
            if (pcur <= ppar) //remain here or go down
            {
                int c = compareMarkedness(substat.level, curstat.level);
                if (c < 0) //substat.level < clevel
                {
                    candParent = curstat.node;
                }
                else //if (c >= 0) //substat.level == clevel
                {
                    LogicalNode parent = curstat.node.getParentNode();
                    if (parent != null)
                    {
                        candParent = parent;
                    }
                    else
                    {
                        System.err.println("ERROR: LogicalTree: no parent");
                        candParent = curstat.node;
                    }
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
            
            return candParent;
        }
    }
    
    //==========================================================================================================
    
    protected double getMarkedness(LogicalNode node)
    {
        if (node == null)
            return 0;
        else
            return node.getFirstAreaNode().getMarkedness();
    }
    
    /**
     * @return 0 when equal (in the given threshold), 1 when m2>m1, -1 when m2<m1
     */
    protected int compareMarkedness(double m1, double m2)
    {
        double dif = m2 - m1;
        if (Math.abs(dif) < FeatureAnalyzer.MIN_MARKEDNESS_DIFFERENCE)
            return 0;
        else
            return (dif > 0) ? -1 : 1;
    }
    
    /**
     * Recomputes the markedness in all the nodes of an area tree.
     * @param root the root of the tree to be recomputed
     */
    protected void computeAreaMarkedness(AreaNode root)
    {
        if (!root.hasFixedMarkedness())
            root.setMarkedness(fa.getMarkedness(root));
        
        root.setLayoutType(la.detectLayoutType(root));
        
        for (int i = 0; i < root.getChildCount(); i++)
            computeAreaMarkedness(root.getChildArea(i));
    }
    

    protected class TreeCreationStatus
    {
        public LogicalNode node;
        public double level;
        public Rectangular pos;
        
        public void replaceWith(TreeCreationStatus other)
        {
            node = other.node;
            level = other.level;
            pos = other.pos;
        }
    }
}
