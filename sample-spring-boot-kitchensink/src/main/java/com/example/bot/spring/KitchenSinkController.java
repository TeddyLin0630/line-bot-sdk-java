/*
 * Copyright 2016 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.example.bot.spring;

import com.example.bot.spring.poker.BigOne;
import com.example.bot.spring.poker.Niuniu;
import com.example.bot.spring.poker.Poker;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.event.BeaconEvent;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.FollowEvent;
import com.linecorp.bot.model.event.JoinEvent;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.PostbackEvent;
import com.linecorp.bot.model.event.UnfollowEvent;
import com.linecorp.bot.model.event.message.AudioMessageContent;
import com.linecorp.bot.model.event.message.ImageMessageContent;
import com.linecorp.bot.model.event.message.LocationMessageContent;
import com.linecorp.bot.model.event.message.StickerMessageContent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.message.VideoMessageContent;
import com.linecorp.bot.model.event.source.GroupSource;
import com.linecorp.bot.model.event.source.RoomSource;
import com.linecorp.bot.model.event.source.Source;
import com.linecorp.bot.model.message.ImageMessage;
import com.linecorp.bot.model.message.LocationMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.StickerMessage;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.template.ButtonsTemplate;
import com.linecorp.bot.model.message.template.ConfirmTemplate;
import com.linecorp.bot.model.message.template.ImageCarouselColumn;
import com.linecorp.bot.model.message.template.ImageCarouselTemplate;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@LineMessageHandler
public class KitchenSinkController {

    enum YOUTUBE_LIST {
        android,
        buycartv,
        行車紀錄趣,
        木曜4超玩,
        一日,
        蔡阿嘎,
        國光幫幫忙,
        這群人
    }

    enum WEB_SITES {
//        jpBeautifyHouse,
//        ck101,
        //        voc,
//        plus28,
        gigacircle_31,
//        gigacircle_32,
//        hkCom,
//        news_gamme,
//        forum_gamme,
//        beautify_leg,
//        ookkk,
//        rosiyy,
//        k163k163,
//        sina_poppy,
//        ptt_beauty
    }

    enum WEB_SITES_18 {
        plus28,
        bbs_tw
    }

    enum WEB_SITES_MOTO {
        cafeRacers,
        motoGP,
        newMotoGP,
        canadaMoto
    }

    enum WEB_SITES_CAR {
        autoblog,
        autocar_rss,
        bmw_blog,
        driver_blog,
        high_gear_media,
        latest,
        truth_car
    }

    enum CONSTELLATION {
        Aries(0, "牡羊"),
        Taurus(1, "金牛"),
        Gemini(2, "雙子"),
        Cancer(3, "巨蟹"),
        Leo(4, "獅子"),
        Virgo(5, "處女"),
        Libra(6, "天秤"),
        Scorpio(7, "天蠍"),
        Sagittarius(8, "射手"),
        Capricorn(9, "摩羯"),
        Aquarius(10, "水瓶"),
        Pisces(11, "雙魚");

        int number;
        String name;

        CONSTELLATION(int num, String text) {
            number = num;
            name = text;
        }

        private int getNumber() {
            return number;
        }

        public static int value (String con) {
            for (CONSTELLATION constellation : CONSTELLATION.values()) {
                if (con.equalsIgnoreCase(constellation.name)) {
                    return constellation.getNumber();
                }
            }
            return 0;
        }
    }

    @Autowired
    private LineMessagingClient lineMessagingClient;

    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws Exception {
        TextMessageContent message = event.getMessage();
        handleTextContent(event.getReplyToken(), event, message);
    }

    @EventMapping
    public void handleStickerMessageEvent(MessageEvent<StickerMessageContent> event) {
//        handleSticker(event.getReplyToken(), event.getMessage());
    }

    @EventMapping
    public void handleLocationMessageEvent(MessageEvent<LocationMessageContent> event) {
      /*  LocationMessageContent locationMessage = event.getMessage();
        reply(event.getReplyToken(), new LocationMessage(
                locationMessage.getTitle(),
                locationMessage.getAddress(),
                locationMessage.getLatitude(),
                locationMessage.getLongitude()
        ));*/
    }

    @EventMapping
    public void handleImageMessageEvent(MessageEvent<ImageMessageContent> event) throws IOException {
        // You need to install ImageMagick
       /* handleHeavyContent(
                event.getReplyToken(),
                event.getMessage().getId(),
                responseBody -> {
                    DownloadedContent jpg = saveContent("jpg", responseBody);
                    DownloadedContent previewImg = createTempFile("jpg");
                    system(
                            "convert",
                            "-resize", "240x",
                            jpg.path.toString(),
                            previewImg.path.toString());
                    reply(((MessageEvent) event).getReplyToken(),
                            new ImageMessage(jpg.getUri(), jpg.getUri()));
                });*/
    }

    @EventMapping
    public void handleAudioMessageEvent(MessageEvent<AudioMessageContent> event) throws IOException {
       /* handleHeavyContent(
                event.getReplyToken(),
                event.getMessage().getId(),
                responseBody -> {
                    DownloadedContent mp4 = saveContent("mp4", responseBody);
                    reply(event.getReplyToken(), new AudioMessage(mp4.getUri(), 100));
                });*/
    }

    @EventMapping
    public void handleVideoMessageEvent(MessageEvent<VideoMessageContent> event) throws IOException {
        // You need to install ffmpeg and ImageMagick.
        /*handleHeavyContent(
                event.getReplyToken(),
                event.getMessage().getId(),
                responseBody -> {
                    DownloadedContent mp4 = saveContent("mp4", responseBody);
                    DownloadedContent previewImg = createTempFile("jpg");
                    system("convert",
                            mp4.path + "[0]",
                            previewImg.path.toString());
                    reply(((MessageEvent) event).getReplyToken(),
                            new VideoMessage(mp4.getUri(), previewImg.uri));
                });*/
    }

    @EventMapping
    public void handleUnfollowEvent(UnfollowEvent event) {
        log.info("unfollowed this bot: {}", event);
    }

    @EventMapping
    public void handleFollowEvent(FollowEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "Got followed event");
    }

    @EventMapping
    public void handleJoinEvent(JoinEvent event) {
        String replyToken = event.getReplyToken();
//        this.replyText(replyToken, "Joined " + event.getSource());
        this.reply(replyToken, getHelpMessage());
    }

    @EventMapping
    public void handlePostbackEvent(PostbackEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "Got postback data " + event.getPostbackContent().getData() + ", param " + event.getPostbackContent().getParams().toString());
    }

    @EventMapping
    public void handleBeaconEvent(BeaconEvent event) {
        String replyToken = event.getReplyToken();
        this.replyText(replyToken, "Got beacon message " + event.getBeacon().getHwid());
    }

    @EventMapping
    public void handleOtherEvent(Event event) {
        log.info("Received message(Ignored): {}", event);
    }

    private void reply(@NonNull String replyToken, @NonNull Message message) {
        reply(replyToken, Collections.singletonList(message));
    }

    private void reply(@NonNull String replyToken, @NonNull List<Message> messages) {
        try {
            BotApiResponse apiResponse = lineMessagingClient
                    .replyMessage(new ReplyMessage(replyToken, messages))
                    .get();
            log.info("Sent messages: {}", apiResponse);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void replyText(@NonNull String replyToken, @NonNull String message) {
        if (replyToken.isEmpty()) {
            throw new IllegalArgumentException("replyToken must not be empty");
        }
        if (message.length() > 1000) {
            message = message.substring(0, 1000 - 2) + "……";
        }
        this.reply(replyToken, new TextMessage(message));
    }

    private void handleHeavyContent(String replyToken, String messageId,
                                    Consumer<MessageContentResponse> messageConsumer) {
        final MessageContentResponse response;
        try {
            response = lineMessagingClient.getMessageContent(messageId)
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            reply(replyToken, new TextMessage("Cannot get image: " + e.getMessage()));
            throw new RuntimeException(e);
        }
        messageConsumer.accept(response);
    }

    private void handleSticker(String replyToken, StickerMessageContent content) {
        reply(replyToken, new StickerMessage(
                content.getPackageId(), content.getStickerId())
        );
    }

    private void handleTextContent(String replyToken, Event event, TextMessageContent content)
            throws Exception {
        String text = content.getText();
        text = text.toLowerCase();
        log.info("Got text message from {}: {}", replyToken, text);
        switch (text) {
            case "profile": {
                String userId = event.getSource().getUserId();
                if (userId != null) {
                    lineMessagingClient
                            .getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if (throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }

                                this.reply(
                                        replyToken,
                                        Arrays.asList(new TextMessage(
                                                        "Display name: " + profile.getDisplayName()),
                                                new TextMessage("Status message: "
                                                        + profile.getStatusMessage()))
                                );

                            });
                } else {
                    this.replyText(replyToken, "Bot can't use profile API without user ID");
                }
                break;
            }
            case "bye": {
                Source source = event.getSource();
                if (source instanceof GroupSource) {
                    this.replyText(replyToken, "Leaving group");
                    lineMessagingClient.leaveGroup(((GroupSource) source).getGroupId()).get();
                } else if (source instanceof RoomSource) {
                    this.replyText(replyToken, "Leaving room");
                    lineMessagingClient.leaveRoom(((RoomSource) source).getRoomId()).get();
                } else {
                    this.replyText(replyToken, "Bot can't leave from 1:1 chat");
                }
                break;
            }
            case "confirm": {
                ConfirmTemplate confirmTemplate = new ConfirmTemplate(
                        "Do it?",
                        new MessageAction("Yes", "Yes!"),
                        new MessageAction("No", "No!")
                );
                TemplateMessage templateMessage = new TemplateMessage("Confirm alt text", confirmTemplate);
                this.reply(replyToken, templateMessage);
                break;
            }
            case "buttons": {
                String imageUrl = createUri("/static/buttons/1040.jpg");
                ButtonsTemplate buttonsTemplate = new ButtonsTemplate(
                        imageUrl,
                        "My button sample",
                        "Hello, my button",
                        Arrays.asList(
                                new URIAction("Go to line.me",
                                        "https://line.me"),
                                new PostbackAction("Say hello1",
                                        "hello こんにちは"),
                                new PostbackAction("言 hello2",
                                        "hello こんにちは",
                                        "hello こんにちは"),
                                new MessageAction("Say message",
                                        "Rice=米")
                        ));
                TemplateMessage templateMessage = new TemplateMessage("Button alt text", buttonsTemplate);
                this.reply(replyToken, templateMessage);
                break;
            }
            /*case "carousel": {
                String imageUrl = createUri("/static/buttons/1040.jpg");
                CarouselTemplate carouselTemplate = new CarouselTemplate(
                        Arrays.asList(
                                new CarouselColumn(imageUrl, "hoge", "fuga", Arrays.asList(
                                        new URIAction("Go to line.me",
                                                      "https://line.me"),
                                        new URIAction("Go to line.me",
                                                "https://line.me"),
                                        new PostbackAction("Say hello1",
                                                           "hello こんにちは")
                                )),
                                new CarouselColumn(imageUrl, "hoge", "fuga", Arrays.asList(
                                        new PostbackAction("言 hello2",
                                                           "hello こんにちは",
                                                           "hello こんにちは"),
                                        new PostbackAction("言 hello2",
                                                "hello こんにちは",
                                                "hello こんにちは"),
                                        new MessageAction("Say message",
                                                          "Rice=米")
                                )),
                                new CarouselColumn(imageUrl, "Datetime Picker", "Please select a date, time or datetime", Arrays.asList(
                                        new DatetimePickerAction("Datetime",
                                                "action=sel",
                                                "datetime",
                                                "2017-06-18T06:15",
                                                "2100-12-31T23:59",
                                                "1900-01-01T00:00"),
                                        new DatetimePickerAction("Date",
                                                "action=sel&only=date",
                                                "date",
                                                "2017-06-18",
                                                "2100-12-31",
                                                "1900-01-01"),
                                        new DatetimePickerAction("Time",
                                                "action=sel&only=time",
                                                "time",
                                                "06:15",
                                                "23:59",
                                                "00:00")
                                ))
                        ));
                TemplateMessage templateMessage = new TemplateMessage("Carousel alt text", carouselTemplate);
                this.reply(replyToken, templateMessage);
                break;
            }*/
            /*case "image_carousel": {
                String imageUrl = createUri("/static/buttons/1040.jpg");
                ImageCarouselTemplate imageCarouselTemplate = new ImageCarouselTemplate(
                        Arrays.asList(
                                new ImageCarouselColumn(imageUrl,
                                        new URIAction("Goto line.me",
                                                "https://line.me")
                                ),
                                new ImageCarouselColumn(imageUrl,
                                        new MessageAction("Say message",
                                                "Rice=米")
                                ),
                                new ImageCarouselColumn(imageUrl,
                                        new PostbackAction("言 hello2",
                                                "hello こんにちは",
                                                "hello こんにちは")
                                )
                        ));
                TemplateMessage templateMessage = new TemplateMessage("ImageCarousel alt text", imageCarouselTemplate);
                this.reply(replyToken, templateMessage);
                break;
            }*/
            case "妞":
                int numberOfPokersForNiuniu = 5;
                Niuniu niuniu = new Niuniu(numberOfPokersForNiuniu);
                this.reply(replyToken, createPokerMessage(numberOfPokersForNiuniu, niuniu));
                break;

            case "發":
                int numberOfPokersForBigOne = 1;
                BigOne bigOne = new BigOne(numberOfPokersForBigOne);
                this.reply(replyToken, createPokerMessage(numberOfPokersForBigOne, bigOne));
                break;

            case "抽大奶":
            case "18抽":
            case "抽18":
            case "脫":
                Random random18 = new Random();
                String image18Uri;
                WEB_SITES_18 who18 = WEB_SITES_18.values()[random18.nextInt(WEB_SITES_18.values().length)];
                System.out.println("抽 website 18 : " + who18.name());
                switch (who18) {

                    case bbs_tw:
                        //自拍寫真區｜洪爺色情網
                        String bbs_tw = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Fbbs.bbs-tw.com%2Frss-4_people.xml&count=20&hours=22&backfill=true&boostMustRead=true&unreadOnly=false&ck="+getTimestamp()+"&ct=feedly.desktop&cv=30.0.1408";
                        List<String> bbs_twSet = runCommonFeedParser(bbs_tw, 2);
                        image18Uri = bbs_twSet.get(random18.nextInt(bbs_twSet.size()));
                        break;

                    case plus28:
                    default:
                        Map<String, String> cookies = loginPlus28();

                        String USER_AGENT = "User-Agent";
                        String USER_AGENT_VALUE = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36";
                        List<String> image18UriSet = new ArrayList<String>();
                        Random randomFor18 = new Random();
                /*String[] plus2818Set = {"https://www.plus28.com/rss.php?fid=826&auth=119dpFYXjLHpaDPhkFevCnMAtbvt2tbDppWqDMuJBzTIXHsF7IEvzUXaWJBBUtkoVw",
                        "https://www.plus28.com/rss.php?fid=1188&auth=bc9fR%2FT59V6zEoU%2BjUzx6dY3XnAFe981p%2FrQe8mTKyTnUZ2qR1HxB4P6Zpq5p3ueCTo",
                        "https://www.plus28.com/rss.php?fid=445&auth=ffc5a6AxTTHvL7EUpuQL34NX%2FgOdDivSBRZb9TEdLBoOGiT5OJ8JWQG0t4gPL9S5CQ",
                        "https://www.plus28.com/rss.php?fid=1283&auth=2d271zg5CEehb1q9aRu5osAQx45Ogw1dIRhmF7qI3hgPWwLXGv%2FZ9BQnWixVFy1HR4o",
                        "https://www.plus28.com/rss.php?fid=1286&auth=e4efX98trILBTjq39QV97u8kgp7NDh%2Bmf1lr0QPkxlViv2eth9yChW6CcanpNr6mIZY",
                        "https://www.plus28.com/rss.php?fid=1074&auth=50f1BzHQrQB2P5MRqeph77zsy%2F5UU2t9bdz1bXV%2Blk3pBuVu02iSOBlGK0w%2B6%2FFz3%2BY",
                        "https://www.plus28.com/rss.php?fid=250&auth=6fe9vJm9oH7ZzqEsl8wQ3ogeRKfmIh00QKT4Kwom%2Bv0v4D3xFM%2BnTSTakhasqCS4cA",
                        "https://www.plus28.com/rss.php?fid=249&auth=9a80polbblwzR1snvpSz6TB3kvPMic9aHfIsxyPJ3jSz0v%2FKEyCgbzbNzFoFA8WqXA"};*/
                        String[] plus2818Set = {"https://www.plus28.com/rss.php?fid=250&auth=ee83PM4DVkRVHxYG4Zvc6EbUdI4gdEjiSC4tQ1QgWekeXXaEG%2Bu5wZntnjcg5JxpLA",
                                "https://www.plus28.com/rss.php?fid=1283&auth=077ctBahMudv5MaXQj6F1lBziYn28e6uCblaV6m4JSVLDAiLA3HTNP2aAD5QKS6DZF0"};
                        String plus2818Url =  plus2818Set[0];

                        if (text.equals("抽大奶")) {
                            plus2818Url =  plus2818Set[1];
                        }
                        Document plus2818Doc = Jsoup.connect(plus2818Url).get();
                        Elements plus2818ItemSet = plus2818Doc.select("item");
                        Elements plus2818LinkSet = plus2818ItemSet.select("link");
                        String plus2818Link = plus2818LinkSet.get(randomFor18.nextInt(plus2818LinkSet.size())).text();

                        Connection con3 = Jsoup.connect(plus2818Link);
                        con3.header(USER_AGENT, USER_AGENT_VALUE);
                        con3.header("origin", "https://plus28.com");
                        con3.header("referer", "https://plus28.com/logging.php?action=login");
                        con3.header("upgrade-insecure-requests", "1");
                        // 设置cookie和post上面的map数据
                        Connection.Response imageResponse = con3.ignoreContentType(true).followRedirects(true).method(Connection.Method.GET)
                                .cookies(cookies).execute();
                        Document plus2818ImgDoc = imageResponse.parse();
                        Elements plus2818ImgDivSet = plus2818ImgDoc.select("div[class=t_msgfont]");
                        Elements plus2818ImgSet = plus2818ImgDivSet.get(0).select("img[src$=.jpg]");

                        for (Element plus28Img : plus2818ImgSet) {
                            image18UriSet.add(plus28Img.attr("src"));
                        }
                        int image18Number = randomFor18.nextInt(image18UriSet.size()) ;

                        image18Uri = image18UriSet.get(image18Number);
                        if (!image18Uri.contains("https")) {
                            image18Uri = image18Uri.replaceFirst("http", "https");
                        }
                        break;
                }

                this.reply(replyToken, new ImageMessage(image18Uri, image18Uri));
                break;
            case "抓":
                Document meetAvDoc = Jsoup.connect(String.format("http://www.meetav.com/?page=%d", new Random().nextInt(2000))).get();
                Elements meetAvElements = meetAvDoc.select("h2[class=title fulltitle]");
                List<String> meetAVUrl = new ArrayList<String>();
                List<String> meetAVTitle = new ArrayList<String>();
                List<String> meetAVImage = new ArrayList<String>();

                for (Element e: meetAvElements) {
                    meetAVTitle.add(e.text());
                    meetAVUrl.add(e.select("a[lang=zh-Hant]").attr("href"));
                }

                meetAvElements = meetAvDoc.select("div[style=display:none;]");
                for (Element e : meetAvElements) {
                    meetAVImage.add("https://" + e.select("span").get(0).text());
                }

                int numberOfMeetAV = 5;
                List<String> realMeetAVLink = new ArrayList<String>();
                List<String> realMeetAVTitle = new ArrayList<String>();
                List<String> realMeetAVImage = new ArrayList<String>();

                for (int i = 0; i < numberOfMeetAV; i++) {
                    int meetAVLuckyNum = new Random().nextInt(20);
                    meetAvElements = Jsoup.connect(meetAVUrl.get(meetAVLuckyNum)).get().select("script[type=text/javascript]");
                    for (Element e : meetAvElements) {
                        String patternStr = "var\\shq_video_file\\s=\\s'(.*)'";
                        Pattern pattern = Pattern.compile(patternStr);
                        Matcher matcher = pattern.matcher(e.toString());
                        boolean matchFound = matcher.find();
                        if (matchFound) {
                            realMeetAVLink.add(matcher.group(1));
                            realMeetAVTitle.add(meetAVTitle.get(meetAVLuckyNum).length() >= 12 ?
                                    meetAVTitle.get(meetAVLuckyNum).substring(0,11) : meetAVTitle.get(meetAVLuckyNum));
                            realMeetAVImage.add(meetAVImage.get(meetAVLuckyNum));
                            break;
                        }
                    }
                }

                    /*meetAvElements = Jsoup.connect(meetAVUrl.get(meetAVLuckyNum)).get().select("script[type=text/javascript]");
                    for (Element e : meetAvElements) {
                        String patternStr = "var\\shq_video_file\\s=\\s'(.*)'";
                        Pattern pattern = Pattern.compile(patternStr);
                        Matcher matcher = pattern.matcher(e.toString());
                        boolean matchFound = matcher.find();
                        if (matchFound) {
                            meetAVMp4 = matcher.group(1);
                            break;
                        }
                    }*/
                ArrayList<ImageCarouselColumn> meetAVCarouselColumns = new ArrayList<>();
                for (int i = 0; i < numberOfMeetAV; i++) {
                    meetAVCarouselColumns.add(
                            new ImageCarouselColumn(meetAVImage.get(i),
                                    new URIAction(realMeetAVTitle.get(i), realMeetAVLink.get(i))));
                }

                ImageCarouselTemplate meetAVCarouselTemplate = new ImageCarouselTemplate(
                        meetAVCarouselColumns);

                this.reply(replyToken, new TemplateMessage("ImageCarousel alt text", meetAVCarouselTemplate));

//                this.reply(replyToken, Arrays.asList(new TextMessage(meetAVTitle.get(meetAVLuckyNum)), new TextMessage(meetAVMp4)));
                break;

            case "抽":
                List<String> imageUriSet = new ArrayList<>();
                Random random = new Random();

                WEB_SITES who = WEB_SITES.values()[random.nextInt(WEB_SITES.values().length)];
                System.out.println("抽 website : " + who.name());
                switch (who) {
                    //jpBeautyHouse
                    /*case jpBeautifyHouse:
                        Document jpDoc = Jsoup.connect("http://jpbeautyhouse.blogspot.com/feeds/posts/default").get();
                        Elements jpBeautyHouseSet = jpDoc.select("content");
                        for (Element jpBeautyHouse : jpBeautyHouseSet) {
                            Document elementDoc = Jsoup.parse(jpBeautyHouse.text());
                            Elements elements = elementDoc.select("div");
                            for (Element e : elements) {
                                Elements elements1 = e.select("img");
                                for (Element e1 : elements1) {
                                    imageUriSet.add(e1.attr("src"));
                                }
                            }
                        }
                        break;

                    case plus28:
                        String[] plus28Set = {"https://www.plus28.com/rss.php?fid=1112&auth=d84eSWET9kKraQGfHm9F2shgsTffgV2RR7LcVr83KC3eqYqL30YXrufJ7vCwVj9VXhk",
                                "https://www.plus28.com/rss.php?fid=52&auth=f084bqckg3mcy1DtaSopQh3lTbYVJ1tymAx%2FiOMy%2BT0Jks1BtSsw8IAa2INJ5OhD",
                                "https://www.plus28.com/rss.php?fid=165&auth=7359GpiGkkIW8y2Hzu2Fx7Gs4RbJSXcLRlnok3jxI2oeKr2gvmGhuXpIvKJI84kx5A"};

                        Document plus28Doc = Jsoup.connect(plus28Set[random.nextInt(plus28Set.length)]).get();
                        Elements plus28ItemSet = plus28Doc.select("item");
                        Elements plus28LinkSet = plus28ItemSet.select("link");
                        String plus28Link = plus28LinkSet.get(random.nextInt(plus28LinkSet.size())).text();
                        Document plus28ImgDoc = Jsoup.connect(plus28Link).get();
                        Elements plus28ImgDivSet = plus28ImgDoc.select("div[class=t_msgfont]");
                        Elements plus28ImgSet = plus28ImgDivSet.get(0).select("img[src$=.jpg]");
                        for (Element plus28Img : plus28ImgSet) {
                            imageUriSet.add(plus28Img.attr("src"));
                        }
                        break;*/

                    /*case gigacircle_32:
                        //GigaCircle - 日韓美女
                        String gigacircle32MainUrl = String.format("http://tw.gigacircle.com/s32-%d", new Random().nextInt(10));
                        System.out.println("gigacircle32MainUrl : " + gigacircle32MainUrl);
                        Document gigaCircle32Doc = Jsoup.connect(gigacircle32MainUrl).get();
                        List<String> gigaCircle32LinkSet = new ArrayList<>();

                        for (Element e : gigaCircle32Doc.select("div[class=thumbs]")) {
                            gigaCircle32LinkSet.add(e.select("a").attr("href"));
                        }
                        String gigaCircle32ThumbLink = gigaCircle32LinkSet.get(new Random().nextInt(gigaCircle32LinkSet.size()));
                        System.out.println("gigaCircle32ThumbLink : " + gigaCircle32ThumbLink);
                        gigaCircle32Doc = Jsoup.connect(gigaCircle32ThumbLink).get();
                        gigaCircle32LinkSet.clear();
                        for (Element e : gigaCircle32Doc.select("div[id=gallery]")) {
                            for (Element se : e.select("img")) {
                                gigaCircle32ThumbLink = se.attr("data-original");
                                if (gigaCircle32ThumbLink.isEmpty()) {
                                    continue;
                                }
                                if (!gigaCircle32ThumbLink.contains("https")) {
                                    gigaCircle32ThumbLink = gigaCircle32ThumbLink.replace("http", "https");
                                }
                                imageUriSet.add(gigaCircle32ThumbLink);
                            }
                        }

                        imageUriSet.addAll(getGigaCirclePicMap(String.format("http://tw.gigacircle.com/s32-%d", new Random().nextInt(10))));
                        break;*/

                    case gigacircle_31:
                    default:
                        //GigaCircle - 正妹美眉
                        String gigacircle31MainUrl = String.format("http://tw.gigacircle.com/s31-%d", new Random().nextInt(10));
                        System.out.println("gigacircle31MainUrl : " + gigacircle31MainUrl);
                        Document gigaCircle31Doc = Jsoup.connect(gigacircle31MainUrl).get();
                        List<String> gigaCircle31LinkSet = new ArrayList<String>();
                        for (Element e : gigaCircle31Doc.select("div[class=thumbs]")) {
                            gigaCircle31LinkSet.add(e.select("a").attr("href"));
                        }
                        String gigaCircle31ThumbLink = gigaCircle31LinkSet.get(new Random().nextInt(gigaCircle31LinkSet.size()));
                        System.out.println("gigacircle_31_link : " + gigaCircle31ThumbLink);
                        gigaCircle31Doc = Jsoup.connect(gigaCircle31ThumbLink).get();
                        gigaCircle31LinkSet.clear();
                        for (Element e : gigaCircle31Doc.select("div[id=gallery]")) {
                            for (Element se : e.select("img")) {
                                gigaCircle31ThumbLink = se.attr("data-original");
                                if (gigaCircle31ThumbLink.isEmpty()) {
                                    continue;
                                }
                                if (!gigaCircle31ThumbLink.contains("https")) {
                                    gigaCircle31ThumbLink = gigaCircle31ThumbLink.replace("http", "https");
                                }
                                imageUriSet.add(gigaCircle31ThumbLink);
                            }
                        }
                        break;

                    /*case hkCom://discuss.com.hk
                        Document hkDoc = Jsoup.connect("http://www.discuss.com.hk/archiver/?fid-140.html").get();
                        Elements hkElements = hkDoc.select("li").select("a");
                        Random hkRandom = new Random();
                        List<String> hkUrlSet = new ArrayList<String>();
                        for (int i = 0; i < hkElements.size(); i++) {
                            if (i <= 6) {
                                continue;
                            }
                            String hkUrl = hkElements.get(i).attr("href");
                            String hkId = hkUrl.substring(hkUrl.indexOf("-") + 1, hkUrl.lastIndexOf("."));

                            hkUrlSet.add("http://www.discuss.com.hk/viewthread.php?tid="+hkId+"&extra=page%3D1");
                        }
                        String visitHKUrl = hkUrlSet.get(hkRandom.nextInt(hkUrlSet.size()));
                        System.out.println(visitHKUrl + "\n\n");
                        hkDoc = Jsoup.connect(visitHKUrl).get();
                        hkElements = hkDoc.select("div[class=post-img-container]");

                        for (Element hkElement : hkElements) {
                            String hkElementContent = hkElement.html();
                            hkElementContent = hkElementContent.substring(hkElementContent.indexOf("<img src="));
                            hkElementContent = hkElementContent.substring(hkElementContent.indexOf("\"") + 1);
                            hkElementContent = hkElementContent.substring(0, hkElementContent.indexOf("\""));

                            if (!hkElementContent.contains("https")) {
                                hkElementContent = hkElementContent.replace("http", "https");
                            }
                            imageUriSet.add(hkElementContent);
                        }
                        break;*/

                    /*case ck101:
                        //ck101
                        Document ck101Doc = Jsoup.connect("https://ck101.com/forum.php?mod=rss&fid=1345&auth=0").get();
                        Elements ck101LinkSet = ck101Doc.select("item");
                        List<String> links = new ArrayList<String>();
                        for (Element ck101Link : ck101LinkSet) {
                            links.add(ck101Link.select("link").text());
                        }

                        if (links.size() > 0) {
                            int ck101Num = random.nextInt(links.size());
                            String link = links.get(ck101Num);
                            Document ck101SubDoc = Jsoup.connect(link).get();
                            Elements imageSet = ck101SubDoc.select("div[class=article_plc_user]");
                            for (Element e : imageSet) {
                                Elements ck101Set = e.select("img[file]");
                                for (Element e1 : ck101Set) {
                                    imageUriSet.add(e1.attr("file"));
                                }
                            }
                        }
                        break;*/

                    /*case news_gamme: //news.gamme 卡卡洛普
                        String newsGammeUrl = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Fnews.gamme.com.tw%2Fcategory%2Fhotchick%2Ffeed&count=20&hours=22&backfill=true&boostMustRead=true&unreadOnly=false&ck="+getTimestamp()+"&ct=feedly.desktop&cv=30.0.1403";
                        imageUriSet.addAll(runCommonFeedParser(newsGammeUrl, 1));
                        break;*/

                    /*case rosiyy:
                        String rosiyy = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Fwww.rosiyy.com%2Ffeed%2F&count=20&hours=23&backfill=true&boostMustRead=true&unreadOnly=false&ck="+getTimestamp()+"&ct=feedly.desktop&cv=30.0.1403";
                        imageUriSet.addAll(runCommonFeedParser(rosiyy, 2));
                        break;

                    case k163k163:
                        //美女画像
                        String k163k163 = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Fk163k163.tumblr.com%2Frss&count=20&hours=22&backfill=true&boostMustRead=true&unreadOnly=false&ck="+getTimestamp()+"&ct=feedly.desktop&cv=30.0.1408";
                        imageUriSet.addAll(runCommonFeedParser(k163k163, 2));
                        break;

                    //Beautyleg腿模高清美腿写真套图
                    case beautify_leg:
                        String beautyLeg = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Fwww.beautylegmm.com%2Ffeed%2F&count=20&hours=23&backfill=true&boostMustRead=true&unreadOnly=false&ck="+getTimestamp()+"&ct=feedly.desktop&cv=30.0.1403";
                        imageUriSet.addAll(runCommonFeedParser(beautyLeg, 2));
                    break;

                    case ookkk: //"正妹星球!! ♥ 就是愛正妹"
                        String ookkk = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Ffeeds.feedburner.com%2Fookkk&count=20&hours=23&backfill=true&boostMustRead=true&unreadOnly=false&ck="+getTimestamp()+"&ct=feedly.desktop&cv=30.0.1408";
                        imageUriSet.addAll(runCommonFeedParser(ookkk, 2));
                        break;

                    case sina_poppy: //□■□□妖色
                        String sina_poppy = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Fblog.sina.com.cn%2Frss%2F1579071145.xml&count=20&hours=23&backfill=true&boostMustRead=true&unreadOnly=false&ck="+getTimestamp()+"&ct=feedly.desktop&cv=30.0.1408";
                        imageUriSet.addAll(runCommonFeedParser(sina_poppy, 3));
                        break;*/

                    /*case forum_gamme: //forum 聊天事 - 正妹研究所
                        String forumGammeUrl = "https://feedly.com/v3/streams/contents?streamId=feed%2Fhttp%3A%2F%2Fforum.gamme.com.tw%2Fforum.php%3Fmod%3Drss%26fid%3D2%26auth%3D0&count=20&unreadOnly=false&ranked=newest&similar=true&ck="+getTimestamp()+"&ct=feedly.desktop&cv=30.0.1403";
                        imageUriSet.addAll(runCommonFeedParser(forumGammeUrl, 1));
                        break;*/

                   /* case ptt_beauty: //PTT BEAUTIFY
                        Document ptt_beautify_doc = Jsoup.connect("http://feed43.com/getbeaktyurl.xml").get();
                        Elements ptt_beautifyElements = ptt_beautify_doc.select("item");
                        List<String> ptt_beauty_url_set = new ArrayList<String>();
                        for (Element ptt_beautifyElement : ptt_beautifyElements) {
                            ptt_beauty_url_set.add(ptt_beautifyElement.select("link").text());
                        }
                        String ptt_beauty_pic_url = ptt_beauty_url_set.get(new Random().nextInt(ptt_beauty_url_set.size() - 5));
                        ptt_beautify_doc = Jsoup.connect(ptt_beauty_pic_url).get();
                        ptt_beautifyElements = ptt_beautify_doc.select("a[rel=nofollow]");
                        for (Element ptt_beautifyElement : ptt_beautifyElements) {
                            if (ptt_beautifyElement.html().contains(".jpg")){
                                imageUriSet.add(ptt_beautifyElement.html());
                            }
                        }
                        break;*/
                    /*case voc:
                        //http://bbs.voc.com.cn/
                        Document vocDoc = Jsoup.connect("http://bbs.voc.com.cn/rss.php?fid=50").get();
                        Elements vocItemSet = vocDoc.select("item");
                        Elements vocLinkSet = vocItemSet.select("link");
                        String vocLink = vocLinkSet.get(random.nextInt(vocLinkSet.size())).text();

                        Document vocImgDoc = Jsoup.connect(vocLink).get();
                        Elements vocImgDivSet = vocImgDoc.select("div[class=t_msgfont BSHARE_POP BSHARE_IMAGE_CLASS]");
                        for (Element vocImg : vocImgDivSet) {
                            imageUriSet.add(vocImg.select("img[src$=.jpg]").attr("src").replaceFirst("http", "https"));
                        }
                        break;*/
                }
                int imageNumber = random.nextInt(imageUriSet.size());
                String imageUri = imageUriSet.get(imageNumber);
                if (!imageUri.contains("https")) {
                    imageUri = imageUri.replaceFirst("http", "https");
                }
                this.reply(replyToken, new ImageMessage(imageUri, imageUri));
                break;

            case "!av":
            case "抓av":
            case "thisav":
            case "看":
                Random thisAVRandom = new Random();
                int randomThisAV = thisAVRandom.nextInt(10);
                Document vocDoc = Jsoup.connect(String.format("http://www.thisav.com/videos?o=mr&type=&c=0&t=a&page=%d",randomThisAV)).get();
                Elements vocItemSet = vocDoc.select("div[class=video_box]");
                int totalThisAV = 4;
                ArrayList<String> thisAvLink = new ArrayList<>();
                ArrayList<String> thisAvTitle = new ArrayList<>();
                ArrayList<Integer> thisAvRandomNumber = new ArrayList<>();
                for (int i = 0; i < totalThisAV; i++) {
                    while (true) {
                        randomThisAV = thisAVRandom.nextInt(vocItemSet.size());
                        if (thisAvRandomNumber.contains(randomThisAV)) {
                            continue;
                        } else {
                            break;
                        }
                    }
                    thisAvRandomNumber.add(randomThisAV);
                    thisAvLink.add(vocItemSet.get(randomThisAV).select("a[href]").attr("href"));
                    String title = vocItemSet.get(randomThisAV).select("img[src]").attr("title");
                    thisAvTitle.add(title.length() > 20 ? title.substring(0, 19) : title);
                }

                ButtonsTemplate thisAVCarouselTemplate = new ButtonsTemplate(
                        "https://cdn.thisav.com/images/grey-pink/logo.png",
                        "AV Link",
                        "按下面連結播放",
                        Arrays.asList(
                                new URIAction(thisAvTitle.get(0),
                                        thisAvLink.get(0)),
                                new URIAction(thisAvTitle.get(1),
                                        thisAvLink.get(1)),
                                new URIAction(thisAvTitle.get(2),
                                        thisAvLink.get(2)),
                                new URIAction(thisAvTitle.get(3),
                                        thisAvLink.get(3))
                        ));
                TemplateMessage thisAVTemplateMessage = new TemplateMessage("Button alt text", thisAVCarouselTemplate);
                this.reply(replyToken, thisAVTemplateMessage);
                break;

            /*case "洋妞":
                //pexels.com
                List<String> pexelsImageUriSet = new ArrayList<>();
                Random pexelsRandom = new Random();
                String pexelsPageNum = String.format("page=%d", pexelsRandom.nextInt(100));
                Document pexelsDoc = Jsoup.connect("https://www.pexels.com/search/beautiful%20girl/?"+pexelsPageNum).get();
                System.out.println( "https://www.pexels.com/search/beautiful%20girl/?"+pexelsPageNum + "\n\n");
                Elements pexelsElements = pexelsDoc.select("div[class=photos]").select("a");
                for (Element pexelElement : pexelsElements) {
                    pexelsImageUriSet.add(pexelElement.select("img").attr("src"));
                }
                int pexelsImageNumber = pexelsRandom.nextInt(pexelsImageUriSet.size());
                String pexelsImageUri = pexelsImageUriSet.get(pexelsImageNumber);
                if (!pexelsImageUri.contains("https")) {
                    imageUri = pexelsImageUri.replaceFirst("http", "https");
                }
                this.reply(replyToken, new ImageMessage(pexelsImageUri, pexelsImageUri));
                break;*/

           /* case "抓":
                Document xvideosDoc = Jsoup.connect("https://www.xvideos.com/rss/rss.xml").get();
                Elements xvideoUrlSet = xvideosDoc.select("flv_embed");
                List<String> xvideos = new ArrayList<>();
                for (Element xvideo : xvideoUrlSet) {
                    String html = xvideo.html();
                    String result = html.substring(html.indexOf("\"") + 1, html.lastIndexOf("\""));
                    xvideos.add(result);
                }


                Elements xvideoThumbSet = xvideosDoc.select("thumb_big");
                List<String> thumbs = new ArrayList<>();
                for (Element thumb : xvideoThumbSet) {
                    thumbs.add(thumb.text());
                }
                Random rand = new Random();
                int randNum = rand.nextInt(xvideos.size());
                String url = thumbs.get(randNum).replace("http", "https");
                String videoUrl = xvideos.get(randNum);

                ArrayList<ImageCarouselColumn> imageCarouselColumns = new ArrayList<>();
                for (int i = 0; i < 1; i++) {
                    imageCarouselColumns.add(
                            new ImageCarouselColumn(url,
                                    new URIAction("點一下開始播放", videoUrl)));
                }

                ImageCarouselTemplate imageCarouselTemplate = new ImageCarouselTemplate(
                        imageCarouselColumns);

                this.reply(replyToken, new TemplateMessage("ImageCarousel alt text", imageCarouselTemplate));
                break;*/

            case "moto":
            case "機車":
            case "騎":
                List<String> imageUriMotoSet = new ArrayList<>();
                Random motoRandom = new Random();

                WEB_SITES_MOTO whoMoto = WEB_SITES_MOTO.values()[motoRandom.nextInt(WEB_SITES_MOTO.values().length)];

                System.out.println("moto website : " + whoMoto.name());
                switch (whoMoto) {
                    case cafeRacers://Return of the Cafe Racers
                        String cafeRacers = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Fcaferacersreturn.blogspot.com%2Ffeeds%2Fposts%2Fdefault&count=20&hours=23&backfill=true&boostMustRead=true&unreadOnly=false&ck="+ getTimestamp() +"&ct=feedly.desktop&cv=30.0.1408";
                        imageUriMotoSet.addAll(runCommonFeedParser(cafeRacers, 1));
                        break;

                    case canadaMoto://Canada Moto Guide
                        String canadaMotoGP = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Fcanadamotoguide.com%2Ffeed%2F&count=20&hours=23&backfill=true&boostMustRead=true&unreadOnly=FALSE&ck="+ getTimestamp() +"&ct=feedly.desktop&cv=30.0.1408";
                        imageUriMotoSet.addAll(runCommonFeedParser(canadaMotoGP, 1));
                        break;

                    case newMotoGP://news RSS on motogp.com
                        String newMotoGP = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Fwww.motogp.com%2Fen%2Fnews%2Frss&count=20&hours=23&backfill=true&boostMustRead=true&unreadOnly=false&ck="+ getTimestamp() +"&ct=feedly.desktop&cv=30.0.1408";
                        imageUriMotoSet.addAll(runCommonFeedParser(newMotoGP, 2));
                        break;

                    case motoGP://MotoGP
                    default:
                        String motoGP = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Fwww.autosport.com%2Frss%2Fmotogpnews.xml&count=20&hours=23&backfill=true&boostMustRead=true&unreadOnly=false&ck=" + getTimestamp() + "&ct=feedly.desktop&cv=30.0.1408";
                        imageUriMotoSet.addAll(runCommonFeedParser(motoGP, 1));
                        break;
                }
                int imageMotoNumber = motoRandom.nextInt(imageUriMotoSet.size());
                String motoImageUri = imageUriMotoSet.get(imageMotoNumber);
                if (!motoImageUri.contains("https")) {
                    motoImageUri = motoImageUri.replaceFirst("http", "https");
                }
                this.reply(replyToken, new ImageMessage(motoImageUri, motoImageUri));
                break;

            case "car":
            case "汽車":
            case "開":
                List<String> imageUriCarSet = new ArrayList<>();
                Random carRandom = new Random();

                WEB_SITES_CAR whoCar = WEB_SITES_CAR.values()[carRandom.nextInt(WEB_SITES_CAR.values().length)];

                System.out.println("moto website : " + whoCar.name());
                switch (whoCar) {
                    case autoblog://Autoblog
                        String autoblog = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Fwww.autoblog.com%2Frss.xml&count=20&hours=23&backfill=true&boostMustRead=true&unreadOnly=false&ck="+ getTimestamp() +"&ct=feedly.desktop&cv=30.0.1408";
                        imageUriCarSet.addAll(runCommonFeedParser(autoblog, 1));
                        break;

                    case autocar_rss://Autocar RSS Feed
                        String autocarrss = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Fwww.autocar.co.uk%2Frss%2Flatestnews%2F&count=20&hours=23&backfill=true&boostMustRead=true&unreadOnly=true&ck="+ getTimestamp() +"&ct=feedly.desktop&cv=30.0.1408";
                        imageUriCarSet.addAll(runCommonFeedParser(autocarrss, 1));
                        break;

                    case bmw_blog://BMW BLOG
                        String bmw_blog = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Ffeeds.feedburner.com%2FBmwBlog&count=20&hours=23&backfill=true&boostMustRead=true&unreadOnly=false&ck="+ getTimestamp() +"&ct=feedly.desktop&cv=30.0.1408";
                        imageUriCarSet.addAll(runCommonFeedParser(bmw_blog, 2));
                        break;

                    case high_gear_media://High Gear Media Network Feed
                        String high_gear_media = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Ffeeds.feedburner.com%2FMotorAuthority2&count=20&hours=0&backfill=true&boostMustRead=true&unreadOnly=false&ck="+ getTimestamp() +"&ct=feedly.desktop&cv=30.0.1408";
                        imageUriCarSet.addAll(runCommonFeedParser(high_gear_media, 1));
                        break;

                    case latest://Latest
                        String latest = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Fwww.autoexpress.co.uk%2Frss%2Fnews%2F&count=20&hours=0&backfill=true&boostMustRead=true&unreadOnly=false&ck="+ getTimestamp() +"&ct=feedly.desktop&cv=30.0.1408";
                        imageUriCarSet.addAll(runCommonFeedParser(latest, 2));
                        break;

                    case truth_car://The Truth About Cars
                        String truth_car = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Fwww.thetruthaboutcars.com%2F%3Ffeed%3Drss2&count=20&hours=0&backfill=true&boostMustRead=true&unreadOnly=false&ck="+ getTimestamp() +"&ct=feedly.desktop&cv=30.0.1408";
                        imageUriCarSet.addAll(runCommonFeedParser(truth_car, 2));
                        break;

                    case driver_blog://Car and Driver BlogCar and Driver Blog
                    default:
                        String driver_blog = "https://feedly.com/v3/mixes/contents?streamId=feed%2Fhttp%3A%2F%2Ffeeds.feedburner.com%2Fcaranddriver%2Fblog&count=20&hours=23&backfill=true&boostMustRead=true&unreadOnly=false&ck="+ getTimestamp() +"&ct=feedly.desktop&cv=30.0.1408";
                        imageUriCarSet.addAll(runCommonFeedParser(driver_blog, 1));
                        break;
                }
                int imageCarNumber = carRandom.nextInt(imageUriCarSet.size());
                String carImageUri = imageUriCarSet.get(imageCarNumber);
                if (!carImageUri.contains("https")) {
                    carImageUri = carImageUri.replaceFirst("http", "https");
                }
                this.reply(replyToken, new ImageMessage(carImageUri, carImageUri));
                break;

            case "魔羯":
            case "摩羯":
            case "射手":
            case "天蠍":
            case "天秤":
            case "處女":
            case "獅子":
            case "巨蟹":
            case "雙子":
            case "金牛":
            case "牡羊":
            case "雙魚":
            case "水瓶"://星座
                int constellationNum = CONSTELLATION.value(text);
                String whichConstellation = String.format("http://astro.click108.com.tw/daily_%d.php?iAstro=%d",constellationNum, constellationNum);
                Document constellationDoc = Jsoup.connect(whichConstellation).get();
                Elements constellationElements = constellationDoc.select("div[class=TODAY_CONTENT]");
                if (constellationElements.size() > 0) {
                    Element constellationElement = constellationElements.get(0);
                    String constellationResult = constellationElement.text().replaceAll(" ", "\n\n").toString();
                    this.reply(replyToken, new TextMessage(constellationResult));
                }
                break;
            case "動":
                String[] youtubeChannelList = {
                        "https://www.youtube.com/feeds/videos.xml?channel_id=UCb8GewmyomdoQiu2-VGP8PQ", //正妹直播福利站
                        "https://www.youtube.com/feeds/videos.xml?channel_id=UCWyKul9xGolA956GUbN-5PQ" ,//正妹本舖
                        "https://www.youtube.com/feeds/videos.xml?channel_id=UCKpk92zYWMgGavWbSIHi-HQ" ,//正妹珍藏網
                        "https://www.youtube.com/feeds/videos.xml?channel_id=UCXRzp3nMjrUmAqWFCHl4S_Q"//extrating
                };
                ArrayList<String> VGP8PQURLList = fetchYoutubeIDList(youtubeChannelList[new Random().nextInt(youtubeChannelList.length)]);
                String VGP8PQURL = "https://youtu.be/" + VGP8PQURLList.get(new Random().nextInt(VGP8PQURLList.size()));
                this.reply(replyToken, new TextMessage(VGP8PQURL));
                break;

            case "sg": //百大SG 頻道
                ArrayList<String> sgList = fetchYoutubeIDList("https://www.youtube.com/feeds/videos.xml?channel_id=UCD0RVDhw9LbLy2RB36DWvbA");
                String sgURL = "https://youtu.be/" + sgList.get(new Random().nextInt(sgList.size()));
                this.reply(replyToken, new TextMessage(sgURL));
                break;

            case "運動":
            case "健身": //美女健身聯盟
                ArrayList<String> exerciseList = fetchYoutubeIDList("https://www.youtube.com/feeds/videos.xml?channel_id=UC_rifxahjOOKTOBsyT9ZPSA");
                String exerciseURL = "https://youtu.be/" + exerciseList.get(new Random().nextInt(exerciseList.size()));
                this.reply(replyToken, new TextMessage(exerciseURL));
                break;

            case "一日":
            case "木曜4超玩":
                this.reply(replyToken, new TextMessage(fetchYoutubeRss("https://www.youtube.com/feeds/videos.xml?channel_id=UCLW_SzI9txZvtOFTPDswxqg")));
                break;

            case "行車紀錄趣":
                this.reply(replyToken, new TextMessage(fetchYoutubeRss("https://www.youtube.com/feeds/videos.xml?channel_id=UCeV8mGk7CodtelsRNjgfg3w")));
                break;

            case "buycartv":
                this.reply(replyToken, new TextMessage(fetchYoutubeRss("https://www.youtube.com/feeds/videos.xml?channel_id=UC-MByr5LRnWmVN94Ija4XJg")));
                break;

            case "android":
            case "android developer":
                this.reply(replyToken, new TextMessage(fetchYoutubeRss("https://www.youtube.com/feeds/videos.xml?channel_id=UCVHFbqXqoYvEWM1Ddxl0QDg")));
                break;

            case "蔡阿嘎":
                this.reply(replyToken, new TextMessage(fetchYoutubeRss("https://www.youtube.com/feeds/videos.xml?channel_id=UCPwxSX0DYDMQxCvgfeVDv_g")));
                break;

            case "國光幫幫忙":
                this.reply(replyToken, new TextMessage(fetchYoutubeRss("https://www.youtube.com/feeds/videos.xml?channel_id=UCNz2YbRPdGvyBq5qq7iqNwQ")));
                break;

            case "這群人":
                this.reply(replyToken, new TextMessage(fetchYoutubeRss("https://www.youtube.com/feeds/videos.xml?channel_id=UC6FcYHEm7SO1jpu5TKjNXEA")));
                break;

            case "!youtube":
                StringBuffer youtubeList = new StringBuffer();
                youtubeList.append("[Youtube 清單]\n");
                for (YOUTUBE_LIST youtube : YOUTUBE_LIST.values()) {
                    youtubeList.append(youtube.name() + "\n");
                }
                this.reply(replyToken, new TextMessage(youtubeList.toString()));
                break;

            case "抓寶"://pockmon go pic
                String pockmonUrl = String.format("http://www.otaku-hk.com/pkmgo/pokemon/%d", new Random().nextInt(250) + 1);
                Document ptt_beautify_doc = Jsoup.connect(pockmonUrl).get();
                pockmonUrl = ptt_beautify_doc.select("div[y=1]").select("img").attr("src").replaceFirst("http","https");
                this.reply(replyToken, new ImageMessage(pockmonUrl, pockmonUrl));
                break;
            case "每日一字":
                Document wordOfTheDay = Jsoup.connect("http://learnersdictionary.com/word-of-the-day").get();
                StringBuffer wordOfTheDayResult = new StringBuffer();
                String date = wordOfTheDay.select("div[class=for_date ld_xs_hidden]").text();
                String word = wordOfTheDay.select("span[class=hw_txt georgia_font]").text();
                String partOfSpeech = wordOfTheDay.select("div[class=fl]").get(0).text();

                wordOfTheDayResult.append(word + "  (" + partOfSpeech + ")\n\n" );

                Elements meaning = wordOfTheDay.select("div[class=midbt]");
                for (int i = 0; i < meaning.size(); i++) {
                    String[] eArr = meaning.get(i).text().split(":");
                    if (eArr.length >= 2) {
                        wordOfTheDayResult.append((i + 1) + "." + eArr[1]+"\n\n");
                    }
                }
                Elements sample = wordOfTheDay.select("li[class=vi]");
                for (Element e: sample) {
                    wordOfTheDayResult.append(e.text()+"\n");
                }
                wordOfTheDayResult.append("\n" + date + "\n");
                this.reply(replyToken, new TextMessage(wordOfTheDayResult.toString()));
                break;
            case "!help":
            case "小白":
            case "汪汪":
            case "汪":
            case "說明":
                this.reply(replyToken, getHelpMessage());
                break;

            /*case "imagemap":
                this.reply(replyToken, new ImagemapMessage(
                        createUri("/static/rich"),
                        "This is alt text",
                        new ImagemapBaseSize(1040, 1040),
                        Arrays.asList(
                                new URIImagemapAction(
                                        "https://store.line.me/family/manga/en",
                                        new ImagemapArea(
                                                0, 0, 520, 520
                                        )
                                ),
                                new URIImagemapAction(
                                        "https://store.line.me/family/music/en",
                                        new ImagemapArea(
                                                520, 0, 520, 520
                                        )
                                )
                                ,
                                new URIImagemapAction(
                                        "https://store.line.me/family/play/en",
                                        new ImagemapArea(
                                                0, 520, 520, 520
                                        )
                                ),
                                new MessageImagemapAction(
                                        "URANAI!",
                                        new ImagemapArea(
                                                520, 520, 520, 520
                                        )
                                )
                        )
                ));
                break;*/
            default:
                log.info("Returns echo message {}: {}", replyToken, text);
                /*this.replyText(
                        replyToken,
                        text
                );*/
                break;
        }
    }

    private static String fetchYoutubeRss(String youtubeUrl) throws IOException {
        Document youtubeDoc = Jsoup.connect(youtubeUrl).get();
        Elements youtubeElements = youtubeDoc.select("entry");
        StringBuffer youtubeContent = new StringBuffer();
        for (Element youtubeElement : youtubeElements) {
            youtubeContent.append(youtubeElement.select("title").text()+"\n");
            youtubeContent.append(youtubeElement.select("link[href]").attr("href")+"\n\n");
        }
        return youtubeContent.toString();
    }

    private static ArrayList<String> fetchYoutubeIDList(String youtubeUrl) throws IOException {
        Document youtubeDoc = Jsoup.connect(youtubeUrl).get();
        Elements youtubeElements = youtubeDoc.select("entry");
        ArrayList<String> youtubeVideoList = new ArrayList<>();
        for (Element youtubeElement : youtubeElements) {
            youtubeVideoList.add(youtubeElement.select("id").text().split(":")[2]);
        }
        return youtubeVideoList;
    }

    private static List<String> runCommonFeedParser(String gammeUrl, int type) throws IOException{
        List<String> imageUriSet = new ArrayList<>();
        String json = null;
        json = Jsoup.connect(gammeUrl).ignoreContentType(true).execute().body();
        JsonElement gammeJsonElement = new JsonParser().parse(json);
        JsonObject gammeJobject = gammeJsonElement.getAsJsonObject();
        JsonArray gammeJsonArray = gammeJobject.getAsJsonArray("items");
        for (JsonElement gammeElement : gammeJsonArray) {
            if (!gammeElement.getAsJsonObject().has("visual")) {
                continue;
            }

            String gammeImageUrl;
            if (type == 1) {
                gammeImageUrl = gammeElement.getAsJsonObject().getAsJsonObject("visual").get("url").toString().replaceAll("\"","");

            } else if (type == 2){

                if (!gammeElement.getAsJsonObject().getAsJsonObject("visual").has("edgeCacheUrl")) {
                    continue;
                }

                gammeImageUrl = gammeElement.getAsJsonObject().getAsJsonObject("visual").get("edgeCacheUrl").toString().replaceAll("\"","");
            } else {
                Document gammeDoc = Jsoup.parse(gammeElement.getAsJsonObject().getAsJsonObject("summary").get("content").toString());
                gammeImageUrl = gammeDoc.select("p").select("a[target]").select("img").attr("src").replaceAll("\\\\\"","");

                if (gammeImageUrl.isEmpty()) {
                    continue;
                }
            }

            if (!gammeImageUrl.contains("https")) {
                gammeImageUrl = gammeImageUrl.replace("http", "https");
            }

            if (gammeImageUrl.equalsIgnoreCase("none")) {
                continue;
            }
            imageUriSet.add(gammeImageUrl);
        }
        return imageUriSet;
    }

    private static TemplateMessage createPokerMessage(int numberOfPokers, Poker poker) {
        ArrayList<ImageCarouselColumn> imageCarouselColumns = new ArrayList<>();
        for (int i = 0; i < numberOfPokers; i++) {
            String imagePath = poker.getPath(i);
            String pokerPoint = String.valueOf(poker.getPoint(i));
            imageCarouselColumns.add(
                    new ImageCarouselColumn(createUri("/static/poker/" + imagePath + ".jpeg"),
                            new MessageAction(pokerPoint, poker.getResult())));
        }

        ImageCarouselTemplate imageCarouselTemplate = new ImageCarouselTemplate(
                imageCarouselColumns
        );

        return new TemplateMessage("ImageCarousel alt text", imageCarouselTemplate);
    }

    private static String createUri(String path) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(path).build()
                .toUriString();
    }

    private void system(String... args) {
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        try {
            Process start = processBuilder.start();
            int i = start.waitFor();
            log.info("result: {} =>  {}", Arrays.toString(args), i);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            log.info("Interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private static DownloadedContent saveContent(String ext, MessageContentResponse responseBody) {
        log.info("Got content-type: {}", responseBody);

        DownloadedContent tempFile = createTempFile(ext);
        try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
            ByteStreams.copy(responseBody.getStream(), outputStream);
            log.info("Saved {}: {}", ext, tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static DownloadedContent createTempFile(String ext) {
        String fileName = LocalDateTime.now().toString() + '-' + UUID.randomUUID().toString() + '.' + ext;
        Path tempFile = KitchenSinkApplication.downloadedContentDir.resolve(fileName);
        tempFile.toFile().deleteOnExit();
        return new DownloadedContent(
                tempFile,
                createUri("/downloaded/" + tempFile.getFileName()));
    }

    @Value
    public static class DownloadedContent {
        Path path;
        String uri;
    }

    public static Map<String, String> loginPlus28() {
        try {

            System.out.println(isRunning + " / " + plusCookies);
            if (!isRunning && plusCookies != null) {
                return plusCookies;
            }

            Connection.Response login = null;
            String USER_AGENT = "User-Agent";
            String USER_AGENT_VALUE = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36";

            String plusUserName = "kane750630";
            String plusPassword = "26892615";
            Connection con = Jsoup.connect("https://plus28.com/logging.php?action=login");//获取连接
            con.header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:29.0) Gecko/20100101 Firefox/29.0");//配置模拟浏览器
            Connection.Response rs = con.execute();//获取响应
            Document d1 = Jsoup.parse(rs.body());//转换为Dom树
            List<Element> et = d1.select("form");//获取form表单，可以通过查看页面源码代码得知
            Map<String, String> datas = new HashMap<String, String>();
            for (Element e : et.get(0).getAllElements()) {
                // 设置用户名
                if (e.attr("name").equals("username")) {
                    e.attr("value", plusUserName);
                }
                // 设置用户密码
                if (e.attr("name").equals("password")) {
                    e.attr("value", plusPassword);
                }

                if (e.attr("name").equals("loginfield") && e.attr("value").equals("uid")) {
                    continue;
                }

                if (e.attr("name").equals("cookietime") && !e.attr("value").equals("315360000")) {
                    continue;
                }

                // 排除空值表单属性
                if (e.attr("name").length() > 0) {
                    datas.put(e.attr("name"), e.attr("value"));
                }
            }

            Connection con2 = Jsoup.connect("https://plus28.com/logging.php?action=login");
            con2.header(USER_AGENT, USER_AGENT_VALUE);
            con2.header("origin", "https://plus28.com");
            con2.header("referer", "https://plus28.com/logging.php?action=login");
            con2.header("upgrade-insecure-requests", "1");
            // 设置cookie和post上面的map数据
            login = con2.ignoreContentType(true).followRedirects(true).method(Connection.Method.POST)
                    .data(datas).cookies(rs.cookies()).execute();

            if (login.cookies().size() > 1) {
                    loginThread();
                plusCookies = login.cookies();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return plusCookies;
    }

    private static Map<String, String> plusCookies;
    private static boolean isRunning = false;

    static void loginThread() {
        isRunning = true;
        new Thread() {
            @Override
            public void run() {
                long plusExpireTime = 0L;
                long PLUS_EXPIRE_TIME = 2592000;
                long PLUS_EXPIRE_TIME_THREAD = 300000;

                while (true) {
                    try {
                        System.out.println("plus login thread running time -> " + plusExpireTime );
                        sleep(PLUS_EXPIRE_TIME_THREAD);
                        plusExpireTime += PLUS_EXPIRE_TIME_THREAD;

                        if (plusExpireTime >= PLUS_EXPIRE_TIME) {
                            isRunning = false;
                            plusCookies = null;
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    public static String getTimestamp() {
        return System.currentTimeMillis() + "000";
    }

    public static TextMessage getHelpMessage() {
        return new TextMessage("感謝您的加入，希望您會喜歡以下功能！\n" +
                "[指令]\n" +
                "\"!help\" : 查詢指令\n" +
                "\"!youtube\" : 支援的youtube清單\n" +
                "\"妞\" : 妞妞樸克\n" +
                "\"發\" : 發一張牌（可玩比大小）\n" +
                "\"星座(ex:\"金牛\" or \"巨蟹\"...)\" : 每天星座運勢\n" +
                "\"騎\" : 機車圖\n" +
                "\"開\" : 汽車圖\n" +
                "\"抓寶\" : 隨機抓神奇寶貝\n" +
                "\"看\" : 隨機看AV\n" +
                "\"脫\" : 抽鹹濕圖\n" +
                "\"抓\" : 抓片 \n" +
                "\"抽\" : 抽美女圖\n" +
                "\"sg\" : show girl\n" +
                "\"健身\" : 健身影片\n" +
                "\"動\" : 正妹影片\n" );
    }

    public static ArrayList<String>  getGigaCirclePicMap(String url) {
        ArrayList<String> imageUriSet = new ArrayList<>();
        try {
            Document gigaCircleDoc = Jsoup.connect(url).get();
            List<String> gigaCircleLinkSet = new ArrayList<String>();
            for (Element e : gigaCircleDoc.select("div[class=thumbs]")) {
                gigaCircleLinkSet.add(e.select("a").attr("href"));
            }
            String gigaCircleThumbLink = gigaCircleLinkSet.get(new Random().nextInt(gigaCircleLinkSet.size()));
            System.out.println("gigaCircleThumbLink : " + gigaCircleThumbLink);
            gigaCircleDoc = Jsoup.connect(gigaCircleThumbLink).get();
            gigaCircleLinkSet.clear();
            for (Element e : gigaCircleDoc.select("div[id=gallery]")) {
                for (Element se : e.select("img")) {
                    gigaCircleThumbLink = se.attr("data-original");

                    if (gigaCircleThumbLink.isEmpty()) {
                        continue;
                    }

                    if (!gigaCircleThumbLink.contains("https")) {
                        gigaCircleThumbLink = gigaCircleThumbLink.replace("http", "https");
                    }

                    imageUriSet.add(gigaCircleThumbLink);
                }
            }
        } catch (IOException e) {
        }
        return imageUriSet;
    }
}
