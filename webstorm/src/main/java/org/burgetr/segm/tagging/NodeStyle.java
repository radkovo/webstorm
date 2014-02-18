/**
 * NodeStyle.java
 *
 * Created on 27.9.2012, 15:02:43 by burgetr
 */
package org.burgetr.segm.tagging;

import java.awt.Color;

import org.burgetr.segm.AreaNode;
import org.burgetr.segm.LogicalNode;

/**
 * This class represents the features of the node style that are important for node purpose
 * comparison.
 * @author burgetr
 */
public class NodeStyle
{
    private double fontSize;
    private double style;
    private double weight;
    private Color color;
    private int indent;
    
    /**
     * Computes the style of an area node.
     * @param area
     */
    public NodeStyle(AreaNode area)
    {
        fontSize = area.getArea().getAverageFontSize();
        style = area.getArea().getAverageFontStyle();
        weight = area.getArea().getAverageFontWeight();
        color = area.getFirstNestedBox().getBox().getVisualContext().getColor();
        indent = (int) Math.round(computeIndentation(area));
    }
    
    /**
     * Computes the style of a logical node.
     * @param node
     */
    public NodeStyle(LogicalNode node)
    {
        this(node.getFirstAreaNode());
    }
    
    /**
     * Computes the style of a logical node.
     * @param node
     */
    public NodeStyle(NodeStyle src)
    {
        this.fontSize = src.fontSize;
        this.style = src.style;
        this.weight = src.weight;
        this.color = new Color(src.color.getRed(), src.color.getGreen(), src.color.getGreen(), src.color.getAlpha());
        this.indent = src.indent;
    }
    
    public double getFontSize()
    {
        return fontSize;
    }

    public double getStyle()
    {
        return style;
    }

    public double getWeight()
    {
        return weight;
    }

    public Color getColor()
    {
        return color;
    }

    public int getIndent()
    {
        return indent;
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((color == null) ? 0 : color.hashCode());
        long temp;
        temp = Double.doubleToLongBits(fontSize);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + indent;
        temp = Double.doubleToLongBits(style);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(weight);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        NodeStyle other = (NodeStyle) obj;
        if (color == null)
        {
            if (other.color != null) return false;
        }
        else if (!color.equals(other.color)) return false;
        if (Double.doubleToLongBits(fontSize) != Double
                .doubleToLongBits(other.fontSize)) return false;
        if (indent != other.indent) return false;
        if (Double.doubleToLongBits(style) != Double
                .doubleToLongBits(other.style)) return false;
        if (Double.doubleToLongBits(weight) != Double
                .doubleToLongBits(other.weight)) return false;
        return true;
    }

    @Override
    public String toString()
    {
        String ret = "[fs:" + fontSize + " w:" + weight + " s:" + style + " c:";
        ret += String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        ret += " i:" + indent;
        ret += "]";
        return ret;
    }
    
    public String toARFFString()
    {
        return fontSize + "," + weight + "," + style + "," 
                + (color.getRed() / 255.0) + "," + (color.getGreen() / 255.0) + "," + (color.getBlue() / 255.0)
                + "," + indent; 
    }
    
    private double computeIndentation(AreaNode node)
    {
        final double max_levels = 3;
        
        if (node.getPreviousOnLine() != null)
            return computeIndentation(node.getPreviousOnLine()); //use the indentation of the first one on the line
        else
        {
            double ind = max_levels;
            if (!node.isCentered() && node.getParentArea() != null)
                ind = ind - (node.getGridX() - node.getParentArea().getGrid().getMinIndent());
            if (ind < 0) ind = 0;
            return ind / max_levels;
        }
    }

}
