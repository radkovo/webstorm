/**
 * 
 */

package org.burgetr.segm;

import java.awt.Color;
import java.awt.Graphics;

import org.fit.cssbox.layout.BrowserCanvas;

/**
 * @author radek
 * A horizontal or vertical visual separator.
 */
public class Separator extends Rectangular implements Comparable<Separator>
{
    /* Separator types */
	public static final short HORIZONTAL = 0;
    public static final short VERTICAL = 1;
    public static final short BOXH = 2;
    public static final short BOXV = 3;
    
    /** Separator type -- either HORIZONTAL, VERTICAL or BOX */
    protected short type;
    
    /** Left (top) separated area node (if any) */
    protected AreaNode area1;
    
    /** Bottom (right) separated area node (if any) */
    protected AreaNode area2;
    
    //======================================================================================
    
	public Separator(short type, int x1, int y1, int x2, int y2)
	{
        super(x1, y1, x2, y2);
        this.type = type;
        area1 = null;
        area2 = null;
	}

	public Separator(Separator orig)
	{
        super(orig);
        this.type = orig.type;
        area1 = null;
        area2 = null;
	}
	
    public Separator(short type, Rectangular rect)
    {
        super(rect);
        this.type = type;
        area1 = null;
        area2 = null;
    }

    public short getType()
    {
        return type;
    }
    
    public void setType(short type)
    {
    	this.type = type;
    }
    
    public boolean isBoxSep()
    {
        return type == BOXH || type == BOXV;
    }

    public AreaNode getArea1()
    {
        return area1;
    }

    public void setArea1(AreaNode area1)
    {
        this.area1 = area1;
    }

    public AreaNode getArea2()
    {
        return area2;
    }

    public void setArea2(AreaNode area2)
    {
        this.area2 = area2;
    }

    public String toString()
    {
        String t = "?";
        switch (type)
        {
            case HORIZONTAL: t = "HSep"; break;
            case VERTICAL:   t = "VSep"; break;
            case BOXH:       t = "BoxH"; break;
            case BOXV:       t = "BoxV"; break;
        }
        return t + " (" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + ")" + " w=" + getWeight() + " a1=" + area1 + " a2=" + area2;
    }

    public int compareTo(Separator other)
    {
        /*if (type == HORIZONTAL || type == BOXH)
        {
            if (x1 == other.x1)
                return y1 - other.y1;
            else
                return x1 - other.x1;
        }
        else
        {
            if (y1 == other.y1)
                return x1 - other.x1;
            else
                return y1 - other.y1;
        }*/
        return other.getWeight() - getWeight();
    }

	//======================================================================================

    public boolean isHorizontal()
    {
        return getWidth() >= getHeight();
    }
    
    public boolean isVertical()
    {
        return getWidth() < getHeight();
    }
    
	public int getWeight()
	{
		int ww = Math.min(getWidth(), getHeight()) / 10; //TODO: zatim jen nastrel vypoctu
		ww = isVertical() ? (ww * 2) : ww;
		if (area1 != null && area2 != null)
		{
		    if (!area1.getEffectiveBackgroundColor().equals(area2.getEffectiveBackgroundColor()))
		        ww += 4;
		    if (!area1.hasSameStyle(area2))
		        ww += 2;
		    if (area2.getArea().getAverageFontSize() - Config.FONT_SIZE_THRESHOLD > area1.getArea().getAverageFontSize())
		        ww += 2;
		}
		return ww;
	}
	
    //======================================================================================
	
	public Separator hsplit(Separator other)
	{
	    Rectangular r = super.hsplit(other);
	    if (r == null)
	        return null;
	    else
	        return new Separator(type, r);
	}
	
    public Separator vsplit(Separator other)
    {
        Rectangular r = super.vsplit(other);
        if (r == null)
            return null;
        else
            return new Separator(type, r);
    }
    
    //======================================================================================
    
    public void drawExtent(BrowserCanvas canvas)
    {
        Graphics ig = canvas.getImageGraphics();
        Color c = ig.getColor();
        if (isHorizontal())
            ig.setColor(Color.BLUE);
        else
            ig.setColor(Color.RED);
        ig.fillRect(getX1(), getY1(), getWidth(), getHeight());
        ig.setColor(c);
    }

}
