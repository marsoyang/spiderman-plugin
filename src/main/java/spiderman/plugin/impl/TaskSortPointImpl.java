package spiderman.plugin.impl;

import java.util.Collection;

import org.eweb4j.spiderman.plugin.TaskSortPoint;
import org.eweb4j.spiderman.spider.SpiderListener;
import org.eweb4j.spiderman.task.Task;
import org.eweb4j.spiderman.url.SourceUrlChecker;
import org.eweb4j.spiderman.xml.Site;
import org.eweb4j.spiderman.xml.Target;

import spiderman.plugin.util.Util;

public class TaskSortPointImpl implements TaskSortPoint {

	public void init(Site site, SpiderListener listener) {
	}

	public void destroy() {
	}
	
	public synchronized Collection<Task> sortTasks(Collection<Task> tasks) throws Exception {
		for (Task task : tasks) {
			// 检查url是否符合target的url规则，并且是否是来自于来源url，如果符合排序调整为20
			Target tgt = Util.isTargetUrl(task);
			boolean isFromSourceUrl = SourceUrlChecker.checkSourceUrl(task.site.getTargets().getTarget().get(0).getSourceRules(), task.sourceUrl);
			if (tgt != null && isFromSourceUrl){
				task.sort = 20;
			}else{
				//检查url是否符合target的sourceUrl规则，如果符合排序调整为15，否则为0
				boolean isSourceUrl = SourceUrlChecker.checkSourceUrl(task.site.getTargets().getTarget().get(0).getSourceRules(), task.url);
				if (isSourceUrl){
					task.sort = 15;
				}else{
					task.sort = 0;
				}
			}
		}

		return tasks;
	}

}
