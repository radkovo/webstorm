/**
 * LogicalTagLookup.java
 *
 * Created on 24. 2. 2014, 11:38:19 by burgetr
 */
package org.fit.burgetr.webstorm.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import org.burgetr.segm.AreaNode;
import org.burgetr.segm.BoxNode;
import org.burgetr.segm.LogicalNode;
import org.burgetr.segm.LogicalTree;
import org.burgetr.segm.Segmentator;
import org.burgetr.segm.tagging.Tag;
import org.burgetr.segm.tagging.taggers.PersonsTagger;
import org.burgetr.segm.tagging.taggers.Tagger;
import org.fit.cssbox.layout.ReplacedBox;
import org.fit.cssbox.layout.ReplacedContent;
import org.fit.cssbox.layout.ReplacedImage;
import org.xml.sax.SAXException;

/**
 * This class implemens a lookup for particular tags in the logical tree.
 * @author burgetr
 */
public class LogicalTagLookup
{
    protected Pattern wordExpr = Pattern.compile("[A-Za-z]+");
    protected LogicalTree ltree;
    
    /**
     * Creates a lookup on a logical tree. 
     * @param ltree the logical tree to be used.
     */
    public LogicalTagLookup(LogicalTree ltree)
    {
        this.ltree = ltree;
    }
    
    /**
     * Finds all logical nodes that correspond to the given tag.
     * @param tag The tag to be found
     * @return List of corresponding logical nodes
     */
    public List<LogicalNode> lookupTag(Tag tag)
    {
        Vector<LogicalNode> ret = new Vector<LogicalNode>();
        recursiveLookupTag(ltree.getRoot(), tag, ret);
        return ret;
    }

    /**
     * Recursively finds all logical nodes that correspond to the given tag starting with the given root node.
     * @param root The root node
     * @param tag The tag to be found
     * @param result List of corresponding logical nodes
     */
    protected void recursiveLookupTag(LogicalNode root, Tag tag, Vector<LogicalNode> result)
    {
        if (root.hasTag(tag))
            result.add(root);
        for (int i = 0; i < root.getChildCount(); i++)
            recursiveLookupTag(root.getChildNode(i), tag, result);
    }

    //=====================================================================================================
    // Keyword extraction
    //=====================================================================================================
    
    /**
     * Finds all the name - related string relationships in the document
     * @param tagger The name tagger to be used for recoginizing the names
     * @return A map assigning strings to surnames
     */
    public Map<String, List<String>> findRelatedText(Tagger tagger)
    {
        Map<String, List<String>> ret = new HashMap<String, List<String>>();
        
        List<LogicalNode> rel = lookupTag(tagger.getTag());
        for (LogicalNode node : rel)
        {
            String text = node.getText();
            Set<String> names = extractSurnames(text, tagger);
            List<String> related = findRelatedTextForNode(node);
            for (String name : names)
            {
                List<String> nameStrings = ret.get(name);
                if (nameStrings == null)
                    ret.put(name, related);
                else
                    nameStrings.addAll(related);
            }
        }
        
        return ret;
    }
    
    /**
     * Goes through the name - text string relationships and transforms them to name - keywords relationships
     * @param related 
     * @return
     */
    public Map<String, Set<String>> extractRelatedKeywords(Map<String, List<String>> related)
    {
        Map<String, Set<String>> ret = new HashMap<String, Set<String>>();

        for (Map.Entry<String, List<String>> entry : related.entrySet())
        {
            Set<String> nameWords = new HashSet<String>();
            for (String text : entry.getValue())
                nameWords.addAll(extractKeywords(text));
            ret.put(entry.getKey(), nameWords);
        }
        
        return ret;
    }
    
    /**
     * Extracts all last names from the given string using a tagger.
     * @param text The text string to be processed (containing the names).
     * @param tagger The tagger to be used for recognizing the names.
     * @return A set of surnames found in the text.
     */
    protected Set<String> extractSurnames(String text, Tagger tagger)
    {
        Vector<String> allNames = tagger.extract(text);
        
        //extract surnames, unify
        Set<String> names = new HashSet<String>();
        for (String name : allNames)
        {
            String[] parts = name.toLowerCase().split("\\s+");
            if (parts.length > 0)
                names.add(parts[parts.length - 1]); //take last names only
        }
        
        return names;
    }
    
