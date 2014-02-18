/**
 * LogicalTree.java
 *
 * Created on 20.10.2008, 16:02:01 by burgetr
 */
package org.burgetr.segm;

/**
 * This tree represents the logical document structure.
 * 
 * @author burgetr
 */
public abstract class LogicalTree implements GenericAreaTree
{
    protected AreaTree atree;
    protected LogicalNode root;
    
    /**
     * Creates the logical document structure from an area tree (the segmented document).
     * @param atree the working tree of areas
     * @param fa the feature analyzer used for computing the logical structure
     */
    public LogicalTree(AreaTree atree)
    {
        this.atree = atree;
    }
    
    public LogicalNode getRoot()
    {
        return root;
    }
    
    /**
     * Obtains the corresponding area tree.
     * @return The area tree.
     */
    public AreaTree getAreaTree()
    {
        return atree;
    }
    
    //==========================================================================================================
    
    public AreaNode getAreaAt(int x, int y)
    {
        return atree.getAreaAt(x, y);
    }

    public AreaNode getAreaByName(String name)
    {
        return atree.getAreaByName(name);
    }
    
    public LogicalNode findArea(AreaNode area)
    {
        return root.findArea(area);
    }
    
    //==========================================================================================================
    
    /**
     * Joins the joinable nodes in the tree if they are joinable based on a join analyzer.
     * @param ja the NodeJoinAnalyzer to be used
     */
    public void joinTaggedNodes(NodeJoinAnalyzer ja)
    {
        recursiveJoinTaggedNodes(getRoot(), ja);
    }
    
    /**
     * Joins the joinable areas int the area tree if they share some tags.
     * @param root the root of the tree to process
     */
    protected void recursiveJoinTaggedNodes(LogicalNode root, NodeJoinAnalyzer ja)
    {
        if (!root.isLeaf())
        {
            LogicalNode c1 = root.getChildNode(0);
            for (int i = 1; i < root.getChildCount(); i++)
            {
                LogicalNode c2 = root.getChildNode(i);
                if (ja.isJoinable(c1, c2))
                {
                    c1.getAreaNodes().addAll(c2.getAreaNodes()); //copy the referenced areas
                    while (c2.getChildCount() > 0) //move all the child nodes
                        c1.add(c2.getChildNode(0));
                    root.remove(i);
                    i--;
                }
                else
                    c1 = c2;
            }
            
            for (int i = 0; i < root.getChildCount(); i++)
                recursiveJoinTaggedNodes(root.getChildNode(i), ja);
        }
    }
    
    
    
}
