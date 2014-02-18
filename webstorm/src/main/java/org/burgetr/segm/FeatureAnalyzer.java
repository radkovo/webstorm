/**
 * FeatureAnalyzer.java
 *
 * Created on 6.5.2011, 14:48:51 by burgetr
 */
package org.burgetr.segm;

import java.awt.Color;

/**
 * This class provides the methods for obtaining the values of various vertical features from the area tree.
 * 
 * @author burgetr
 */
public class FeatureAnalyzer
{
    /** Minimal difference in the markedness that should be interpreted as a difference between the meaning of the areas. */
    public static final double MIN_MARKEDNESS_DIFFERENCE = 0.5; //0.5 is the difference between the whole area in italics and not in italics
    
    public static final double[] DEFAULT_WEIGHTS = {1000.0, 2.0, 0.5, 5.0, 0.0, 1.0, 0.5, 100.0}; 
    
    //weights
    private static final int WFSZ = 0; 
    private static final int WFWT = 1;
    private static final int WFST = 2; 
    private static final int WIND = 3;
    private static final int WCON = 4;
    private static final int WCEN = 5;
    private static final int WCP = 6;
    private static final int WBCP = 7;
    
    private double[] weights;
    
    private AreaTree tree;
    private AreaNode root;
    private double avgfont;
    private ColorAnalyzer ca;
    private BackgroundColorAnalyzer bca;
    
    public FeatureAnalyzer(AreaTree tree)
    {
        weights = DEFAULT_WEIGHTS;
        setTree(tree);
    }
    
    public AreaTree getTree()
    {
        return tree;
    }
    
    public void setTree(AreaTree tree)
    {
        this.tree = tree;
        root = tree.getRoot();
        avgfont = root.getArea().getAverageFontSize();
        ca = new ColorAnalyzer(root);
        bca = new BackgroundColorAnalyzer(root);
    }
    
    public void setWeights(double[] weights)
    {
        this.weights = weights;
    }
    
    public double[] getWeights()
    {
        return weights;
    }
    
    public FeatureVector getFeatureVector(AreaNode node)
    {
        FeatureVector ret = new FeatureVector();
        Area area = node.getArea();
        String text = node.getText();
        int plen = text.length();
        if (plen == 0) plen = 1; //kvuli deleni nulou
        
        ret.setFontSize(area.getAverageFontSize() / avgfont);
        ret.setWeight(area.getAverageFontWeight());
        ret.setStyle(area.getAverageFontStyle());
        ret.setReplaced(area.isReplaced());
        ret.setAabove(countAreasAbove(node));
        ret.setAbelow(countAreasBelow(node));
        ret.setAleft(countAreasLeft(node));
        ret.setAright(countAreasRight(node));
        ret.setPdigits(countChars(text, Character.DECIMAL_DIGIT_NUMBER) / (double) plen);
        ret.setPlower(countChars(text, Character.LOWERCASE_LETTER) / (double) plen);
        ret.setPupper(countChars(text, Character.UPPERCASE_LETTER) / (double) plen);
        ret.setPspaces(countChars(text, Character.SPACE_SEPARATOR) / (double) plen);
        ret.setPpunct(countCharsPunct(text) / (double) plen);
        ret.setTlum(getAverageTextLuminosity(node));
        ret.setBglum(getBackgroundLuminosity(node));
        ret.setContrast(getContrast(node));
        ret.setCperc(ca.getColorPercentage(node));
        ret.setBcperc(bca.getColorPercentage(node));
        ret.setMarkedness(getMarkedness(node));
        ret.setTagLevel(node.getTagLevel());
        
        //TODO ostatni vlastnosti obdobne
        return ret;
    }
    
    /**
     * Computes the indentation metric.
     * @return the indentation metric (0..1) where 1 is for the non-indented areas, 0 for the most indented areas.
     */
    public double getIndentation(AreaNode node)
    {
        final double max_levels = 3;
        
        if (node.getPreviousOnLine() != null)
        	return getIndentation(node.getPreviousOnLine()); //use the indentation of the first one on the line
        else
        {
	        double ind = max_levels;
	        if (!node.isCentered() && node.getParentArea() != null)
	            ind = ind - (node.getGridX() - node.getParentArea().getGrid().getMinIndent());
	        if (ind < 0) ind = 0;
	        return ind / max_levels;
        }
    }
    
