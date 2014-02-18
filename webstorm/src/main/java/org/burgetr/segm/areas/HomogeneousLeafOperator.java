/**
 * HomogeneousLeafOperator.java
 *
 * Created on 24. 10. 2013, 15:12:59 by burgetr
 */
package org.burgetr.segm.areas;

import org.burgetr.segm.AreaNode;
import org.burgetr.segm.AreaTree;

/**
 * This operator joins the homogeneous-style leaf nodes to larger artificial areas. 
 * 
 * @author burgetr
 */
public class HomogeneousLeafOperator extends SuperAreaOperator
{
    public HomogeneousLeafOperator()
    {
        super(10);
    }
    
    @Override
    public void apply(AreaTree atree)
    {
        findHomogeneousLeafs(atree.getRoot());
    }

    @Override
    public void apply(AreaTree atree, AreaNode root)
    {
        findHomogeneousLeafs(root);
    }

    //==============================================================================

    @Override
    protected GroupAnalyzer createGroupAnalyzer(AreaNode root)
    {
        return new GroupAnalyzerByStyles(root, 1, true);
    }
    
    //==============================================================================

    /**
     * Takes the leaf areas and tries to join the homogeneous paragraphs.
     */
    private void findHomogeneousLeafs(AreaNode root)
    {
        if (root.getChildCount() > 1)
            findSuperAreas(root, 1);
        for (int i = 0; i < root.getChildCount(); i++)
            findHomogeneousLeafs(root.getChildArea(i));
    }
    

}
