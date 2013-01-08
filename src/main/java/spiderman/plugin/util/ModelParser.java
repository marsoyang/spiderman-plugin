package spiderman.plugin.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
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
import org.eweb4j.spiderman.task.Task;
import org.eweb4j.spiderman.xml.Field;
import org.eweb4j.spiderman.xml.Target;
import org.eweb4j.util.CommonUtil;
import org.eweb4j.util.FileUtil;
import org.eweb4j.util.xml.Attrs;
import org.eweb4j.util.xml.Tags;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.Serializer;
import org.htmlcleaner.SimpleXmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.helpers.DefaultHandler;

import com.greenpineyu.fel.FelEngine;
import com.greenpineyu.fel.FelEngineImpl;
import com.greenpineyu.fel.context.FelContext;
import com.greenpineyu.fel.function.CommonFunction;
import com.greenpineyu.fel.function.Function;

public class ModelParser extends DefaultHandler{

	private Task task = null;
	private Target target = null;
	private SpiderListener listener = null;
	private static FelEngine fel = new FelEngineImpl();
	
	static {
        Function fun = new CommonFunction() {
			public String getName() {
				return "$output";
			}

			public Object call(Object[] arguments) {
				Object node = arguments[0];
				boolean keepHeader = false;
				if (arguments.length > 2)
					keepHeader = (Boolean) arguments[1];
				
				return ParserUtil.xml(node, keepHeader);
			}
		};
		
    	fel.addFun(fun);
	}
	public ModelParser(){}
	public ModelParser(Task task, Target target, SpiderListener listener) {
		this.task = task;
		this.target = target;
		this.listener = listener;
	}
	
	public static void main(String[] args){
		List<String> list = new ArrayList<String>(Arrays.asList("1", "2", "3"));
		list.addAll(0, Arrays.asList("0"));
		System.out.println(list);
	}
	
	public static void mains(String[] args) throws Exception{
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
        
//        int count = 0;
//        String regex = "\\w+\\.(gif|png|jpg|jpeg|bmp)";
        for (int i = 0; i < nodes.getLength(); i++) {
        	if (i > 0)
        		break;
            
        	NodeList subs = (NodeList)xpath.compile("Description").evaluate(nodes.item(i), XPathConstants.NODESET);
        	
        	Node node = subs.item(0);
        	FelContext ctx = fel.getContext();
        	ctx.set("$this", node);
        	Tags $Tags = Tags.me();
        	Attrs $Attrs = Attrs.me();
			ctx.set("$Tags", $Tags);
			ctx.set("$Attrs", $Attrs);
    		
			System.out.println($Attrs.xml(ParserUtil.xml(node, false)).rm("style").Tags().kp("p").ok());
    		
    		System.out.println(fel.eval("$Attrs.xml($output($this)).rm('style').Tags().kp('p').ok()"));
    		
//    		Object newVal =  MVEL.eval("org.eweb4j.util.CommonUtil.toXml($this, false)", ctx);
//    		System.out.println(newVal);
        }

//        	
//            NodeList subs = (NodeList)xpath.compile("*[matches(text(),'"+regex+"')]/text()").evaluate(nodes.item(i), XPathConstants.NODESET);
//            if (subs == null || subs.getLength() == 0)
//            	continue;
//            for (int j = 0; j < subs.getLength(); j++) {
//            	Node item = subs.item(j);
//            	String value = item.getNodeValue();
//            	List<String> imgs = CommonUtil.findByRegex(value, "[^\\s'=\"]+\\.(gif|png|jpg|jpeg|bmp)(?=[\"']?)");
//            	System.out.println(item.getParentNode().getNodeName()+"->"+imgs);
//            	count++;
//            }
//        }
//        System.out.println("count->"+count);
        
        String html = "<div id='desc'><p><a href='http://www.baidu.com'>click me</a></p></div>";
        HtmlCleaner cleaner = new HtmlCleaner();
		TagNode tagNode = cleaner.clean(html);
		Object[] nodeVals = tagNode.evaluateXPath("//p");
		StringWriter sw = new StringWriter();  
	    Serializer serializer = new SimpleXmlSerializer(cleaner.getProperties());  
	    serializer.write((TagNode)nodeVals[0], sw, "UTF-8");
//	    System.out.println(sw.getBuffer().toString());
		
//		//第一步：获得解析工厂的实例  
//        SAXParserFactory spf = SAXParserFactory.newInstance();  
//        //第二部：获得工厂解析器  
//        SAXParser sp = spf.newSAXParser();  
//        //第三部：对xml进行解析  
//        sp.parse(file, new ModelParser());
        
	}
	
	public List<Map<String, Object>> parse(Page page) throws Exception{
		listener.onInfo(Thread.currentThread(), task, "parse Page->[cType:" + page.getContentType()+",charset:"+page.getCharset()+",encoding:"+page.getEncoding()+",url->"+page.getUrl());
		String contentType = page.getContentType();
		if (contentType == null)
			contentType = "text/html";
		boolean isXml = contentType.contains("text/xml") || contentType.contains("application/rss+xml") || contentType.contains("application/xml");
		
		//解析xml
		if (isXml) 
			return parseXml(page);
		
		// TODO 解析 JSON
		
		//解析html
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		list.add(parseHtml(page));
		return list;
	}

	private List<Map<String, Object>> parseXml(Page page) throws Exception{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(page.getContentData()));
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
				String exp = field.getParser().getExp();
				
				XPathExpression expr = xpathParser.compile(xpath);
		        Object result = expr.evaluate(item, XPathConstants.NODESET);
		        
