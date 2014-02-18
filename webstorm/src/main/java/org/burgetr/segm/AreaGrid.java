/**
 * AreaGrid.java
 *
 * Created on 29.6.2006, 10:30:36 by burgetr
 */
package org.burgetr.segm;

import java.util.*;

/**
 * A grid of visual areas that contains all the child areas of a visual area node.
 * 
 * @author burgetr
 */
public class AreaGrid
{
    /** The maximal difference between two lengths that are considered a "being the same" */
    public static final int GRID_THRESHOLD = 0;
    
    /** Number of columns */
    private int width;
    
    /** Minimal indentation level */
    private int minindent;
    
    /** Maximal indentation level */
    private int maxindent;

    /** Array of column widths */
    private int[] cols;
    
    /** Number of rows */
    private int height;
    
    /** Array of row heights */
    private int[] rows;
    
    /** Enclosing visual area node */
    private AreaNode parent;
    
    //================================================================================
    
    public AreaGrid(AreaNode anode)
    {
        parent = anode;
        calculateColumns();
        calculateRows();
    }
    
    //================================================================================
    
    /**
     * @return Returns the cols.
     */
    public int[] getCols()
    {
        return cols;
    }

    /**
     * @return Returns the height.
     */
    public int getHeight()
    {
        return height;
    }

    /**
     * @return Returns the rows.
     */
    public int[] getRows()
    {
        return rows;
    }

    /**
     * @return Returns the width.
     */
    public int getWidth()
    {
        return width;
    }
    
    public int getMinIndent()
    {
        return minindent;
    }
    
    public int getMaxIndent()
    {
        return maxindent;
    }
    
    public String toString()
    {
    	return "Grid " + width + "x" + height;
    }
    
    /**
     * Find a node at the specified position in the grid.
     * @param x the <code>x</code> coordinate of the grid cell  
     * @param y the <code>y</code> coordinate of the grid cell  
     * @return the node at the specified position or null if there is no node
     */
	@SuppressWarnings("rawtypes")
    public AreaNode getNodeAt(int x, int y)
    {
        if (x < width && y < height)
        {
            for (Enumeration e = parent.children(); e.hasMoreElements(); )
            {
                AreaNode node = (AreaNode) e.nextElement();
                if (x >= node.getGridX() && x < node.getGridX() + node.getGridWidth() &&
                    y >= node.getGridY() && y < node.getGridY() + node.getGridHeight())
                    return node;
            }
            return null;
        }
        else
            return null;
    }
    
    /**
     * Checks if the cell with the specified coordinates is empty.
     * @param x the <code>x</code> coordinate of the grid cell  
     * @param y the <code>y</code> coordinate of the grid cell  
     * @return true if the cell doesn't contain any subarea
     */
    public boolean cellEmpty(int x, int y)
    {
    	return getNodeAt(x, y) == null;
    }
    
    /**
     * @return the offset of the specified column from the grid origin. Column 0
     * has always the offset 0
     */
    public int getColOfs(int col) throws ArrayIndexOutOfBoundsException
    {
    	if (col < width)
    	{
    		int ofs = 0;
    		for (int i = 0; i < col; i++)
    			ofs += cols[i];
    		return ofs;
    	}
    	else if (col == width)
    		return parent.getWidth();
    	else
    		throw new ArrayIndexOutOfBoundsException(col + ">" + width + " (" + parent + ")");
    }
    
    /**
     * @return the offset of the specified row from the grid origin. Row 0
     * has always the offset 0
     */
    public int getRowOfs(int row) throws ArrayIndexOutOfBoundsException
    {
    	if (row < height)
    	{
	    	int ofs = 0;
	    	for (int i = 0; i < row; i++)
	    		ofs += rows[i];
	    	return ofs;
    	}
    	else if (row == height)
    		return parent.getHeight();
    	else
    		throw new ArrayIndexOutOfBoundsException(row + ">" + height + " (" + parent + ")");
    }
    
    /**
     * @return the coordinates of the specified grid cell relatively to the area
     */
    public Rectangular getCellBoundsRelative(int x, int y)
    {
        int x1 = getColOfs(x);
        int y1 = getRowOfs(y);
        int x2 = (x == width-1) ? parent.getWidth() - 1 : x1 + cols[x] - 1;
        int y2 = (y == height-1) ? parent.getHeight() - 1 : y1 + rows[y] - 1;
        return new Rectangular(x1, y1, x2, y2);
    }
    
    /**
     * @return the coordinates of the specified grid cell absolutely
     */
    public Rectangular getCellBoundsAbsolute(int x, int y)
    {
        int x1 = parent.getX() + getColOfs(x);
        int y1 = parent.getY() + getRowOfs(y);
        int x2 = ((x == width-1) ? parent.getX() + parent.getWidth() - 1 : x1 + cols[x] - 1);
        int y2 = ((y == height-1) ? parent.getY()+ parent.getHeight() - 1 : y1 + rows[y] - 1);
        return new Rectangular(x1, y1, x2, y2);
    }
    
    /**
     * @return the coordinates of the specified area absolutely]
     */
    public Rectangular getAreaBoundsAbsolute(Rectangular area)
    {
        int x1 = parent.getX() + getColOfs(area.getX1());
        int y1 = parent.getY() + getRowOfs(area.getY1());
        Rectangular end = getCellBoundsAbsolute(area.getX2(), area.getY2());
        return new Rectangular(x1, y1, end.getX2(), end.getY2());
    }
    
    /**
     * Finds a grid cell that contains the specified point
     * @param x the x coordinate of the specified point
     * @returns the X offset of the grid cell that contains the specified absolute 
     * x coordinate or -1 when there is no such cell
     */
    public int findCellX(int x)
    {
    	int ofs = parent.getX();
    	for (int i = 0; i < cols.length; i++)
    	{
    		ofs += cols[i];
    		if (x < ofs)
    			return i;
    	}
    	return -1;
    }
    
