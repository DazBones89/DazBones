package com.dazbones.service;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;
class InstagramMetadataClientTest {
 @Test void urlsAreNormalizedAndUnsafeOrNonPostUrlsRejected(){
  assertThat(InstagramMetadataClient.normalize("https://instagram.com/p/DVETcRUkxMO/?stkn=secret").url()).isEqualTo("https://www.instagram.com/p/DVETcRUkxMO/");
  for(String url:new String[]{"http://instagram.com/p/abcde/","https://instagram.com.evil.test/p/abcde/","https://www.instagram.com@evil.test/p/abcde/","https://instagram.com:443/p/abcde/","https://instagram.com/dazbones89/","https://instagram.com/p/../","javascript:alert(1)","https://instagram.com/p/%61bcde/"})assertThatThrownBy(()->InstagramMetadataClient.normalize(url)).isInstanceOf(IllegalArgumentException.class);
 }
 @Test void datesUsePublishedMetadataAndNeverRegistrationTime(){
  var post=InstagramMetadataClient.normalize("https://www.instagram.com/p/DVETcRUkxMO/");
  assertThat(InstagramMetadataClient.parse(post,"DVETcRUkxMO <time datetime='2026-02-22T13:20:00Z'>")).isEqualTo(LocalDateTime.of(2026,2,22,13,20));
  assertThat(InstagramMetadataClient.parse(post,"DVETcRUkxMO <meta property='og:description' content='0 likes, 0 comments - dazbones89 on February 22, 2026' />")).isEqualTo(LocalDateTime.of(2026,2,22,0,0));
  assertThatThrownBy(()->InstagramMetadataClient.parse(post,"DVETcRUkxMO Login required")).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->InstagramMetadataClient.parse(post,"<time datetime='2026-02-22T13:20:00Z'>other post")).isInstanceOf(IllegalArgumentException.class);
 }
}
