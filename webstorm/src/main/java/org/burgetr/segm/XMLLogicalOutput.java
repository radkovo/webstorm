/**
 * XMLLogicalOutput.java
 *
 * Created on 1.9.2011, 14:27:26 by burgetr
 */
package org.burgetr.segm;

import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.burgetr.segm.tagging.Tag;

/**
 * @author burgetr
 *
 */
public class XMLLogicalOutput
{
    private LogicalTree tree;
    private URL url;
    private boolean header;
    private int idcnt = 0;

    //=====================================================================
    
    public XMLLogicalOutput(LogicalTree tree, URL url)
    {
        this.tree = tree;
        this.url = url;
        this.header = true;
    }
    
    public XMLLogicalOutput(LogicalTree tree, URL url, boolean header)
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
        {
            out.println("<?xml version=\"1.0\"?>");
            out.println("<?xml-stylesheet type=\"text/css\" href=\"file:///home/burgetr/workspace/Layout/misc/logicaltree.css\"?>");
        }
        out.println("<logicalTree base=\"" + HTMLEntities(url.toString()) + "\">");
        recursiveDump(tree.getRoot(), 1, false, out);
        out.println("</logicalTree>");
    }
    
    //=====================================================================
    
    @SuppressWarnings("rawtypes")
    private void recursiveDump(LogicalNode ln, int level, boolean lines, java.io.PrintWriter p)
    {
        //AreaNode n = ln.getAreaNode();
        
        String attrs = "id=\"x" + (idcnt++) + "\"";
        String cls = "";
        Set<Tag> tags = ln.getFirstAreaNode().getTags();
        if (!tags.isEmpty())
        {
            String tagstring = "";
            for (Tag tag : tags)
                tagstring += tag.toString() + ' ';
            cls = " class=\"" + tagstring.trim() + "\" title=\"Tags: " + tagstring.trim() + "\"";
        }
        
        if (!lines)
        {
            indent(level, p);
            startTag("area", attrs, p);
            
            //title (if any)
            if (ln.getFirstAreaNode().isLeaf())
            {
                indent(level+1, p);
                startTag("title", cls, p);
                p.print(HTMLEntities(ln.getText()));
                endTag("title", p);
            }
            
            boolean generateLines = containsLeafsOnly(ln);
            for (Enumeration e = ln.children(); e.hasMoreElements(); )
            {
                recursiveDump((LogicalNode) e.nextElement(), level+1, generateLines, p);
            }
            
            indent(level, p);
            endTag("area", p);
        }
        else
        {
            indent(level, p);
            startTag("line", attrs + cls, p);
            dumpBoxes(ln, p, level+1);
            indent(level, p);
            endTag("line", p);
        }
        
    }
    
    private void dumpBoxes(LogicalNode ln, PrintWriter p, int level)
    {
    	for (AreaNode node : ln.getAreaNodes())
    	{
	        Vector<BoxNode> boxes = node.getArea().getBoxes();
	        for (Iterator<BoxNode> it = boxes.iterator(); it.hasNext(); )
	        {
	            BoxNode boxnode = it.next();
	            /*Box box = boxnode.getBox();
	            Rectangular pos = boxnode.getVisualBounds();
	            String attrs = "x1=\"" + pos.getX1() + "\"" 
	                            + " y1=\"" + pos.getY1() + "\"" 
	                            + " x2=\"" + pos.getX2() + "\"" 
	                            + " y2=\"" + pos.getY2() + "\""
	                            + " color=\"" + colorString(box.getVisualContext().getColor()) + "\""
	                            + " fontfamily=\"" + box.getVisualContext().getFont().getName() + "\""
	                            + " fontsize=\"" + box.getVisualContext().getFont().getSize() + "\""
	                            + " fontweight=\"" + (box.getVisualContext().getFont().isBold()?100:0) + "\""
	                            + " fontstyle=\"" + (box.getVisualContext().getFont().isItalic()?100:0) + "\""
	                            + " fontvariant=\"" + box.getVisualContext().getFontVariant() + "\""
	                            + " decoration=\"" + box.getVisualContext().getTextDecoration() + "\""
	                            + " replaced=\"" + (box.isReplaced()?"true":"false") + "\""
	                            + ">";*/
	            String attrs = "";
	            indent(level, p);
	            startTag("text", attrs, p);
	            p.print(HTMLEntities(boxnode.getText()));
	            endTag("text", p);
	        }
    	}
    }
    
    private void indent(int level, PrintWriter p)
    {
        String ind = "";
        for (int i = 0; i < level*4; i++) ind = ind + ' ';
        p.println();
        p.print(ind);
    }
    
    private void startTag(String name, String attrs, PrintWriter p)
    {
        StringBuilder out = new StringBuilder("<");
        out.append(name);
        if (attrs.length() > 0)
        {
            out.append(' ');
            out.append(attrs);
        }
        out.append('>');
        p.write(out.toString());
    }
    
    private void endTag(String name, PrintWriter p)
    {
        StringBuilder out = new StringBuilder("</");
        out.append(name);
        out.append('>');
        p.write(out.toString());
    }
    
    /*private String colorString(java.awt.Color color)
    {
        if (color == null)
            return "";
        else
            return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }*/
    
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
    
    private String HTMLEntities(String s)
    {
        return s.replaceAll(">", "&gt;").replaceAll("<", "&lt;").replaceAll("&", "&amp;");
    }

    private boolean containsLeafsOnly(LogicalNode ln)
    {
        return ln.getDepth() <= 1;
    }
    
}