    /**
     * Finds a grid cell that contains the specified point
     * @param y the y coordinate of the specified point
     * @returns the Y offset of the grid cell that contains the specified absolute 
     * y coordinate or -1 when there is no such cell
     */
    public int findCellY(int y)
    {
    	int ofs = 0;
    	for (int i = 0; i < rows.length; i++)
    	{
    		ofs += rows[i];
    		if (y < ofs + parent.getY())
    			return i;
    	}
    	return -1;
    }
    
    //================================================================================
    
    /**
     * @return <code>true</code> if the values are equal in the specified threshold
     */
    private boolean theSame(int val1, int val2)
    {
        return Math.abs(val2 - val1) <= GRID_THRESHOLD;
    }
    
    /**
     * Goes through the child areas and creates a list of collumns
     */
    @SuppressWarnings("rawtypes")
	private void calculateColumns()
    {
        //create the sorted list of points
        GridPoint points[] = new GridPoint[parent.getChildCount() * 2];
        int pi = 0;
        for (Enumeration e = parent.children(); e.hasMoreElements(); pi += 2)
        {
            AreaNode node = (AreaNode) e.nextElement();
            points[pi] = new GridPoint(node.getArea().getX1(), node, true);
            points[pi+1] = new GridPoint(node.getArea().getX2() + 1, node, false);
            //X2+1 ensures that the end of one box will be on the same point
            //as the start of the following box
        }
        Arrays.sort(points);
        
        //calculate the number of columns
        int cnt = 0;
        int last = parent.getArea().getX1();
        for (int i = 0; i < points.length; i++)
            if (!theSame(points[i].value, last))
            { 
                last = points[i].value;
                cnt++;
            }
        if (!theSame(last, parent.getArea().getX2()))
        	cnt++; //last column finishes the whole area
        width = cnt;
        
        //calculate the column widths and the layout
        maxindent = 0;
        minindent = -1;
        cols = new int[width];
        cnt = 0;
        last = parent.getArea().getX1();
        for (int i = 0; i < points.length; i++)
        {
            if (!theSame(points[i].value, last)) 
            {
                cols[cnt] = points[i].value - last;
                last = points[i].value;
                cnt++;
            }
            if (points[i].begin)
            {
                points[i].node.getGridPosition().setX1(cnt);
                maxindent = cnt;
                if (minindent == -1) minindent = maxindent;
                //points[i].node.getArea().setX1(parent.getArea().getX1() + getColOfs(cnt));
            }
            else
            {
                Rectangular pos = points[i].node.getGridPosition(); 
                pos.setX2(cnt-1);
                if (pos.getX2() < pos.getX1())
                    pos.setX2(pos.getX1());
                //points[i].node.getArea().setX2(parent.getArea().getX1() + getColOfs(pos.getX2()+1));
            }
        }
        if (!theSame(last, parent.getArea().getX2()))
        	cols[cnt] = parent.getArea().getX2() - last;
        if (minindent == -1)
            minindent = 0;
    }

    /**
     * Goes through the child areas and creates a list of rows
     */
    @SuppressWarnings("rawtypes")
	private void calculateRows()
    {
        //create the sorted list of points
        GridPoint points[] = new GridPoint[parent.getChildCount() * 2];
        int pi = 0;
        for (Enumeration e = parent.children(); e.hasMoreElements(); pi += 2)
        {
            AreaNode node = (AreaNode) e.nextElement();
            points[pi] = new GridPoint(node.getArea().getY1(), node, true);
            points[pi+1] = new GridPoint(node.getArea().getY2() + 1, node, false);
            //Y2+1 ensures that the end of one box will be on the same point
            //as the start of the following box
        }
        Arrays.sort(points);
        
        //calculate the number of rows
        int cnt = 0;
        int last = parent.getArea().getY1();
        for (int i = 0; i < points.length; i++)
            if (!theSame(points[i].value, last))
            { 
                last = points[i].value;
                cnt++;
            }
        if (!theSame(last, parent.getArea().getY2()))
        	cnt++; //last row finishes the whole area
        height = cnt;
        
        //calculate the row heights and the layout
        rows = new int[height];
        cnt = 0;
        last = parent.getArea().getY1();
        for (int i = 0; i < points.length; i++)
        {
            if (!theSame(points[i].value, last)) 
            {
                rows[cnt] = points[i].value - last;
                last = points[i].value;
                cnt++;
            }
            if (points[i].begin)
            {
                points[i].node.getGridPosition().setY1(cnt);
                //points[i].node.getArea().setY1(parent.getArea().getY1() + getRowOfs(cnt));
            }
            else
            {
                Rectangular pos = points[i].node.getGridPosition(); 
                pos.setY2(cnt-1);
                if (pos.getY2() < pos.getY1())
                    pos.setY2(pos.getY1());
                //points[i].node.getArea().setY2(parent.getArea().getY1() + getRowOfs(pos.getY2()+1));
            }
        }
        if (!theSame(last, parent.getArea().getY2()))
        	rows[cnt] = parent.getArea().getY2() - last;
    }
    
    
}

/** A point in the grid */
class GridPoint implements Comparable<GridPoint>
{
    public int value;       //the point position
    public AreaNode node;   //the corresponding visual area node
    public boolean begin;   //is it the begining or the end of the node?
    
    public GridPoint(int value, AreaNode node, boolean begin)
    {
        this.value = value;
        this.node = node;
        this.begin = begin;
    }
    
    public int compareTo(GridPoint other)
    {
        return value - ((GridPoint) other).value;
    }
}
