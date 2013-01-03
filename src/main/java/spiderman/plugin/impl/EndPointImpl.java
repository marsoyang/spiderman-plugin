package spiderman.plugin.impl;

import java.util.Map;

import org.eweb4j.spiderman.plugin.EndPoint;
import org.eweb4j.spiderman.spider.SpiderListener;
import org.eweb4j.spiderman.task.Task;

public class EndPointImpl implements EndPoint{

	public void init(Task task, Map<String, Object> model, SpiderListener listener) throws Exception {
	}
	
	public Map<String, Object> complete(Map<String, Object> dataMap) throws Exception {
		
		return dataMap;
	}

}
