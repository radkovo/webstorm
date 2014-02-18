/**
 * HTMLOuput.java
 *
 * Created on 7.8.2008, 11:03:02 by burgetr
 */
package org.burgetr.segm;

import java.net.URL;
import java.util.*;

import org.fit.cssbox.layout.Box;


/**
  * Produces an HTML representation of the AreaTree
  * 
  * @author burgetr
  */
public class HTMLOutput
{

    private AreaTree tree;
    private URL url;
    private int idcnt = 0;

    //=====================================================================
    
    public HTMLOutput(AreaTree tree, URL url)
    {
        this.tree = tree;
        this.url = url;
    }
    
    /**
     * Formats the complete tag tree to an output stream
     */
    public void dumpTo(java.io.PrintWriter out)
    {
        out.println("<html>");
        out.println("<head><title>Restructured Document</title></head>");
        out.println("<style type=\"text/css\">");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        recursiveDump(tree.getRoot(), 1, out);
        out.println("</body>");
        out.println("</html>");
    }
    
    //=====================================================================
    
    @SuppressWarnings("unchecked")
    private void recursiveDump(AreaNode n, int level, java.io.PrintWriter p)
    {
        Area a = n.getArea();
        
        if (n.getChildCount() > 0)
        {
            for (Enumeration e = n.children(); e.hasMoreElements(); )
                recursiveDump((AreaNode) e.nextElement(), level, p);
        }
        else
        {
            indent(level, p);
            p.println("<div class=\"area\">");
            dumpBoxes(a, p, level+1);
            indent(level, p);
            p.println("<div>");
        }
        
    }
    
    private void dumpBoxes(Area a, java.io.PrintWriter p, int level)
    {
        Vector<BoxNode> boxes = a.getBoxes();
        for (Iterator<BoxNode> it = boxes.iterator(); it.hasNext(); )
        {
            BoxNode boxnode = it.next();
            Box box = boxnode.getBox();
            indent(level, p);
            String style = "color:" + colorString(box.getVisualContext().getColor()) + ";"
                            + "font-family:" + box.getVisualContext().getFont().getName() + ";"
                            + "font-size:" + box.getVisualContext().getFont().getSize() + "pt;"
                            + "font-weight:" + (box.getVisualContext().getFont().isBold()?"bold":"normal") + ";"
                            + "font-style:" + (box.getVisualContext().getFont().isItalic()?"italic":"normal") + ";"
                            + "font-variant:" + box.getVisualContext().getFontVariant() + ";"
                            + "text-decoration:" + box.getVisualContext().getTextDecoration() + ";";
            
            p.print("<div class=\"box\" style=\"" + style + "\">");
            p.print(HTMLEntities(boxnode.getText()));
            p.println("</div>");
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
    
    private String HTMLEntities(String s)
    {
        return s.replaceAll(">", "&gt;").replaceAll("<", "&lt;").replaceAll("&", "&amp;");
    }
    
    
}
