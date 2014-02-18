/**
 * TableSplitOperator.java
 *
 * Created on 29. 11. 2013, 22:40:09 by burgetr
 */
package org.burgetr.segm.areas;

import java.util.Vector;

import org.burgetr.segm.AreaNode;
import org.burgetr.segm.AreaTree;
import org.burgetr.segm.LayoutAnalyzer;
import org.burgetr.segm.Rectangular;

/**
 * This operator splits the visual areas that contain different types of layout and creates
 * smaller areas of homogeneous layout types. This means that the detected table and lists
 * will create separate visual areas.
 * 
 * @author burgetr
 */
public class LayoutSplitOperator implements AreaTreeOperator
{
    protected LayoutAnalyzer la;

    public LayoutSplitOperator(LayoutAnalyzer la)
    {
        this.la = la;
    }
    
    @Override
    public void apply(AreaTree atree)
    {
        apply(atree, atree.getRoot());
    }

    @Override
    public void apply(AreaTree atree, AreaNode root)
    {
        recursiveFindTables(root);
    }

    //==============================================================================

    private void recursiveFindTables(AreaNode root)
    {
        for (int i = 0; i < root.getChildCount(); i++)
            recursiveFindTables(root.getChildArea(i));
        findTables(root);
    }

    private void findTables(AreaNode root)
    {
        //find table bounds
        Vector<Integer> breaks = new Vector<Integer>();
        for (int i = 0; i < root.getChildCount(); i++)
        {
            int tend = la.findTableEnd(root, root.getChildAreas(), i);
            if (tend > i + 1) //found a table
            {
                //mark the beginning and end
                if (i > 0)
                    breaks.add(i);
                if (tend + 1 < root.getChildCount())
                    breaks.add(tend + 1);
                //skip the table
                i = tend;
            }
            else
            {
                int lend = la.findListEnd(root, root.getChildAreas(), i);
                if (lend > i + 2) //found a list
                {
                    //mark the beginning and end
                    if (i > 0)
                        breaks.add(i);
                    if (lend + 1 < root.getChildCount())
                        breaks.add(lend + 1);
                    //skip the list
                    i = lend;
                }
            }
        }
        //split the area with breaks
        if (!breaks.isEmpty())
        {
            AreaNode[][] regions = new AreaNode[breaks.size() + 1][];
            int strt = 0;
            int i = 0;
            for (int end : breaks)
            {
                regions[i] = new AreaNode[end - strt];
                for (int j = 0; j < end - strt; j++)
                    regions[i][j] = root.getChildArea(strt + j);
                strt = end;
                i++;
            }
            int end = root.getChildCount();
            regions[i] = new AreaNode[end - strt];
            for (int j = 0; j < end - strt; j++)
                regions[i][j] = root.getChildArea(strt + j);

            for (AreaNode[] sub : regions)
            {
                createSubArea(root, sub);
            }
        }
    }
    
    private AreaNode createSubArea(AreaNode root, AreaNode[] sub)
    {
        if (sub.length > 1)
        {
            Vector<AreaNode> region = new Vector<AreaNode>(sub.length);
            Rectangular bounds = null;
            
            for (AreaNode area : sub)
            {
                region.add(area);
                if (bounds == null)
                    bounds = new Rectangular(area.getGridPosition());
                else
                    bounds.expandToEnclose(area.getGridPosition());
            }
            
            AreaNode grp = root.createSuperArea(bounds, region, "<areaT>");
            return grp;
        }
        else
            return null;
    }
    
}
