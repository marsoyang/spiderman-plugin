package spiderman.plugin.impl;

import java.util.List;
import java.util.Map;

import org.eweb4j.spiderman.plugin.EndPoint;
import org.eweb4j.spiderman.spider.SpiderListener;
import org.eweb4j.spiderman.task.Task;

public class EndPointImpl implements EndPoint{

	public void init(Task task, List<Map<String, Object>> models, SpiderListener listener) throws Exception {
	}
	
	public List<Map<String, Object>> complete(List<Map<String, Object>> dataMap) throws Exception {
		
		return dataMap;
	}

}
