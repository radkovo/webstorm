/**
 * GroupAnalyzerByFlooding.java
 *
 * Created on 23.1.2007, 14:29:39 by burgetr
 */
package org.burgetr.segm.areas;

import java.util.*;

import org.burgetr.segm.Area;
import org.burgetr.segm.AreaNode;
import org.burgetr.segm.Rectangular;
import org.burgetr.segm.SeparatorSet;

/**
 * @author burgetr
 *
 */
public class GroupAnalyzerByFlooding extends GroupAnalyzer
{
    private SeparatorSet seps;

    public GroupAnalyzerByFlooding(AreaNode parent)
    {
        super(parent);
        seps = null;
    }

    @Override
    public AreaNode findSuperArea(AreaNode sub,  Vector<AreaNode> selected)
    {
        parent.createSeparators();
        seps = parent.getSeparators();
        //try to expand
        //Rectangular limit = new Rectangular(0, 0, grid.getWidth()-1, grid.getHeight()-1);
        //expandToLimit(sub, gp, limit, true, true);
        Rectangular gp;
        if (sub.explicitelySeparated())
            gp = sub.getGridPosition();
        else
            gp = flood(sub.getGridPosition().getX1(), sub.getGridPosition().getY1());
        gp.expandToEnclose(sub.getGridPosition()); //ensure that at least the source node fits
        
        //select areas inside of the area found
        Rectangular mingp = null;
        selected.removeAllElements();
        for (int i = 0; i < parent.getChildCount(); i++)
        {
            AreaNode chld = parent.getChildArea(i);
            if (gp.encloses(chld.getGridPosition()))
            {
                selected.add(chld);
                if (mingp == null)
                    mingp = new Rectangular(chld.getGridPosition());
                else
                    mingp.expandToEnclose(chld.getGridPosition());
            }
        }
        
        //create the new area
        Area area = new Area(parent.getArea().getX1() + grid.getColOfs(mingp.getX1()),
                             parent.getArea().getY1() + grid.getRowOfs(mingp.getY1()),
                             parent.getArea().getX1() + grid.getColOfs(mingp.getX2()+1) - 1,
                             parent.getArea().getY1() + grid.getRowOfs(mingp.getY2()+1) - 1);
        //area.setBorders(true, true, true, true);
        AreaNode ret = new AreaNode(area);
        ret.setSeparated(true);
        ret.setGridPosition(mingp);
        //if more than one area is in the group, add them to the result
        //(if only one area is present, we don't move it to the resulting area)
        /*if (inside.size() > 1)
        {
            wait(1000);
            for (Iterator it = inside.iterator(); it.hasNext(); )
                ret.addArea((AreaNode) it.next());
        }*/
        return ret;
    }
      
    /**
     * Starts in the specified point and floods the area.
     * @param sx starting point x coordinate
     * @param sy starting point y coordinate
     * @return the rectangular bounds of the flooded area
     */
    private Rectangular flood(int x, int y)
    {
        boolean[][] visited = new boolean[grid.getWidth()][grid.getHeight()];
        boolean[][] filled = new boolean[grid.getWidth()][grid.getHeight()];
        for (int i = 0; i < grid.getWidth(); i++)
            for (int j = 0; j < grid.getHeight(); j++)
            {
                visited[i][j] = false;
                filled[i][j] = false;
            }
        
        visited[x][y] = true;
        visited[x+1][y] = true;
        filled[x][y] = true;
        filled[x+1][y] = true;
        LinkedList<Rectangular> queue = new LinkedList<Rectangular>();
        Rectangular ret = new Rectangular(x, y, x+1, y);
        queue.offer(ret);
        
        while (!queue.isEmpty())
        {
            Rectangular cur = queue.poll();
            int sx = cur.getX1();
            int sy = cur.getY1();
            ret.expandToEnclose(cur);
            
            filled[sx][sy] = true;
            Rectangular spos = grid.getCellBoundsAbsolute(sx, sy);
            //dispCell(sx, sy);
            
            //up
            //if (sy > 0 && !visited[sx][sy-1])
            if (sy > 0 && canFlood(visited, filled, sx, sy, -1, true))
            {
                visited[sx][sy-1] = true;
                Rectangular epos = grid.getCellBoundsAbsolute(sx, sy-1);
                if (!seps.isSeparatorAt(spos.midX(), spos.getY1()) &&
                    !seps.isSeparatorAt(epos.midX(), epos.getY2()))
                    queue.offer(new Rectangular(sx, sy-1, sx, sy-1));
            }
            //down
            //if (sy < grid.getHeight()-1 && !visited[sx][sy+1])
            if (sy < grid.getHeight()-1 && canFlood(visited, filled, sx, sy, 1, true))
            {
                visited[sx][sy+1] = true;
                Rectangular epos = grid.getCellBoundsAbsolute(sx, sy+1);
                if (!seps.isSeparatorAt(spos.midX(), spos.getY2()) &&
                    !seps.isSeparatorAt(epos.midX(), epos.getY1()))
                    queue.offer(new Rectangular(sx, sy+1, sx, sy+1));
            }
            //left
            //if (sx > 0 && !visited[sx-1][sy])
            if (sx > 0 && canFlood(visited, filled, sx, sy, -1, false))
            {
                visited[sx-1][sy] = true;
                Rectangular epos = grid.getCellBoundsAbsolute(sx-1, sy);
                if (!seps.isSeparatorAt(spos.getX1(), spos.midY()) &&
                    !seps.isSeparatorAt(epos.getX2(), epos.midY()))
                    queue.offer(new Rectangular(sx-1, sy, sx-1, sy));
            }
            //right
            //if (sx < grid.getWidth()-1 && !visited[sx+1][sy])
            if (sx < grid.getWidth()-1 && canFlood(visited, filled, sx, sy, 1, false))
            {
                visited[sx+1][sy] = true;
                Rectangular epos = grid.getCellBoundsAbsolute(sx+1, sy);
                if (!seps.isSeparatorAt(spos.getX2(), spos.midY()) &&
                    !seps.isSeparatorAt(epos.getX1(), epos.midY()))
                    queue.offer(new Rectangular(sx+1, sy, sx+1, sy));
            }
        }
        
        return ret;
    }

    private boolean canFlood(boolean[][] visited, boolean[][] filled, int x, int y, int dist, boolean vertical)
    {
        if (vertical)
        {
            if (visited[x][y+dist])
                return false;
            if ((x == 0 || !filled[x-1][y]) &&
                (x == filled.length-1 || !filled[x+1][y]))
                return false;
        }
        else
        {
            if (visited[x+dist][y])
                return false;
            if ((y == 0 || !filled[x][y-1]) &&
                (y == filled[x].length-1 || !filled[x][y+1]))
                return false;
        }
        return true;
    }
    
}
