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
import com.linecorp.bot.model.message.AudioMessage;
import com.linecorp.bot.model.message.ImageMessage;
import com.linecorp.bot.model.message.ImagemapMessage;
import com.linecorp.bot.model.message.LocationMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.StickerMessage;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.VideoMessage;
import com.linecorp.bot.model.message.imagemap.ImagemapArea;
import com.linecorp.bot.model.message.imagemap.ImagemapBaseSize;
import com.linecorp.bot.model.message.imagemap.MessageImagemapAction;
import com.linecorp.bot.model.message.imagemap.URIImagemapAction;
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
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@Slf4j
@LineMessageHandler
public class KitchenSinkController {
    enum WEB_SITES {
        jpBeautifyHouse, ck101, voc, plus28
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
        LocationMessageContent locationMessage = event.getMessage();
        reply(event.getReplyToken(), new LocationMessage(
                locationMessage.getTitle(),
                locationMessage.getAddress(),
                locationMessage.getLatitude(),
                locationMessage.getLongitude()
        ));
    }

    @EventMapping
    public void handleImageMessageEvent(MessageEvent<ImageMessageContent> event) throws IOException {
        // You need to install ImageMagick
        handleHeavyContent(
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
                });
    }

    @EventMapping
    public void handleAudioMessageEvent(MessageEvent<AudioMessageContent> event) throws IOException {
        handleHeavyContent(
                event.getReplyToken(),
                event.getMessage().getId(),
                responseBody -> {
                    DownloadedContent mp4 = saveContent("mp4", responseBody);
                    reply(event.getReplyToken(), new AudioMessage(mp4.getUri(), 100));
                });
    }

    @EventMapping
    public void handleVideoMessageEvent(MessageEvent<VideoMessageContent> event) throws IOException {
        // You need to install ffmpeg and ImageMagick.
        handleHeavyContent(
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
                });
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
        this.replyText(replyToken, "Joined " + event.getSource());
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

            case "18抽":
                List<String> image18UriSet = new ArrayList<>();
                Random randomFor18 = new Random();
                String[] plus2818Set = {"https://www.plus28.com/rss.php?fid=826&auth=119dpFYXjLHpaDPhkFevCnMAtbvt2tbDppWqDMuJBzTIXHsF7IEvzUXaWJBBUtkoVw",
                        "https://www.plus28.com/rss.php?fid=1188&auth=bc9fR%2FT59V6zEoU%2BjUzx6dY3XnAFe981p%2FrQe8mTKyTnUZ2qR1HxB4P6Zpq5p3ueCTo",
                        "https://www.plus28.com/rss.php?fid=445&auth=ffc5a6AxTTHvL7EUpuQL34NX%2FgOdDivSBRZb9TEdLBoOGiT5OJ8JWQG0t4gPL9S5CQ",
                "https://www.plus28.com/rss.php?fid=1283&auth=2d271zg5CEehb1q9aRu5osAQx45Ogw1dIRhmF7qI3hgPWwLXGv%2FZ9BQnWixVFy1HR4o",
                "https://www.plus28.com/rss.php?fid=1286&auth=e4efX98trILBTjq39QV97u8kgp7NDh%2Bmf1lr0QPkxlViv2eth9yChW6CcanpNr6mIZY",
                "https://www.plus28.com/rss.php?fid=1074&auth=50f1BzHQrQB2P5MRqeph77zsy%2F5UU2t9bdz1bXV%2Blk3pBuVu02iSOBlGK0w%2B6%2FFz3%2BY"};

                System.out.println("18抽 website : " + plus2818Set[randomFor18.nextInt(plus2818Set.length)]);
                Document plus2818Doc = Jsoup.connect(plus2818Set[randomFor18.nextInt(plus2818Set.length)]).get();
                Elements plus2818ItemSet = plus2818Doc.select("item");
                Elements plus2818LinkSet = plus2818ItemSet.select("link");
                String plus2818Link = plus2818LinkSet.get(randomFor18.nextInt(plus2818LinkSet.size())).text();
                Document plus2818ImgDoc = Jsoup.connect(plus2818Link).get();
                Elements plus2818ImgDivSet = plus2818ImgDoc.select("div[class=t_msgfont]");
                Elements plus2818ImgSet = plus2818ImgDivSet.get(0).select("img[src$=.jpg]");
                for (Element plus28Img : plus2818ImgSet) {
                    image18UriSet.add(plus28Img.attr("src"));
                }
                int image18Number = randomFor18.nextInt(image18UriSet.size()) ;
                this.reply(replyToken, new ImageMessage(image18UriSet.get(image18Number), image18UriSet.get(image18Number)));
                break;

            case "抽":
                List<String> imageUriSet = new ArrayList<>();
                Random random = new Random();

                WEB_SITES who = WEB_SITES.values()[random.nextInt(WEB_SITES.values().length)];
                System.out.println("抽 website : " + who.name());
                switch (who) {
                    //jpBeautyHouse
                    case jpBeautifyHouse:
                        Document jpDoc = Jsoup.connect("http://jpbeautyhouse.blogspot.com/feeds/posts/default").get();
                        Elements jpBeautyHouseSet = jpDoc.select("content");
                        for (Element jpBeautyHouse : jpBeautyHouseSet) {
                            Document elementDoc = Jsoup.parse(jpBeautyHouse.text());
                            Elements elements = elementDoc.select("div");
                            for (Element e : elements) {
                                Elements elements1 = e.select("img");
                                for(Element e1 : elements1) {
                                    imageUriSet.add(e1.attr("src"));
                                }
                            }
                        }
                        break;

                    case ck101:
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
                        break;

                   default:
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
                        break;
                }
                int imageNumber = random.nextInt(imageUriSet.size()) ;
                this.reply(replyToken, new ImageMessage(imageUriSet.get(imageNumber), imageUriSet.get(imageNumber)));
                break;

            case "抓":
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

                /*System.out.println("抓 url : "+url);
                this.reply(replyToken, new ImageMessage(url, url));*/

                ArrayList<ImageCarouselColumn> imageCarouselColumns = new ArrayList<>();
                for (int i = 0; i < 1; i++) {
                    imageCarouselColumns.add(
                            new ImageCarouselColumn(url,
                                    new URIAction("點一下開始播放", videoUrl)));
                }

                ImageCarouselTemplate imageCarouselTemplate = new ImageCarouselTemplate(
                        imageCarouselColumns);

                this.reply(replyToken,new TemplateMessage("ImageCarousel alt text", imageCarouselTemplate));
                break;

            case "!help":
                this.reply(
                        replyToken, new TextMessage("[指令]\n"+
                                "\"妞\" : 妞妞樸克\n" +
                                "\"發\" : 發一張牌（可玩比大小）\n" +
                                "\"抓\" : 抓片）\n" +
                                "\"抽\" : 抽美女圖"));
                break;
            case "imagemap":
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
                break;
            default:
                log.info("Returns echo message {}: {}", replyToken, text);
                /*this.replyText(
                        replyToken,
                        text
                );*/
                break;
        }
    }

    private static TemplateMessage createPokerMessage(int numberOfPokers, Poker poker) {
        ArrayList<ImageCarouselColumn> imageCarouselColumns = new ArrayList<>();
        for (int i = 0; i < numberOfPokers; i++) {
            String imagePath = poker.getPath(i);
            String pokerPoint = String.valueOf(poker.getPoint(i));
            imageCarouselColumns.add(
                    new ImageCarouselColumn(createUri("/static/poker/" + imagePath +".jpeg"),
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

}
