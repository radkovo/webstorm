/**
 * Segmentator.java
 *
 * Created on 8.11.2007, 13:47:44 by burgetr
 */
package org.burgetr.segm;

import java.net.*;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.*;

import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.exceptions.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.fit.cssbox.css.*;
import org.fit.cssbox.io.DOMSource;
import org.fit.cssbox.io.DefaultDOMSource;
import org.fit.cssbox.io.DefaultDocumentSource;
import org.fit.cssbox.io.DocumentSource;
import org.fit.cssbox.layout.BrowserCanvas;
import org.fit.cssbox.pdf.PdfBrowserCanvas;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author burgetr
 *
 */
public class Segmentator
{
    private static BufferedImage tmpImg = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB); //small temporary image used for rendering

    protected boolean xmlHeader = true; /* whether to display XML header */
    protected BrowserCanvas contentCanvas;
    protected Processor proc;
    private double[] weights;
  
    
    public Segmentator()
    {
        weights = FeatureAnalyzer.DEFAULT_WEIGHTS;
    }
    
    public void setWeights(double[] weights)
    {
        this.weights = weights;
    }
    
    public double[] getWeights()
    {
        return weights;
    }
    
    public String getWeightString()
    {
        String ret = "{";
        for (double w : weights)
        {
            ret += w + " ";
        }
        ret += "}";
        return ret;
    }
    
    public BoxTree getBoxTree()
    {
        return proc.getBoxTree();
    }
    
    public AreaTree getAreaTree()
    {
    	return proc.getAreaTree();
    }
    
    public LogicalTree getLogicalTree()
    {
    	return proc.getLogicalTree();
    }
    
    public void setXmlHeader(boolean b)
    {
        xmlHeader = b;
    }
    
    public void loadUrl(URL url) throws IOException, SAXException
    {
        DocumentSource src = new DefaultDocumentSource(url);
        contentCanvas = createContentCanvas(src, new java.awt.Dimension(1200, 600));
    }
    
    public void createTrees()
    {
        proc = new Processor();
        proc.setWeights(weights);
        proc.segmentPage(contentCanvas.getViewport());
    }
    
    public void segmentURL(URL url) throws IOException, SAXException
    {
        loadUrl(url);
        createTrees();
    }

    /** Creates the appropriate canvas based on the file type */
    protected BrowserCanvas createContentCanvas(DocumentSource src, Dimension dim) throws IOException, SAXException
    {
        InputStream is = src.getInputStream();
        String mime = src.getContentType();
        if (mime == null)
            mime = "text/html";
        int p = mime.indexOf(';');
        if (p != -1)
            mime = mime.substring(0, p).trim();
        
        if (mime.equals("application/pdf"))
        {
            PDDocument doc = loadPdf(is);
            PdfBrowserCanvas ret = new PdfBrowserCanvas(doc, 0, Integer.MAX_VALUE, null, src.getURL());
            ret.setImage(tmpImg);
            ret.getConfig().setLoadImages(false);
            ret.getConfig().setLoadBackgroundImages(false);
            ret.createLayout(dim);
            return ret;
        }
        else
        {
            DOMSource parser = new DefaultDOMSource(src);
            Document doc = parser.parse();
            String encoding = parser.getCharset();
            
            DOMAnalyzer da = new DOMAnalyzer(doc, src.getURL());
            if (encoding == null)
                encoding = da.getCharacterEncoding();
            da.setDefaultEncoding(encoding);
            da.attributesToStyles();
            da.addStyleSheet(null, CSSNorm.stdStyleSheet(), DOMAnalyzer.Origin.AGENT);
            da.addStyleSheet(null, CSSNorm.userStyleSheet(), DOMAnalyzer.Origin.AGENT);
            da.getStyleSheets();
            
            BrowserCanvas ret = new BrowserCanvas(da.getRoot(), da, src.getURL());
            ret.setImage(tmpImg);
            ret.getConfig().setLoadImages(false);
            ret.getConfig().setLoadBackgroundImages(false);
            ret.createLayout(dim);
            return ret;
        }
    }
    
    private PDDocument loadPdf(InputStream is) throws IOException
    {
        PDDocument document = null;
        document = PDDocument.load(is);
        if( document.isEncrypted() )
        {
            try
            {
                document.decrypt("");
            }
            catch( InvalidPasswordException e )
            {
                System.err.println( "Error: Document is encrypted with a password." );
                System.exit( 1 );
            }
            catch( CryptographyException e )
            {
                System.err.println( "Error: Document is encrypted with a password." );
                System.exit( 1 );
            }
        }
        return document;
    }
    
    
    public boolean outputAreaTree(String urlstring, Writer out)
    {
        try {
            URL url = createURL(urlstring);
            segmentURL(url);
            
            PrintWriter xs = new PrintWriter(out);
            XMLOutput xo = new XMLOutput(proc.getAreaTree(), url, xmlHeader);
            xo.dumpTo(xs);
            
            return true;            
        } catch (Exception e) {
            System.err.println("*** Error: "+e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean outputLogicalTree(String urlstring, Writer out)
    {
        try {
            URL url = createURL(urlstring);
            segmentURL(url);
            
            PrintWriter xs = new PrintWriter(out);
            XMLLogicalOutput xo = new XMLLogicalOutput(proc.getLogicalTree(), url, xmlHeader);
            xo.dumpTo(xs);
            
            return true;            
        } catch (Exception e) {
            System.err.println("*** Error: "+e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    protected URL createURL(String urlstring) throws MalformedURLException
    {
        if (!urlstring.startsWith("http:") &&
                !urlstring.startsWith("ftp:") &&
                !urlstring.startsWith("file:"))
                    urlstring = "http://" + urlstring;
        
        return new URL(urlstring);
    }
    
    public static void main(String[] args)
    {
        if (args.length != 2)
        {
            System.out.println("Usage: Segmentator <url> <outfile>");
        }
        else
        {
            String url = args[0];
            if (url != null && url.length() > 0)
            {
                try {
                    PrintWriter xs = new PrintWriter(new FileOutputStream(args[1]));
                    Segmentator trans = new Segmentator();
                    trans.outputAreaTree(url, xs);
                    xs.close();
                } catch (IOException e) {
                    System.err.println("I/O exception: " + e.getMessage());
                }
            }        
        }
    }
    
}
