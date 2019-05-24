package com.webcollector.demo;

import cn.edu.hfut.dmic.webcollector.model.CrawlDatums;
import cn.edu.hfut.dmic.webcollector.model.Page;
import cn.edu.hfut.dmic.webcollector.plugin.ram.RamCrawler;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * WebCollector 2.40新特性 page.matchType
 * 在添加CrawlDatum时（添加种子、或在抓取时向next中添加任务），
 * 可以为CrawlDatum设置type信息
 * <p/>
 * type的本质也是meta信息，为CrawlDatum的附加信息
 * 在添加种子或向next中添加任务时，设置type信息可以简化爬虫的开发
 * <p/>
 * 例如在处理列表页时，爬虫解析出内容页的链接，在将内容页链接作为后续任务
 * 将next中添加时，可设置其type信息为content（可自定义），在后续抓取中，
 * 通过page.matchType("content")就可判断正在解析的页面是否为内容页
 * <p/>
 * 设置type的方法主要有3种：
 * 1）添加种子时，addSeed(url,type)
 * 2）向next中添加后续任务时：next.add(url,type)或next.add(links,type)
 * 3）在定义CrawlDatum时：crawlDatum.type(type)
 *
 * @author hu
 */
public class MyTypeCrawler extends RamCrawler {

    @Override
    public void visit(Page page, CrawlDatums next) {

        /**
         * 除非是 CrawlDatum 显示指定的 meta ，否则 page.meta 是取不到值的
         * page.matchType 才能获取隐士的 meta 的 type 值
         * 所以 page.meta("type") 的值 会为 null，写出来为的是 以示提醒
         */
        String type = page.meta("type");
        String url = page.url();

        System.out.println("当前页面类型：" + type + " >>> 页面地址：" + url);
        if (page.matchType("tagList")) {
            /**
             * 当是列表页时，获取其中的内容页地址,同时为地址设置 MetaData(元信息)
             */
            next.addAndReturn(page.links("div#syncad_1 a")).type("content");
        } else if (page.matchType("content")) {
            /**
             * 当前页面是内容页时，直接获取内容
             * 这里选择通过选择器的方式进行获取，也可以使用  ContentExtractor 内容快速提取
             * selectText 方法底层就是 select(cssSelector).first().text()
             * text() 方法会获取当前元素中的文本内容，无论嵌套在当前元素下多少级，都会获取
             * 这些选择器
             */
            String title = page.selectText("h1.main-title");
            String dataStr = page.selectText("span.date");
            String content = page.select("div#article").first().text();

            System.out.println("\t正文标题：" + title);
            Date date = SystemUtils.parseDateStr(dataStr);
            if (date != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                System.out.println("发布时间：" + dateFormat.format(date));
            } else {
                System.out.println("发布时间：null");
            }
            Elements imgElements = page.select("div#article img");
            for (Element element : imgElements) {
                System.out.println(">>>>>图片地址：" + element.attr("src"));
            }
            System.out.println("正文内容：" + content);

        }
    }

    public static void main(String[] args) throws Exception {
        MyTypeCrawler myTypeCrawler = new MyTypeCrawler();

        myTypeCrawler.addSeed("https://news.sina.com.cn/", "tagList");

        /**
         * 设置是否自动抽取符合正则的链接并加入后续任务
         * autoParse 默认为 true，即使没有指明为false，只要未设置 url 正则表达式，同样不会自动探测
         * 设置线程数为 30
         */
        myTypeCrawler.setAutoParse(false);
        myTypeCrawler.setThreads(30);
        /**
         * 千万要记得设置爬取的层级深度，否则默认为  start(Integer.MAX_VALUE);
         */
        myTypeCrawler.start(2);

    }

}