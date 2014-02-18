/**
 * CollapseAreasOperator.java
 *
 * Created on 25. 10. 2013, 15:46:34 by burgetr
 */
package org.burgetr.segm.areas;

import org.burgetr.segm.AreaNode;
import org.burgetr.segm.AreaTree;

/**
 * This operator collapses the areas having only one (leaf) child. 
 * 
 * @author burgetr
 */
public class CollapseAreasOperator implements AreaTreeOperator
{
    
    public CollapseAreasOperator()
    {
        
    }

    
    @Override
    public void apply(AreaTree atree)
    {
        recursiveCollapseAreas(atree.getRoot());
    }

    @Override
    public void apply(AreaTree atree, AreaNode root)
    {
        recursiveCollapseAreas(root);
    }
    
    //==============================================================================
    
    private void recursiveCollapseAreas(AreaNode root)
    {
        if (canCollapse(root))
        {
            root.collapseSubtree();
        }
        else
        {
            for (int i = 0; i < root.getChildCount(); i++)
                recursiveCollapseAreas(root.getChildArea(i));
        }
    }
    
    private boolean canCollapse(AreaNode area)
    {
        return (area.getChildCount() == 1 && area.getChildAt(0).isLeaf());
    }
    


}
