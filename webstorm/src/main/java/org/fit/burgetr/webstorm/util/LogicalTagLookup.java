/**
 * LogicalTagLookup.java
 *
 * Created on 24. 2. 2014, 11:38:19 by burgetr
 */
package org.fit.burgetr.webstorm.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.burgetr.segm.LogicalNode;
import org.burgetr.segm.LogicalTree;
import org.burgetr.segm.Segmentator;
import org.burgetr.segm.tagging.Tag;
import org.burgetr.segm.tagging.taggers.PersonsTagger;
import org.burgetr.segm.tagging.taggers.Tagger;
import org.xml.sax.SAXException;

/**
 * This class implemens a lookup for particular tags in the logical tree.
 * @author burgetr
 */
public class LogicalTagLookup
{
    protected LogicalTree ltree;
    
    
    public LogicalTagLookup(LogicalTree ltree)
    {
        this.ltree = ltree;
    }
    
    public Vector<LogicalNode> lookupTag(Tag tag)
    {
        Vector<LogicalNode> ret = new Vector<LogicalNode>();
        recursiveLookupTag(ltree.getRoot(), tag, ret);
        return ret;
    }

    protected void recursiveLookupTag(LogicalNode root, Tag tag, Vector<LogicalNode> result)
    {
        if (root.hasTag(tag))
            result.add(root);
        for (int i = 0; i < root.getChildCount(); i++)
            recursiveLookupTag(root.getChildNode(i), tag, result);
    }

    protected Vector<String> findRelatedText(LogicalNode node)
    {
        Vector<String> ret = new Vector<String>();
        LogicalNode pp = node.getParentNode();
        while (pp != null)
        {
            String pt = pp.getLeafText();
            if (pt != null && !pt.isEmpty())
                ret.add(pt);
            pp = pp.getParentNode();
        }
        return ret;
    }
    
    public Map<String, Vector<LogicalNode>> findRelatedNodes(Tagger tagger)
    {
        Map<String, Vector<LogicalNode>> ret = new HashMap<String, Vector<LogicalNode>>();
        
        Vector<LogicalNode> rel = lookupTag(tagger.getTag());
        for (LogicalNode node : rel)
        {
            String text = node.getText();
            Vector<String> allNames = tagger.extract(text);
            
            //extract surnames, unify
            Set<String> names = new HashSet<String>();
            for (String name : names)
            {
                String[] parts = name.toLowerCase().split("\\s+");
                if (parts.length > 0)
                    names.add(parts[parts.length - 1]); //take last names only
            }
            
            
            LogicalNode pp = node.getParentNode();
            if (pp != null)
            {
                System.out.print("    ");
                while (pp != null)
                {
                    String pt = pp.getLeafText();
                    if (pt != null && !pt.isEmpty())
                        System.out.print(" / " + pt);
                    pp = pp.getParentNode();
                }
                System.out.println();
            }
            
        }

        
        return ret;
    }
    
    //=====================================================================================================

    public static void main(String[] args)
    {
        try
        {
            URL url = new URL("http://edition.cnn.com/2014/02/24/world/europe/ukraine-protests-up-to-speed/index.html?hpt=hp_t1");
            //URL url = new URL("http://edition.cnn.com");
            Segmentator segm = new Segmentator();
            segm.segmentURL(url);
            
            Tagger p = new PersonsTagger(1);
            
            LogicalTagLookup lookup = new LogicalTagLookup(segm.getLogicalTree());
            Vector<LogicalNode> result = lookup.lookupTag(p.getTag());
            
            System.out.println("Found:");
            for (LogicalNode node : result)
            {
                String text = node.getText();
                Vector<String> names = p.extract(text);
                System.out.println(names.toString() + " : " + text);
                
                LogicalNode pp = node.getParentNode();
                if (pp != null)
                {
                    System.out.print("    ");
                    while (pp != null)
                    {
                        String pt = pp.getLeafText();
                        if (pt != null && !pt.isEmpty())
                            System.out.print(" / " + pt);
                        pp = pp.getParentNode();
                    }
                    System.out.println();
                }
                
            }
            
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
