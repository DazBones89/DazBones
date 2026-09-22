package com.dazbones.service;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.regex.*;
import org.springframework.stereotype.Component;

@Component
public class InstagramMetadataClient {
 public record PostUrl(String url,String shortcode){}
 public static PostUrl normalize(String raw){
  URI uri;
  try {uri=URI.create(raw==null?"":raw.trim());}catch(Exception e){throw invalid();}
  if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||
    !(uri.getHost().equalsIgnoreCase("www.instagram.com")||uri.getHost().equalsIgnoreCase("instagram.com"))||
    uri.getUserInfo()!=null||uri.getPort()!=-1)throw invalid();
  var m=Pattern.compile("^/(p|reel|tv)/([A-Za-z0-9_-]{5,64})/?$").matcher(uri.getRawPath());
  if(!m.matches())throw invalid();
  return new PostUrl("https://www.instagram.com/"+m.group(1)+"/"+m.group(2)+"/",m.group(2));
 }
 private static IllegalArgumentException invalid(){return new IllegalArgumentException("Instagramの投稿URL（https://www.instagram.com/p/…/ または /reel/…/）を入力してください。プロフィールや共有用の短縮URLは登録できません。");}
 public LocalDateTime publishedAt(PostUrl post){
  String embed=fetch(post.url()+"embed/captioned/");
  if(!embed.contains(post.shortcode())||!embed.contains("EmbeddedMedia"))throw unavailable();
  try{return parse(post,embed);}catch(IllegalArgumentException e){return parse(post,fetch(post.url()));}
 }
 private String fetch(String url){
  HttpURLConnection connection=null;
  try {
   // Only the normalized Instagram host is contacted; redirects never reach another host.
   connection=(HttpURLConnection)URI.create(url).toURL().openConnection();
   connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(5000);connection.setReadTimeout(5000);
   connection.setRequestProperty("User-Agent","DazBones/1.0 (Instagram post embed verification)");
   int status=connection.getResponseCode();
   if(status==404||status==410)throw new IllegalArgumentException("投稿が見つかりません。URLや投稿の公開設定を確認してください。");
   if(status!=200)throw unavailable();
   try(var input=connection.getInputStream();var output=new ByteArrayOutputStream()){
    byte[] buffer=new byte[8192];int n;long deadline=System.nanoTime()+10_000_000_000L;
    while((n=input.read(buffer))!=-1){if(output.size()+n>2_000_000||System.nanoTime()>deadline)throw unavailable();output.write(buffer,0,n);}
    return output.toString(StandardCharsets.UTF_8);
   }
  }catch(IOException e){throw unavailable();}finally{if(connection!=null)connection.disconnect();}
 }
 static LocalDateTime parse(PostUrl post,String html){
  if(!html.contains(post.shortcode()))throw unavailable();
  var time=Pattern.compile("<time\\b[^>]*datetime=[\"']([^\"']+)[\"']",Pattern.CASE_INSENSITIVE).matcher(html);
  if(time.find())try{return valid(OffsetDateTime.parse(time.group(1)).toInstant());}catch(DateTimeException ignored){}
  var timestamp=Pattern.compile("\"(?:taken_at_timestamp|taken_at)\"\\s*:\\s*(\\d{10})(?!\\d)").matcher(html);
  if(timestamp.find())try{return valid(Instant.ofEpochSecond(Long.parseLong(timestamp.group(1))));}catch(DateTimeException ignored){}
  var date=Pattern.compile("\"datePublished\"\\s*:\\s*\"([^\"]+)\"").matcher(html);
  if(date.find())try{return valid(OffsetDateTime.parse(date.group(1)).toInstant());}catch(DateTimeException ignored){}
  // Public post metadata sometimes provides only a calendar date, without a time.
  var meta=Pattern.compile("<meta\\b[^>]*>",Pattern.CASE_INSENSITIVE).matcher(html);
  while(meta.find()){
   String tag=meta.group();
   if(!tag.contains("og:description"))continue;
   var content=Pattern.compile("content=[\"']([^\"']*)[\"']",Pattern.CASE_INSENSITIVE).matcher(tag);
   if(content.find()){
    String description=org.springframework.web.util.HtmlUtils.htmlUnescape(content.group(1));
    var dateOnly=Pattern.compile("^.*? - [A-Za-z0-9._]+ on ([A-Za-z]+ \\d{1,2}, \\d{4})(?:$|[\\s:\\.])").matcher(description);
    if(dateOnly.find())try{return valid(java.time.LocalDate.parse(dateOnly.group(1),java.time.format.DateTimeFormatter.ofPattern("MMMM d, uuuu",java.util.Locale.ENGLISH)).atStartOfDay().toInstant(ZoneOffset.UTC));}catch(DateTimeException ignored){}
   }
  }
  throw unavailable();
 }
 private static LocalDateTime valid(Instant instant){if(instant.isBefore(Instant.parse("2010-01-01T00:00:00Z"))||instant.isAfter(Instant.now().plusSeconds(86400)))throw unavailable();return LocalDateTime.ofInstant(instant,ZoneOffset.UTC);}
 private static IllegalArgumentException unavailable(){return new IllegalArgumentException("Instagramで投稿の公開状態・投稿日時を確認できませんでした。公開と埋め込み許可を確認して再試行してください。通信制限の場合もあります。登録は行っていません。");}
}
