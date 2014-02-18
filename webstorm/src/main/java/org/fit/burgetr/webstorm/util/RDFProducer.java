/**
 * RDFProducer.java
 *
 * Created on 12. 2. 2014, 13:28:58 by burgetr
 */
package org.fit.burgetr.webstorm.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import cz.vutbr.web.css.CSSProperty.TextDecoration;

import org.burgetr.segm.Area;
import org.burgetr.segm.AreaNode;
import org.burgetr.segm.AreaTree;
import org.burgetr.segm.BoxNode;
import org.burgetr.segm.BoxTree;
import org.burgetr.segm.Rectangular;
import org.burgetr.segm.Segmentator;
import org.fit.burgetr.webstorm.ontology.Render;
import org.fit.burgetr.webstorm.ontology.Segm;
import org.fit.cssbox.layout.Box;
import org.xml.sax.SAXException;

/**
 * 
 * @author burgetr
 */
public class RDFProducer
{
    private BoxTree btree;
    private AreaTree atree;
    private URL baseUrl;
    private Model model;
    
    public RDFProducer(BoxTree btree, AreaTree atree, URL url)
    {
        this.btree = btree;
        this.atree = atree;
        try
        {
            this.baseUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile());
        } catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
    }
    
    public void dumpTo(PrintWriter out)
    {
        getModel().write(out);
        //model.write(out, "RDF/XML-ABBREV");
    }
    
    public Model getModel()
    {
        if (model == null)
        {
            createModel();
            model.setNsPrefix("b", Render.NS);
            model.setNsPrefix("a", Segm.NS);
        }
        return model;
    }
    
    //=====================================================================================================
    
    private void createModel()
    {
        model = ModelFactory.createDefaultModel();
        Resource page = dumpPage();
        dumpBoxes(btree.getRoot(), page, null);
        dumpAreas(atree.getRoot(), page, null);
    }
    
    //=====================================================================================================
    
    private Resource dumpPage()
    {
        Resource page = model.createResource(documentId()).addProperty(RDF.type, Render.Page);
        page.addLiteral(Render.width, btree.getRootBox().getWidth());
        page.addLiteral(Render.height, btree.getRootBox().getHeight());
        page.addLiteral(Render.sourceUrl, baseUrl.toString());
        return page;
    }
    
    private Resource dumpBoxes(BoxNode boxnode, Resource page, Resource parent)
    {
        Box box = boxnode.getBox();
        Rectangular pos = boxnode.getVisualBounds();
        Resource ret = model.createResource(boxId(box)).addProperty(RDF.type, Render.Box);
        
        if (page != null)
            ret.addProperty(Render.belongsTo, page);
        if (parent != null)
            ret.addProperty(Render.isChildOf, parent);
        
        ret.addLiteral(Render.positionX, pos.getX1()); 
        ret.addLiteral(Render.positionY, pos.getY1()); 
        ret.addLiteral(Render.width, pos.getWidth()); 
        ret.addLiteral(Render.height, pos.getHeight());
        ret.addLiteral(Render.color, colorString(box.getVisualContext().getColor()));
        ret.addLiteral(Render.backgroundColor, colorString(boxnode.getBgcolor()));
        ret.addLiteral(Render.fontFamily, box.getVisualContext().getFont().getName());
        ret.addLiteral(Render.fontSize, box.getVisualContext().getFont().getSize());
        ret.addLiteral(Render.fontWeight, (box.getVisualContext().getFont().isBold()?100:0));
        ret.addLiteral(Render.fontStyle, (box.getVisualContext().getFont().isItalic()?100:0));
        ret.addLiteral(Render.fontVariant, box.getVisualContext().getFontVariant());
        ret.addLiteral(Render.fontDecoration, decorationString(box.getVisualContext().getTextDecoration()));

        //TODO borders a obsah
        
        if (!boxnode.isLeaf())
        {
            for (int i = 0; i < boxnode.getChildCount(); i++)
                dumpBoxes(boxnode.getChildBox(i), page, ret);
        }
        
        return ret;
    }
    
    private Resource dumpAreas(AreaNode n, Resource page, Resource parent)
    {
        Area a = n.getArea();
        //Rectangular gp = n.getGridPosition();
        //String attrs = "rdf:about=\"" + areaId(a) + "\"";
                /*+ " gx1=\"" + gp.getX1() + "\"" 
                + " gy1=\"" + gp.getY1() + "\"" 
                + " gx2=\"" + gp.getX2() + "\"" 
                + " gy2=\"" + gp.getY2() + "\"" 
                + " gridw=\"" + n.getGrid().getWidth() + "\"" 
                + " gridh=\"" + n.getGrid().getHeight() + "\""*/ 

        Resource ret = model.createResource(areaId(a)).addProperty(RDF.type, Segm.Area);
        
        if (page != null)
            ret.addProperty(Render.belongsTo, page);
        if (parent != null)
            ret.addProperty(Segm.isChildOf, parent);
        
        ret.addLiteral(Render.positionX, a.getX1());
        ret.addLiteral(Render.positionY, a.getY1());
        ret.addLiteral(Render.width, a.getWidth());
        ret.addLiteral(Render.height, a.getHeight());
        ret.addLiteral(Segm.backgroundColor, colorString(a.getBackgroundColor())); 
        ret.addLiteral(Segm.fontSize, (float) a.getAverageFontSize());
        ret.addLiteral(Segm.fontWeight, (float) a.getAverageFontWeight()); 
        ret.addLiteral(Segm.fontStyle, (float) a.getAverageFontStyle()); 
        
        if (!n.isLeaf())
        {
            for (int i = 0; i < n.getChildCount(); i++)
                dumpAreas(n.getChildArea(i), page, ret);
        }
        
        return ret;
    }
    
    //=====================================================================================================
    
    private String documentId()
    {
        return baseUrl + "#doc";
    }
    
    private String boxId(Box box)
    {
        return baseUrl + "#b" + box.getOrder() + "_" + box.getSplitId();
    }
    
    private String areaId(Area area)
    {
        return baseUrl + "#a" + area.getId();
    }
    
    //=====================================================================================================
    
    private String colorString(java.awt.Color color)
    {
        if (color == null)
            return "";
        else
            return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private String decorationString(List<TextDecoration> decorations)
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
    
    //=====================================================================================================
    
    public static void main(String[] args)
    {
        try
        {
            URL url = new URL("file:/home/burgetr/git/Layout/test/programmes/ehealth07.html");
            Segmentator segm = new Segmentator();
            segm.segmentURL(url);
            
            RDFProducer rdf = new RDFProducer(segm.getBoxTree(), segm.getAreaTree(), url);
            rdf.dumpTo(new PrintWriter(System.out));
        } catch (MalformedURLException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        } catch (SAXException e)
        {
            e.printStackTrace();
        }
    }
    
}
