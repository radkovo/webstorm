/**
 * XMLOutput.java
 *
 * Created on 3.1.2007, 19:23:21 by radek
 */
package org.burgetr.segm;

import java.net.URL;
import java.util.*;

import cz.vutbr.web.css.CSSProperty.TextDecoration;

import org.fit.cssbox.layout.Box;

/**
 * Produces a XML representation of the AreaTree
 * 
 * @author radek
 */
public class XMLOutput
{
	private AreaTree tree;
    private URL url;
    private boolean header;
    private int idcnt = 0;

	//=====================================================================
	
	public XMLOutput(AreaTree tree, URL url)
	{
		this.tree = tree;
        this.url = url;
        this.header = true;
	}
	
    public XMLOutput(AreaTree tree, URL url, boolean header)
    {
        this.tree = tree;
        this.url = url;
        this.header = header;
    }
	
    /**
     * Formats the complete tag tree to an output stream
     */
    public void dumpTo(java.io.PrintWriter out)
    {
        if (header)
            out.println("<?xml version=\"1.0\"?>");
        out.println("<areaTree base=\"" + HTMLEntities(url.toString()) + "\">");
        recursiveDump(tree.getRoot(), 1, out);
        out.println("</areaTree>");
    }
	
	//=====================================================================
	
	@SuppressWarnings("rawtypes")
    private void recursiveDump(AreaNode n, int level, java.io.PrintWriter p)
    {
    	Area a = n.getArea();
    	Rectangular gp = n.getGridPosition();
    	
    	String stag = "<area"
    	                + " id=\"x" + (idcnt++) + "\""
    					+ " x1=\"" + a.getX1() + "\"" 
    					+ " y1=\"" + a.getY1() + "\"" 
    					+ " x2=\"" + a.getX2() + "\"" 
    					+ " y2=\"" + a.getY2() + "\"" 
    					+ " gx1=\"" + gp.getX1() + "\"" 
    					+ " gy1=\"" + gp.getY1() + "\"" 
    					+ " gx2=\"" + gp.getX2() + "\"" 
    					+ " gy2=\"" + gp.getY2() + "\"" 
    					+ " gridw=\"" + n.getGrid().getWidth() + "\"" 
    					+ " gridh=\"" + n.getGrid().getHeight() + "\"" 
                        + " background=\"" + colorString(a.getBackgroundColor()) + "\"" 
                        + " fontsize=\"" + a.getAverageFontSize() + "\"" 
                        + " fontweight=\"" + a.getAverageFontWeight() + "\"" 
                        + " fontstyle=\"" + a.getAverageFontStyle() + "\"" 
    					+ ">";

    	String etag = "</area>";
    	
    	if (n.getChildCount() > 0)
    	{
    		indent(level, p);
    		p.println(stag);
    		
    		for (Enumeration e = n.children(); e.hasMoreElements(); )
    			recursiveDump((AreaNode) e.nextElement(), level+1, p);
    		
	    	indent(level, p);
	    	p.println(etag);
    	}
    	else
    	{
    		indent(level, p);
    		p.println(stag);
            dumpBoxes(a, p, level+1);
            indent(level, p);
    		p.println(etag);
    	}
    	
    }
    
    private void dumpBoxes(Area a, java.io.PrintWriter p, int level)
    {
        Vector<BoxNode> boxes = a.getBoxes();
        for (Iterator<BoxNode> it = boxes.iterator(); it.hasNext(); )
        {
            BoxNode boxnode = it.next();
            Box box = boxnode.getBox();
            Rectangular pos = boxnode.getVisualBounds();
            indent(level, p);
            String stag = "<box"
                            + " x1=\"" + pos.getX1() + "\"" 
                            + " y1=\"" + pos.getY1() + "\"" 
                            + " x2=\"" + pos.getX2() + "\"" 
                            + " y2=\"" + pos.getY2() + "\""
                            + " color=\"" + colorString(box.getVisualContext().getColor()) + "\""
                            + " fontfamily=\"" + box.getVisualContext().getFont().getName() + "\""
                            + " fontsize=\"" + box.getVisualContext().getFont().getSize() + "\""
                            + " fontweight=\"" + (box.getVisualContext().getFont().isBold()?100:0) + "\""
                            + " fontstyle=\"" + (box.getVisualContext().getFont().isItalic()?100:0) + "\""
                            + " fontvariant=\"" + box.getVisualContext().getFontVariant() + "\""
                            + " decoration=\"" + decorationString(box.getVisualContext().getTextDecoration()) + "\""
                            + " replaced=\"" + (box.isReplaced()?"true":"false") + "\""
                            + ">";
            p.print(stag);
            p.print(HTMLEntities(boxnode.getText()));
            p.println("</box>");
        }
    }
    
    private void indent(int level, java.io.PrintWriter p)
    {
        String ind = "";
        for (int i = 0; i < level*4; i++) ind = ind + ' ';
        p.print(ind);
    }
    
    private String colorString(java.awt.Color color)
    {
        if (color == null)
            return "";
        else
            return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
    
    /**
     * Converts the CSS specification rgb(r,g,b) to #rrggbb
     * @param spec the CSS color specification
     * @return a #rrggbb string
     */
    public String colorString(String spec)
    {
        if (spec.startsWith("rgb("))
        {
            String s = spec.substring(4, spec.length() - 1);
            String[] lst = s.split(",");
            try {
                int r = Integer.parseInt(lst[0].trim());
                int g = Integer.parseInt(lst[1].trim());
                int b = Integer.parseInt(lst[2].trim());
                return String.format("#%02x%02x%02x", r, g, b);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        else
            return spec;
    }
    
    public String decorationString(List<TextDecoration> decorations)
    {
        if (decorations.isEmpty())
            return "none";
        else
        {
            boolean first = true;
            StringBuilder ret = new StringBuilder();
            for (TextDecoration dec : decorations)
            {
                if (!first) ret.append(' ');
                ret.append(dec.toString());
                first = false;
            }
            return ret.toString();
        }
    }
    
    private String HTMLEntities(String s)
    {
        return s.replaceAll(">", "&gt;").replaceAll("<", "&lt;").replaceAll("&", "&amp;");
    }
    
}
