/**
 * AreaStyle.java
 *
 * Created on 29.6.2012, 12:43:15 by burgetr
 */
package org.burgetr.segm;

import java.awt.Color;

/**
 * @author burgetr
 *
 */
public class AreaStyle
{
    private double averageFontSize;
    private double averageFontWeight;
    private double averageFontStyle;
    private double averageColorLuminosity;
    private Color backgroundColor;
    
    public AreaStyle(double averageFontSize, double averageFontWeight,
            double averageFontStyle, double averageColorLuminosity,
            Color backgroundColor)
    {
        this.averageFontSize = averageFontSize;
        this.averageFontWeight = averageFontWeight;
        this.averageFontStyle = averageFontStyle;
        this.averageColorLuminosity = averageColorLuminosity;
        this.backgroundColor = backgroundColor;
    }
    
    public AreaStyle(AreaNode source)
    {
        this.averageFontSize = source.getArea().getAverageFontSize();
        this.averageFontWeight = source.getArea().getAverageFontWeight();
        this.averageFontStyle = source.getArea().getAverageFontStyle();
        this.averageColorLuminosity = source.getArea().getAverageColorLuminosity();
        this.backgroundColor = source.getArea().getBackgroundColor();
    }

    public double getAverageFontSize()
    {
        return averageFontSize;
    }

    public void setAverageFontSize(double averageFontSize)
    {
        this.averageFontSize = averageFontSize;
    }

    public double getAverageFontWeight()
    {
        return averageFontWeight;
    }

    public void setAverageFontWeight(double averageFontWeight)
    {
        this.averageFontWeight = averageFontWeight;
    }

    public double getAverageFontStyle()
    {
        return averageFontStyle;
    }

    public void setAverageFontStyle(double averageFontStyle)
    {
        this.averageFontStyle = averageFontStyle;
    }

    public double getAverageColorLuminosity()
    {
        return averageColorLuminosity;
    }

    public void setAverageColorLuminosity(double averageColorLuminosity)
    {
        this.averageColorLuminosity = averageColorLuminosity;
    }

    public Color getBackgroundColor()
    {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor)
    {
        this.backgroundColor = backgroundColor;
    }
    
    
    /**
     * Compares two styles and decides if it is the same style. The thresholds of the style are taken from the {@link Config}.
     * @param other the other area to be compared
     * @return <code>true</code> if the areas are considered to have the same style
     */
    public boolean isSameStyle(AreaStyle other)
    {
        double fsdif = Math.abs(getAverageFontSize() - other.getAverageFontSize());
        double wdif = Math.abs(getAverageFontWeight() - other.getAverageFontWeight());
        double sdif = Math.abs(getAverageFontStyle() - other.getAverageFontStyle());
        double ldif = Math.abs(getAverageColorLuminosity() - other.getAverageColorLuminosity());
        Color bg1 = getBackgroundColor();
        Color bg2 = other.getBackgroundColor();
        
        return fsdif <= Config.FONT_SIZE_THRESHOLD 
                && wdif <= Config.FONT_WEIGHT_THRESHOLD
                && sdif <= Config.FONT_STYLE_THRESHOLD
                && ldif <= Config.TEXT_LUMINOSITY_THRESHOLD
                && ((bg1 == null && bg2 == null) || (bg1 != null && bg2 != null && bg1.equals(bg2)));
    }
    
    
}
