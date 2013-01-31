package spiderman.plugin.impl;

import org.eweb4j.spiderman.fetcher.FetchResult;
import org.eweb4j.spiderman.plugin.FetchPoint;
import org.eweb4j.spiderman.spider.SpiderListener;
import org.eweb4j.spiderman.task.Task;
import org.eweb4j.spiderman.xml.Site;
import org.eweb4j.util.CommonUtil;

import spiderman.plugin.util.PageFetcherImpl;
import spiderman.plugin.util.SpiderConfig;

/**
 * 一个Host一个FetchPointImpl对象
 * @author weiwei l.weiwei@163.com
 * @date 2013-1-7 下午06:40:05
 */
public class FetchPointImpl implements FetchPoint{

	private SpiderListener listener = null;
//	private Task task = null;
	private Site site = null;
	
	public void init(Site site, SpiderListener listener) {
		this.site = site;
		this.listener = listener;
	}

	public void destroy() {
	}

	
//	public void context(Task task) throws Exception {
//		this.task = task;
//	}
	
	public FetchResult fetch(Task task, FetchResult result) throws Exception {
		synchronized (site) {
			if (site.fetcher == null){
				PageFetcherImpl fetcher = new PageFetcherImpl();
				SpiderConfig config = new SpiderConfig();
				config.setCharset(task.site.getCharset());
				String sdelay = task.site.getReqDelay();
				if (sdelay == null || sdelay.trim().length() == 0)
					sdelay = "200";
				
				int delay = CommonUtil.toSeconds(sdelay).intValue()*1000;
				if (delay < 0)
					delay = 200;
				
				config.setPolitenessDelay(delay);
				fetcher.setConfig(config);
				
				fetcher.init(site);
				site.fetcher = fetcher;
			}
			FetchResult fr = site.fetcher.fetch(task.url.replace(" ", "%20"));
			return fr;
		}
//		return fetch();
	}
	
//	private FetchResult fetch(){
//		FetchResult fetchResult = new FetchResult();
//		CrawlerConfiguration config = new CrawlerConfiguration(task.url);
//		
//		listener.onInfo(Thread.currentThread(), "crawling url: " + task.url);
//
//		Url urlToCrawl = new Url(config.beginUrl(), 0);
//        Page page = config.downloader().get(urlToCrawl.link());
//        if (page.getStatusCode() != Status.OK) {
//        	listener.onError(Thread.currentThread(), "errorUrl->" + urlToCrawl.link(), new Exception(page.getStatusCode().name() + " link->" + urlToCrawl.link()));
//        } else {
//        	org.eweb4j.spiderman.fetcher.Page _page = new org.eweb4j.spiderman.fetcher.Page();
//			_page.setContent(page.getContent());
//			_page.setContentType("text/html");
//			_page.setContentData(page.getContent().getBytes());
//			_page.setCharset(page.getCharset());
//			_page.setUrl(page.getUrl());
//			fetchResult.setPage(_page);
//			fetchResult.setFetchedUrl(page.getUrl());
//			fetchResult.setStatusCode(page.getStatusCode().ordinal());
//        }
//
//        for (String l : page.getLinks()) {
//            String link = config.normalizer().normalize(l);
//            final Url url = new Url(link, urlToCrawl.depth() + 1);
//            //是否进入递归抓取，如果进入递归就需要控制深度
//        }
//        
//        return fetchResult;
//	}

}
