package com.webcollector.demo;

import cn.edu.hfut.dmic.contentextractor.ContentExtractor;
import cn.edu.hfut.dmic.webcollector.model.CrawlDatums;
import cn.edu.hfut.dmic.webcollector.model.Page;
import cn.edu.hfut.dmic.webcollector.plugin.rocks.BreadthCrawler;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Crawling news from github news
 * 自动 弹出 URL 地址，继承 BreadthCrawler（广度爬虫）
 * <p/>
 * cn.edu.hfut.dmic.webcollector.plugin.rocks.BreadthCrawler是基于RocksDB的插件,于2.72版重新设计
 * BreadthCrawler可以设置正则规律，让遍历器自动根据URL的正则遍历网站，可以关闭这个功能，自定义遍历
 * 如果autoParse设置为true，遍历器会自动解析页面中符合正则的链接，加入后续爬取任务，否则不自动解析链接。
 * 注意，爬虫会保证爬取任务的唯一性，也就是会自动根据CrawlDatum的key进行去重，默认情况下key就是URL，
 * 所以用户在编写爬虫时完全不必考虑生成重复URL的问题。
 * 断点爬取中，爬虫仍然会保证爬取任务的唯一性。
 *
 * @author hu
 */
public class MyAutoNewsCrawler extends BreadthCrawler {
    /**
     * 构造一个基于 RocksDB 的爬虫
     * RocksDB文件夹为crawlPath，crawlPath中维护了历史URL等信息
     * 不同任务不要使用相同的crawlPath
     * 两个使用相同crawlPath的爬虫并行爬取会产生错误
     *
     * @param crawlPath RocksDB使用的文件夹
     * @param autoParse 是否根据设置的正则自动探测新URL ,默认为 true
     */
    public MyAutoNewsCrawler(String crawlPath, boolean autoParse) {
            super(crawlPath, autoParse);

        /**
         * 只有在autoParse和autoDetectImg都为true的情况下
         * 爬虫才会自动解析图片链接
         */
        getConf().setAutoDetectImg(true);

        /**设置爬取的网站地址
         * addSeed 表示添加种子
         * 种子链接会在爬虫启动之前加入到抓取信息中并标记为未抓取状态.这个过程称为注入
         * 放入爬取的目标网址
         */
        this.addSeed("http://www.gov.cn/xinwen/yaowen.htm");
//        this.addSeed("http://www.gov.cn/xinwen/2018-08/13/content_5313418.htm");

        /**
         * 添加一个 URL 正则规则 正则规则有两种，正正则和反正则
         * URL 符合正则规则需要满足两个条件： 1.至少能匹配一条正正则 2.不能和任何反正则匹配
         * 正正则示例：+abc.*efg 是一条正正则，正则的内容为 abc.*efg ，起始加号表示正正则
         * 反正则示例：-abc.*efg 是一条反正则，正则的内容为 abc.*efg ，起始减号表示反正则
         * (注1)如果一个规则的起始字符不为加号且不为减号，则该正则为正正则，正则的内容为自身，如 a.*c 是一条正正则，正则的内容为 a.*c
         * (注2) 正则的内容与平时 js 与 java 中使用的正则写法是一样的，如 "."表示任意字符、"*"表示0次或多次、[0-9]{4} 表示连续4个数字
         * */
        this.addRegex("http://www.gov.cn/xinwen/[0-9]{4}-[0-9]{2}/[0-9]{2}/content_.*");
        /**将网页中的图片链接也留下来
         * 此时不要再讲图片过滤出去了*/
        this.addRegex(".*/images/.*.(jpg|png|gif)");

        /**
         * 过滤 jpg|png|gif 等图片地址 时：
         * this.addRegex("-.*\\.(jpg|png|gif).*");
         */

        /**
         * 过滤 链接值含 "#" 的地址
         */
        this.addRegex("-.*#.*");

        /**设置线程数*/
        setThreads(10);
        getConf().setTopN(100);

        /**
         * 是否进行断电爬取，默认为 false
         * setResumable(true);
         */
    }

