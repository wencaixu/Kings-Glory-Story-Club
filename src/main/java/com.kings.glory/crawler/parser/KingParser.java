package com.kings.glory.crawler.parser;

import com.kings.glory.crawler.WebAppConfig;
import com.kings.glory.vo.Hero;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.kings.glory.crawler.parser.Attrs.*;
import static com.kings.glory.crawler.parser.Tags.*;

/**
 * @author wencai.xu
 */
public class KingParser implements Parser {

    private static KingParser kingParser = new KingParser();
    private StoryParser storyParser = StoryParser.getInstance();

    private String page;
    private List<Hero> heros = new ArrayList<>();

    private ExecutorService executors = Executors.newCachedThreadPool(new ThreadFactory() {
        AtomicInteger integer = new AtomicInteger();
        @Override
        public Thread newThread(@NotNull Runnable r) {
            return new Thread(r,"parser-thread-"+integer.getAndIncrement());
        }
    });

    @Override
    public void parser() throws ExecutionException, InterruptedException {
        Document document = Jsoup.parse(page);
        if(document == null || StringUtils.isEmpty(document.body().html())){
            return;
        }
        Elements heroBox = document.getElementsByClass(WebAppConfig.kingClassName);
        Elements heroLists = heroBox.get(0).getElementsByTag(li.name());
        long start;
        System.out.println("开始时间==="+(start=System.currentTimeMillis()));
        AtomicInteger count = new AtomicInteger();
        for(Element element : heroLists){
            Hero hero = new Hero();
            count.getAndIncrement();
            Future<Object> submit = executors.submit(() -> {
                Elements aTag = element.getElementsByTag(a.name());
                String uri = WebAppConfig.baseUri + aTag.attr(href.name());
                hero.setDetail(parserStory(uri));
                hero.setHero(aTag.get(0).getElementsByTag(img.name()).get(0).attr(alt.name()));
                hero.setPicture("http:" + aTag.get(0).getElementsByTag(img.name()).get(0).attr(src.name()));
                return hero;
            });
            heros.add((Hero) submit.get());
        }
        //4922
        System.out.println("结束时间==="+(System.currentTimeMillis()-start));
        System.out.println("共抓取:"+count.get());
    }

    public static KingParser getInstance(){
        return kingParser;
    }

    private String parserStory(String uri){
        storyParser.setUri(uri);
        storyParser.parser();
        return storyParser.getStory();
    }

    public void setPage(String page) {
        this.page = page;
    }

    public List<Hero> getHeros(){
        if(CollectionUtils.isEmpty(heros)){
            try {
                parser();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return heros;
    }
}
