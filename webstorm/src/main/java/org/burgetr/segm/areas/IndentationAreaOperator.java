/**
 * IndentationAreaOperator.java
 *
 * Created on 4. 12. 2013, 14:37:01 by burgetr
 */
package org.burgetr.segm.areas;

import java.util.Collections;
import java.util.Vector;

import org.burgetr.segm.AreaNode;
import org.burgetr.segm.AreaTree;
import org.burgetr.segm.LayoutAnalyzer;
import org.burgetr.segm.Rectangular;
import org.burgetr.segm.AreaNode.LayoutType;

/**
 * 
 * @author burgetr
 */
public class IndentationAreaOperator extends LayoutAnalyzer implements AreaTreeOperator
{
    protected LayoutAnalyzer la;

    public IndentationAreaOperator(AreaTree atree)
    {
        super(atree);
    }
    
    @Override
    public void apply(AreaTree atree)
    {
        apply(atree, atree.getRoot());
    }

    @Override
    public void apply(AreaTree atree, AreaNode root)
    {
        recursiveFindColumns(root);
    }

    //==============================================================================
    
    @Override
    public AreaNode.LayoutType detectLayoutType(AreaNode area)
    {
        if (isTable(area, getSortedChildren(area), 0))
            return LayoutType.TABLE;
        else
            return LayoutType.NORMAL;
    }
    
    //==============================================================================

    private void recursiveFindColumns(AreaNode root)
    {
        for (int i = 0; i < root.getChildCount(); i++)
            recursiveFindColumns(root.getChildArea(i));
        if (detectLayoutType(root) == LayoutType.TABLE)
            findColumns(root);
    }

    private void findColumns(AreaNode root)
    {
        Vector<AreaNode> nodes = getSortedChildren(root);
        TableInfo stat = collectTableStats(root, nodes, 0, false);
        int[] cols = findTableGridPositions(stat);
        
        //TODO
        //...
        
        //find table bounds
        Vector<Integer> breaks = new Vector<Integer>();
        for (int i = 0; i < nodes.size(); i++)
        {
            int tend = la.findTableEnd(root, nodes, i);
            if (tend > i + 1) //found a table
            {
                //mark the beginning and end
                if (i > 0)
                    breaks.add(i);
                if (tend + 1 < nodes.size())
                    breaks.add(tend + 1);
                //skip the table
                i = tend;
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
                    regions[i][j] = nodes.get(strt + j);
                strt = end;
                i++;
            }
            int end = root.getChildCount();
            regions[i] = new AreaNode[end - strt];
            for (int j = 0; j < end - strt; j++)
                regions[i][j] = nodes.get(strt + j);

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
    
    private Vector<AreaNode> getSortedChildren(AreaNode area)
    {
        Vector<AreaNode> children = area.getChildAreas();
        if (children != null)
        {
            Vector<AreaNode> ret = new Vector<AreaNode>(children);
            Collections.sort(ret, new AreaPositionComparator());
            return ret;
        }
        else
            return new Vector<AreaNode>(1);
    }
    
}