				if (result == null)
					continue;
				NodeList nodes = (NodeList) result;
				if (nodes.getLength() == 0)
					continue;
				
				List<Object> values = new ArrayList<Object>();
				
				if (attribute != null && attribute.trim().length() > 0){
					for (int i = 0; i < nodes.getLength(); i++){
						Node node = nodes.item(i);
						Element e = (Element)node;
						String[] attrs = attribute.split("\\|");
						for (String attr : attrs){
							String attrVal = e.getAttribute(attr);
							if (attr == null || attr.trim().length() == 0)
								continue;
							values.add(attrVal);
						}
					}
					
					//正则
					parseByRegex(regex, values);
					// EXP表达式
					parseByExp(exp, values);
				}else if (xpath.endsWith("/text()")){
					for (int i = 0; i < nodes.getLength(); i++){
						Node node = nodes.item(i);
						values.add(node.getNodeValue());
					}
					//正则
					parseByRegex(regex, values);
					// EXP表达式
					parseByExp(exp, values);
				} else {
					for (int i = 0; i < nodes.getLength(); i++){
						Node node = nodes.item(i);
						values.add(node);
					}
					// 此种方式获取到的Node节点大部分都不是字符串，因此先执行表达式后执行正则
					// EXP表达式
					parseByExp(exp, values);
					//正则
					parseByRegex(regex, values);
				}
				
				if ("1".equals(isArray)){
					//如果字段key为数组且values不为空，继续沿用
					if (map.containsKey(key)){
						//将原来的值插入到前面
						values.addAll(0, (Collection<?>) map.get(key));
					}
					
					map.put(key, values);
				} else {
					map.put(key, new ArrayList<Object>(values).get(0));
				}
			} catch (Exception e) {
				listener.onError(Thread.currentThread(), task, e.toString(), e);
				continue;
			}
		}
		
		return map;
	}
	
	private Map<String, Object> parseHtml(Page page){
		Map<String, Object> map = new HashMap<String, Object>();
		final List<Field> fields = target.getModel().getField();
		HtmlCleaner cleaner = new HtmlCleaner();
		TagNode rootNode = cleaner.clean(page.getContent());
		
		for (Field field : fields){
			try {
				String key = field.getName();
				String xpath = field.getParser().getXpath();
				String attribute = field.getParser().getAttribute();
				String regex = field.getParser().getRegex();
				String isArray = field.getIsArray();
				String exp = field.getParser().getExp();
				
				Object[] nodeVals = rootNode.evaluateXPath(xpath);
				if (nodeVals == null || nodeVals.length == 0)
					continue;
				
				List<Object> values = new ArrayList<Object>();
				
				if (attribute != null && attribute.trim().length() > 0){
					for (Object nodeVal : nodeVals){
						TagNode node = (TagNode)nodeVal;
						String[] attrs = attribute.split("\\|");
						for (String attr : attrs){
							String attrVal = node.getAttributeByName(attribute);
							if (attr == null || attr.trim().length() == 0)
								continue;
							values.add(attrVal);
						}
					}
					//正则
					parseByRegex(regex, values);
					// EXP表达式
					parseByExp(exp, values);
				}else if (xpath.endsWith("/text()")){
					for (Object nodeVal : nodeVals){
						values.add(nodeVal.toString());
					}
					//正则
					parseByRegex(regex, values);
					// EXP表达式
					parseByExp(exp, values);
				} else {
					for (Object nodeVal : nodeVals){
						TagNode node = (TagNode)nodeVal;
						values.add(node);
					}
					// 此种方式获取到的Node节点大部分都不是字符串，因此先执行表达式后执行正则
					// EXP表达式
					parseByExp(exp, values);
					//正则
					parseByRegex(regex, values);
				}
				
				if ("1".equals(isArray)){
					//如果字段key为数组且values不为空，继续沿用
					if (map.containsKey(key)){
						//将原来的值插入到前面
						values.addAll(0, (Collection<?>) map.get(key));
					}
					
					map.put(key, values);
				}else{
					map.put(key, new ArrayList<Object>(values).get(0).toString());
				}
				
			} catch (XPatherException e) {
				listener.onError(Thread.currentThread(), task, e.toString(), e);
				continue;
			}
		}
		
		return map;
	}
	
	private void parseByExp(String exp, Collection<Object> list) {
		if (exp == null || exp.trim().length() == 0)
			return ;
			
		List<Object> newValue = new ArrayList<Object>();
		for (Object val : list){
			fel.getContext().set("$this", val);
			Tags $Tags = Tags.me();
        	Attrs $Attrs = Attrs.me();
        	fel.getContext().set("$Tags", $Tags);
        	fel.getContext().set("$Attrs", $Attrs);
    		Object newVal = fel.eval(exp);
			if (newVal == null)
				newVal = val;
			
			newValue.add(newVal);
		}
		
		list.clear();
		list.addAll(newValue);
	}
	
	private void parseByRegex(String regex, Collection<Object> list) {
		if (regex == null || regex.trim().length() == 0)
			return ;
		List<Object> newVals = new ArrayList<Object>(list.size());
		for (Object obj : list) {
			String input = (String)obj;
			List<String> vals = CommonUtil.findByRegex(input, regex);
			if (vals == null)
				continue;
			else {
				for (String val : vals){
					if (val == null || val.trim().length() == 0)
						continue;
					newVals.add(val);
				}
			}
		}
		
		if (!newVals.isEmpty()){
			list.clear();
			list.addAll(newVals);
		}
	}
}