    /**
     * Finds the related text string to the given logical node.
     * @param node
     * @return
     */
    protected List<String> findRelatedTextForNode(LogicalNode node)
    {
        Vector<String> ret = new Vector<String>();
        
        //Include the text of the node itself
        ret.add(node.getText());
        
        //Include all the paren nodes
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
    
    /**
     * Obtains all the valid keywords from a string. Removes the stop words and words containing strange characters.
     * @param text
     * @return
     */
    protected Set<String> extractKeywords(String text)
    {
        HashSet<String> ret = new HashSet<String>();
        
        String[] allWords = text.toLowerCase().split("\\s+");
        for (String word : allWords)
        {
            if (isValidWord(word) && !StopList.contains(word))
                ret.add(word);
        }
        
        return ret;
    }
    
    protected boolean isValidWord(String s)
    {
        return s.length() > 1 && wordExpr.matcher(s).matches();
    }
    
    //=====================================================================================================
    // Image extraction
    //=====================================================================================================
    
    /**
     * Finds an expected "container area" containing a logical node and all related images 
     * @param node The logical node to start with
     * @param expansionThreshold the maximal expansion that a parent may add to the right border
     * @return the container area
     */
    protected AreaNode findContainerArea(LogicalNode node, int tx1, int tx2)
    {
        AreaNode ret = node.getFirstAreaNode();
        if (ret.getParentArea() != null)
        {
            ret = ret.getParentArea();
            //expand the area as long as it keeps the right edge (with certain threshold)
            while (ret.getParentArea() != null)
            {
                AreaNode parent = ret.getParentArea();
                if (parent.getX() >= ret.getX() - tx1
                    && parent.getX2() <= ret.getX2() + tx2)
                    ret = parent;
                else
                    break;
            }
        }
        return ret;
    }
    
    protected Set<URL> findImageUrls(AreaNode node)
    {
        Set<URL> ret = new HashSet<URL>();
        recursiveFindUrls(node, ret);
        return ret;
    }
    
    private void recursiveFindUrls(AreaNode root, Set<URL> urls)
    {
        if (root.isLeaf())
        {
            for (BoxNode node : root.getArea().getBoxes())
            {
                if (node.getBox() instanceof ReplacedBox)
                {
                    ReplacedContent content = ((ReplacedBox) node.getBox()).getContentObj();
                    if (content instanceof ReplacedImage)
                    {
                        urls.add(((ReplacedImage) content).getUrl());
                    }
                }
            }
        }
        else
        {
            for (int i = 0; i < root.getChildCount(); i++)
                recursiveFindUrls(root.getChildArea(i), urls);
        }       
    }
    
    public Map<String, Set<URL>> extractRelatedImages(Tagger tagger)
    {
        Map<String, Set<URL>> ret = new HashMap<String, Set<URL>>();
        List<LogicalNode> nameNodes = lookupTag(tagger.getTag());
        
        for (LogicalNode node : nameNodes)
        {
            Set<String> names = extractSurnames(node.getText(), tagger);
            AreaNode container = findContainerArea(node, 100, 20);
            Set<URL> urls = findImageUrls(container);
            
            for (String name : names)
            {
                Set<URL> nameset = ret.get(name);
                if (nameset == null)
                {
                    nameset = new HashSet<URL>();
                    ret.put(name, nameset);
                }
                nameset.addAll(urls);
            }
        }
        
        return ret;
    }
    
    //=====================================================================================================
    
    
    public static void main(String[] args)
    {
        test4(args);
    }
    
    public static void test4(String[] args)
    {
        try
        {
            URL url = new URL("http://edition.cnn.com/2014/02/24/world/europe/ukraine-protests-up-to-speed/index.html?hpt=hp_t1");
            //URL url = new URL("http://edition.cnn.com");
            Segmentator segm = new Segmentator();
            segm.segmentURL(url);
            
            Tagger p = new PersonsTagger(1);
            
            LogicalTagLookup lookup = new LogicalTagLookup(segm.getLogicalTree());
            Map<String, Set<URL>> urls = lookup.extractRelatedImages(p);
            
            System.out.println(urls);
            
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
    
    public static void test3(String[] args)
    {
        try
        {
            URL url = new URL("http://edition.cnn.com/2014/02/24/world/europe/ukraine-protests-up-to-speed/index.html?hpt=hp_t1");
            //URL url = new URL("http://edition.cnn.com");
            Segmentator segm = new Segmentator();
            segm.segmentURL(url);
            
            Tagger p = new PersonsTagger(1);
            
            LogicalTagLookup lookup = new LogicalTagLookup(segm.getLogicalTree());
            List<LogicalNode> nameNodes = lookup.lookupTag(p.getTag());
            
            for (LogicalNode node : nameNodes)
            {
                System.out.println("Node:" + node);
                Set<String> names = lookup.extractSurnames(node.getText(), p);
                System.out.println("    Names:" + names);
                AreaNode container = lookup.findContainerArea(node, 100, 20);
                System.out.println("    Container:" + container);
                Set<URL> urls = lookup.findImageUrls(container);
                System.out.println("    Urls:" + urls);
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
    
    public static void test2(String[] args)
    {
        try
        {
            URL url = new URL("http://edition.cnn.com/2014/02/24/world/europe/ukraine-protests-up-to-speed/index.html?hpt=hp_t1");
            //URL url = new URL("http://edition.cnn.com");
            Segmentator segm = new Segmentator();
            segm.segmentURL(url);
            
            Tagger p = new PersonsTagger(1);
            
            LogicalTagLookup lookup = new LogicalTagLookup(segm.getLogicalTree());
            
            Map<String, List<String>> related = lookup.findRelatedText(p);
            System.out.println("Related text:");
            for (Map.Entry<String, List<String>> entry : related.entrySet())
            {
                System.out.println(entry.getKey() + " : " + entry.getValue());
            }
            
            Map<String, Set<String>> keywords = lookup.extractRelatedKeywords(related);
            System.out.println("Related keywords:");
            for (Map.Entry<String, Set<String>> entry : keywords.entrySet())
            {
                System.out.println(entry.getKey() + " : " + entry.getValue());
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
    
    public static void test1(String[] args)
    {
        try
        {
            URL url = new URL("http://edition.cnn.com/2014/02/24/world/europe/ukraine-protests-up-to-speed/index.html?hpt=hp_t1");
            //URL url = new URL("http://edition.cnn.com");
            Segmentator segm = new Segmentator();
            segm.segmentURL(url);
            
            Tagger p = new PersonsTagger(1);
            
            LogicalTagLookup lookup = new LogicalTagLookup(segm.getLogicalTree());
            List<LogicalNode> result = lookup.lookupTag(p.getTag());
            
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
