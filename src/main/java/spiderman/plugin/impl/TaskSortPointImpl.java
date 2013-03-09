package spiderman.plugin.impl;

import java.util.Collection;

import org.eweb4j.spiderman.plugin.TaskSortPoint;
import org.eweb4j.spiderman.spider.SpiderListener;
import org.eweb4j.spiderman.task.Task;
import org.eweb4j.spiderman.url.SourceUrlChecker;
import org.eweb4j.spiderman.xml.Site;
import org.eweb4j.spiderman.xml.Target;
import org.eweb4j.util.CommonUtil;

import spiderman.plugin.util.Util;

public class TaskSortPointImpl implements TaskSortPoint {

	public void init(Site site, SpiderListener listener) {
	}

	public void destroy() {
	}
	
	public synchronized Collection<Task> sortTasks(Collection<Task> tasks) throws Exception {
		double i = 0;
		for (Task task : tasks) {
			i += 0.0001;
			// 检查url是否符合target的url规则，并且是否是来自于来源url
			Target tgt = Util.isTargetUrl(task);
			boolean isFromSourceUrl = SourceUrlChecker.checkSourceUrl(task.site.getTargets().getTarget().get(0).getSourceRules(), task.sourceUrl);
			if (tgt != null && isFromSourceUrl){
				task.sort = 5 + CommonUtil.toDouble("0."+System.currentTimeMillis()) + i;
			}else{
				//检查url是否符合target的sourceUrl规则
				boolean isSourceUrl = SourceUrlChecker.checkSourceUrl(task.site.getTargets().getTarget().get(0).getSourceRules(), task.url);
				if (isSourceUrl){
					task.sort = 0 + CommonUtil.toDouble("0."+System.currentTimeMillis()) + i;
				}else{
					task.sort = 10 + CommonUtil.toDouble("0."+System.currentTimeMillis()) + i;
				}
			}
		}

		return tasks;
	}

}
