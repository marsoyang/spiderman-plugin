package spiderman.plugin.impl;

import java.util.List;
import java.util.Map;

import org.eweb4j.spiderman.fetcher.Page;
import org.eweb4j.spiderman.plugin.ParsePoint;
import org.eweb4j.spiderman.spider.SpiderListener;
import org.eweb4j.spiderman.xml.Target;

import spiderman.plugin.util.ModelParser;

public class ParsePointImpl implements ParsePoint{

	private SpiderListener listener;
	private Target target ;
	private Page page;
	
	public void init(Target target, Page page, SpiderListener listener) throws Exception{
		this.target = target;
		this.page = page;
		this.listener = listener;
	}
	
	public List<Map<String, Object>> parse(List<Map<String, Object>> models) throws Exception {
		return parseTargetModelByXpathAndRegex();
	}

	private List<Map<String,Object>> parseTargetModelByXpathAndRegex() throws Exception {
		ModelParser parser = new ModelParser(target, listener);
		return parser.parse(page);
	}
}
