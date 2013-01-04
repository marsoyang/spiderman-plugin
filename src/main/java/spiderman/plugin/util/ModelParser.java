package spiderman.plugin.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import net.sf.saxon.xpath.XPathFactoryImpl;

import org.eweb4j.spiderman.fetcher.Page;
import org.eweb4j.spiderman.spider.SpiderListener;
import org.eweb4j.spiderman.xml.Field;
import org.eweb4j.spiderman.xml.Target;
import org.eweb4j.util.CommonUtil;
import org.eweb4j.util.FileUtil;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.helpers.DefaultHandler;

public class ModelParser extends DefaultHandler{

	private Target target = null;
	private SpiderListener listener = null;
	public ModelParser(){}
	public ModelParser(Target target, SpiderListener listener) {
		this.target = target;
		this.listener = listener;
	}
	
	public static void main(String[] args) throws Exception{
		File file = new File("d:\\xml.xml");
		String xml = FileUtil.readFile(file);
		
//		System.setProperty("javax.xml.xpath.XPathFactory:"+NamespaceConstant.OBJECT_MODEL_SAXON, "net.sf.saxon.xpath.XPathFactoryImpl");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        XPathFactory xfactory = XPathFactoryImpl.newInstance();
        XPath xpath = xfactory.newXPath();
        XPathExpression expr = xpath.compile("//node");
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        int count = 0;
        String regex = "\\w+\\.(gif|png|jpg|jpeg|bmp)";
        for (int i = 0; i < nodes.getLength(); i++) {
            NodeList subs = (NodeList)xpath.compile("*[matches(text(),'"+regex+"')]/text()").evaluate(nodes.item(i), XPathConstants.NODESET);
            if (subs == null || subs.getLength() == 0)
            	continue;
            for (int j = 0; j < subs.getLength(); j++) {
            	Node item = subs.item(j);
            	String value = item.getNodeValue();
            	List<String> imgs = CommonUtil.findByRegex(value, "[^\\s'=\"]+\\.(gif|png|jpg|jpeg|bmp)(?=[\"']?)");
            	System.out.println(item.getParentNode().getNodeName()+"->"+imgs);
            	count++;
            }
        }
        System.out.println("count->"+count);
		
//		//第一步：获得解析工厂的实例  
//        SAXParserFactory spf = SAXParserFactory.newInstance();  
//        //第二部：获得工厂解析器  
//        SAXParser sp = spf.newSAXParser();  
//        //第三部：对xml进行解析  
//        sp.parse(file, new ModelParser());
        
	}
	
	public List<Map<String, Object>> parse(Page page) throws Exception{
		listener.onInfo(Thread.currentThread(), "parse Page->[cType:" + page.getContentType()+",charset:"+page.getCharset()+",encoding:"+page.getEncoding()+",url->"+page.getUrl());
		String contentType = page.getContentType();
		if (contentType == null)
			contentType = "text/html";
		boolean isXml = contentType.contains("text/xml") || contentType.contains("application/rss+xml") || contentType.contains("application/xml");
		//解析xml
		if (isXml) {
//			InputStream in = new ByteArrayInputStream(page.getContentData());
//	        SAXParserFactory spf = SAXParserFactory.newInstance();  
//	        SAXParser sp = spf.newSAXParser();
//	        XmlParseHandler handler = new XmlParseHandler();
//	        sp.parse(in, handler);
//	        
//	        return handler.getMap();
			return parseXml(page.getContentData());
		}
		
		// TODO 解析 JSON
		
		//解析html
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		list.add(parseHtml(page.getContent()));
		return list;
	}