    /**
     * Computes the markedness of the area. The markedness generally describes the visual importance of the area based on different criteria.
     * @return the computed expressiveness
     */
    public double getMarkedness(AreaNode node)
    {
        Area area = node.getArea();
        double fsz = area.getAverageFontSize() / avgfont; //use relative font size, 0 is the normal font
        double fwt = area.getAverageFontWeight();
        double fst = area.getAverageFontStyle();
        double ind = getIndentation(node);
        double cen = node.isCentered() ? 1.0 : 0.0;
        double contrast = getContrast(node);
        double cp = 1.0 - ca.getColorPercentage(node);
        double bcp = bca.getColorPercentage(node);
        bcp = (bcp < 0.0) ? 0.0 : (1.0 - bcp);
        
        //weighting
        double exp = weights[WFSZ] * fsz 
                      + weights[WFWT] * fwt 
                      + weights[WFST] * fst 
                      + weights[WIND] * ind
                      + weights[WCON] * contrast
                      + weights[WCEN] * cen
                      + weights[WCP] * cp
                      + weights[WBCP] * bcp;
        
        return exp;
    }
    
    //========================================================================================================
    
    /**
     * Counts the number of sub-areas in the specified region of the area
     * @param a the area to be examined
     * @param r the grid region of the area to be examined
     * @return the number of visual areas in the specified area of the grid
     */
    private int countAreas(AreaNode a, Rectangular r)
    {
        int ret = 0;
        
        for (int i = 0; i < a.getChildCount(); i++)
        {
            AreaNode n = a.getChildArea(i);
            if (n.getGridPosition().intersects(r))
                ret++;
        }
        return ret;
    }
    
    private int countAreasAbove(AreaNode a)
    {
        Rectangular gp = a.getGridPosition();
        AreaNode parent = a.getParentArea();
        if (parent != null)
        {
            Rectangular r = new Rectangular(gp.getX1(), 0, gp.getX2(), gp.getY1() - 1);
            return countAreas(parent, r);
        }
        else
            return 0;
    }

    private int countAreasBelow(AreaNode a)
    {
        Rectangular gp = a.getGridPosition();
        AreaNode parent = a.getParentArea();
        if (parent != null)
        {
            Rectangular r = new Rectangular(gp.getX1(), gp.getY2()+1, gp.getX2(), Integer.MAX_VALUE);
            return countAreas(parent, r);
        }
        else
            return 0;
    }

    private int countAreasLeft(AreaNode a)
    {
        Rectangular gp = a.getGridPosition();
        AreaNode parent = a.getParentArea();
        if (parent != null)
        {
            Rectangular r = new Rectangular(0, gp.getY1(), gp.getX1() - 1, gp.getY2());
            return countAreas(parent, r);
        }
        else
            return 0;
    }

    private int countAreasRight(AreaNode a)
    {
        Rectangular gp = a.getGridPosition();
        AreaNode parent = a.getParentArea();
        if (parent != null)
        {
            Rectangular r = new Rectangular(gp.getX2()+1, gp.getY1(), Integer.MAX_VALUE, gp.getY2());
            return countAreas(parent, r);
        }
        else
            return 0;
    }

    private int countChars(String s, int type)
    {
        int ret = 0;
        for (int i = 0; i < s.length(); i++)
            if (Character.getType(s.charAt(i)) == type)
                    ret++;
        return ret;
    }

    private int countCharsPunct(String s)
    {
        int ret = 0;
        for (int i = 0; i < s.length(); i++)
        {
            char ch = s.charAt(i);
            if (ch == ',' || ch == '.' || ch == ';' || ch == ':')
                    ret++;
        }
        return ret;
    }
    
    private double getAverageTextLuminosity(AreaNode a)
    {
        double sum = 0;
        int cnt = 0;
        
        if (a.getArea().hasContent())
        {
            int l = a.getText().length();
            sum += a.getArea().getAverageColorLuminosity() * l;
            cnt += l;
        }
        
        for (int i = 0; i < a.getChildCount(); i++)
        {
            int l = a.getChildArea(i).getText().length();
            sum += getAverageTextLuminosity(a.getChildArea(i)) * l;
            cnt += l;
        }
        
        if (cnt > 0)
            return sum / cnt;
        else
            return 0;
    }
    
    private double getBackgroundLuminosity(AreaNode a)
    {
        Color bg = a.getEffectiveBackgroundColor();
        if (bg != null)
            return FeatureAnalyzer.colorLuminosity(bg);
        else
            return 0;
    }
    
    private double getContrast(AreaNode a)
    {
        double bb = getBackgroundLuminosity(a);
        double tb = getAverageTextLuminosity(a);
        double lum;
        if (bb > tb)
            lum = (bb + 0.05) / (tb + 0.05);
        else
            lum = (tb + 0.05) / (bb + 0.05);
        return lum;
    }
    
    public static double colorLuminosity(Color c)
    {
        double lr, lg, lb;
        if (c == null)
        {
            lr = lg = lb = 255;
        }
        else
        {
            lr = Math.pow(c.getRed() / 255.0, 2.2);
            lg = Math.pow(c.getGreen() / 255.0, 2.2);
            lb = Math.pow(c.getBlue() / 255.0, 2.2);
        }
        return lr * 0.2126 +  lg * 0.7152 + lb * 0.0722;
    }

    
}