    /**
     * 必须重写 visit 方法，作用是:
     * 在整个抓取过程中,只要抓到符合要求的页面,webCollector 就会回调该方法,并传入一个包含了页面所有信息的 page 对象
     *
     * @param page ：Page是爬取过程中，内存中保存网页爬取信息的一个容器，Page只在内存中存放，用于保存一些网页信息，方便用户进行自定义网页解析之类的操作。
     * @param next ：可以手工将希望后续采集的任务加到next中（会参与自动去重），即如果需要后续再次进行爬取的，则可以添加进去
     */
    @Override
    public void visit(Page page, CrawlDatums next) {
        String url = page.url();

        System.out.println(" page 类型：" + page.contentType());
        System.out.println("开始爬取 URL：：" + url);

        /**
         * 判断当前 Page 的 URL 是否和输入正则匹配
         * 如果此页面地址 确实是要求爬取网址，则进行取值，contentType 是页面请求的内容类型，可以通过浏览器 F12 查看
         */
        if (page.contentType().startsWith("text/html")) {
            /**
             * page 为 html 网页类型
             */
            if (page.matchUrl("http://www.gov.cn/xinwen/[0-9]{4}-[0-9]{2}/[0-9]{2}/content_.*")) {

                /**
                 * 通过 选择器 获取页面 标题、以及 正文内容、时间
                 * 这些选择器语法就是 Jsoup，类似于 JQuery 的选择器，非常方便
                 * 可以参考 Jsoup 官网：https://jsoup.org/cookbook/extracting-data/selector-syntax
                 * select：返回的是 Elements
                 * selectText：是直接获取元素的文本内容，只要是此元素下的，无论嵌套多少层在下面，都会进行获取
                 * <div class="d_con dd_con"><p>...</p></div>：当样式有多个时，选择器使用"."号连接
                 * 因为 "空格" 本身就是一个层级选择器，"A B" 表示查找A元素下的所有B元素
                 * */

                String title = page.selectText("div.article.oneColumn.pub_border>h1");
                String content = page.selectText("div#UCAP-CONTENT");
                String dateStr = page.selectText("div.pages-date");

                System.out.println("title：：" + title);
                /**解析日期文本的自定义工具类*/
                Date date = SystemUtils.parseDateStr(dateStr);
                if (date == null) {
                    System.out.println("发布时间：null");
                } else {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    System.out.println("发布时间：" + simpleDateFormat.format(date));
                }
                System.out.println("content：：" + content);

                /**
                 * 获取正文中的所有图片元素，方法一：仍然和以前一样使用 获取图片元素的方式，这样当页面图片地址是相对路径时
                 * 如 <img src="../images/hh13.png"/>,则获取的值也是相对路径，解决办法之一是自己根据上下文关系去拼接成一个合法的地址
                 *
                 * 本例还会使用 webCollector 自动探测页面超链接时，将图片链接也一并获取出来，
                 * 因为 webCollector 根据 http 请求地址获取，所以会是完整的绝对路径
                 */
                Elements imgElements = page.select("div#UCAP-CONTENT img");
                for (Element imgElement : imgElements) {
                    System.out.println("文章图片(Elements方式)：" + imgElement.attr("src") + "\n");
                }
            }
        } else if (page.contentType().startsWith("image")) {
            /**
             * page 为 图像类型
             */
            System.out.println("图片地址(page.contentType()方式)：" + url);
        }
    }

    public static void main(String[] args) throws Exception {

        ContentExtractor contentExtractor;

        /**
         * MyAutoNewsCrawler 构造器中会进行 数据初始化，这两个参数接着会传给父类
         * super(crawlPath, autoParse);
         * crawlPath：表示设置保存爬取记录的文件夹，本例运行之后会在应用根目录下生成一个 "crawl" 目录存放爬取信息
         * autoParse：表示进行 URL自动探测
         * */
        MyAutoNewsCrawler crawler = new MyAutoNewsCrawler("crawl", true);
        /**
         * 启动爬虫，爬取的深度为3层
         * 添加的第一层种子链接,为第1层，本例层级如下：
         * 第一层：爬取的目标网址，即文章列表页
         * 第二层：具体的文章内容页（此时需要获取的图片恰好就在其中）
         * 第三层：就是为了获取第二层中的图片地址
         */
        crawler.start(3);
    }
}