	private List<Map<String, Object>> parseXml(byte[] xml) throws Exception{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml));
        XPathFactory xfactory = XPathFactoryImpl.newInstance();
        XPath xpathParser = xfactory.newXPath();
        
        final List<Field> fields = target.getModel().getField();
		String isModelArray = target.getModel().getIsArray();
		String modelXpath = target.getModel().getXpath();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		if ("1".equals(isModelArray) || "tre".equals(isModelArray)){
			XPathExpression expr = xpathParser.compile(modelXpath);
	        Object result = expr.evaluate(doc, XPathConstants.NODESET);
//		    listener.onInfo(Thread.currentThread(), "modelXpath -> " + modelXpath + " parse result -> " + result);
	        if (result != null){
		        NodeList nodes = (NodeList) result;
		        if (nodes.getLength() > 0){
			        for (int i = 0; i < nodes.getLength(); i++) {
						list.add(parse2Map(nodes.item(i), xpathParser, fields));
			        }
		        }
	        }
		}else{
			list.add(parse2Map(doc, xpathParser, fields));
		}
		return list;
	}
	private Map<String, Object> parse2Map(Object item, XPath xpathParser, final List<Field> fields) {
		Map<String, Object> map = new HashMap<String, Object>();
		for (Field field : fields){
			try {
				String key = field.getName();
				String xpath = field.getParser().getXpath();
				String attribute = field.getParser().getAttribute();
				String regex = field.getParser().getRegex();
				String isArray = field.getIsArray();
				
				XPathExpression expr = xpathParser.compile(xpath);
		        Object result = expr.evaluate(item, XPathConstants.NODESET);
		        
//		        listener.onInfo(Thread.currentThread(), "xpath -> " + xpath + " parse result -> " + result);
				if (result == null)
					continue;
				NodeList nodes = (NodeList) result;
				if (nodes.getLength() == 0)
					continue;
				
//		        for (int i = 0; i < nodes.getLength(); i++) {
//					Node node = nodes.item(i);
//					listener.onInfo(Thread.currentThread(), key+"->"+node.getNodeValue());
//				}
		        
				Collection<Object> value = new ArrayList<Object>();
				if (attribute != null && attribute.trim().length() > 0){
					for (int i = 0; i < nodes.getLength(); i++) {
						Node node = nodes.item(i);
						Element e = (Element)node;
						value.add(e.getAttribute(attribute));
					}
				}else {
					for (int i = 0; i < nodes.getLength(); i++) {
						Node node = nodes.item(i);
						value.add(node.getNodeValue());
					}
				}
				
				parseByRegex(regex, value);
				
				if ("1".equals(isArray))
					map.put(key, value);
				else
					map.put(key, new ArrayList<Object>(value).get(0));
			} catch (Exception e) {
				listener.onError(Thread.currentThread(), e.toString(), e);
				continue;
			}
		}
		
		return map;
	}
	
	private Map<String, Object> parseHtml(String html){
		Map<String, Object> map = new HashMap<String, Object>();
		final List<Field> fields = target.getModel().getField();
		HtmlCleaner cleaner = new HtmlCleaner();
		TagNode rootNode = cleaner.clean(html);
		
		for (Field field : fields){
			try {
				String key = field.getName();
				String xpath = field.getParser().getXpath();
				String attribute = field.getParser().getAttribute();
				String regex = field.getParser().getRegex();
				String isArray = field.getIsArray();
				Object[] nodeVals = rootNode.evaluateXPath(xpath);
//				listener.onInfo(Thread.currentThread(), "xpath -> " + xpath + " parse result -> " + CommonUtil.toJson(nodeVals));
				if (nodeVals == null || nodeVals.length == 0)
					continue;
				
				Collection<Object> value = new ArrayList<Object>();
				if (attribute != null && attribute.trim().length() > 0){
					for (Object nodeVal : nodeVals){
						TagNode node = (TagNode)nodeVal;
						value.add(node.getAttributeByName(attribute));
					}
				}else 
					value.addAll(Arrays.asList(nodeVals));
				
				this.parseByRegex(regex, value);
				if ("1".equals(isArray))
					map.put(key, value);
				else
					map.put(key, new ArrayList<Object>(value).get(0).toString());
				
			} catch (XPatherException e) {
				listener.onError(Thread.currentThread(), e.toString(), e);
				continue;
			}
		}
		
		return map;
	}
	private void parseByRegex(String regex, Collection<Object> value) {
		if (regex != null && regex.trim().length() > 0){
			Collection<String> inputs = new ArrayList<String>(value.size());
			for (Object obj : value){
				String input = String.valueOf(obj);
				List<String> vals = CommonUtil.findByRegex(input, regex);
				if (vals != null)
					inputs.addAll(vals);
				else
					inputs.add(input);
			}
			
			if (!inputs.isEmpty()){
				value.clear();
				value.addAll(inputs);
			}
		}
	}
}
