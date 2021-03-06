package cm.aptoide.pt.dataprovider.ws.v7;

import cm.aptoide.pt.dataprovider.BuildConfig;
import cm.aptoide.pt.model.v7.SetComment;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Observable;

public class PostCommentForTimelineArticle
    extends V7<SetComment, PostCommentForTimelineArticle.Body> {

  private static final String BASE_HOST = BuildConfig.APTOIDE_WEB_SERVICES_SCHEME
      + "://"
      + BuildConfig.APTOIDE_WEB_SERVICES_WRITE_V7_HOST
      + "/api/7/";

  private PostCommentForTimelineArticle(Body body, BodyInterceptor<BaseBody> bodyInterceptor,
      OkHttpClient httpClient, Converter.Factory converterFactory) {
    super(body, BASE_HOST, httpClient, converterFactory, bodyInterceptor);
  }

  public static PostCommentForTimelineArticle of(String timelineArticleId, String text,
      BodyInterceptor<BaseBody> bodyInterceptor, OkHttpClient httpClient,
      Converter.Factory converterFactory) {
    Body body = new Body(timelineArticleId, text);
    return new PostCommentForTimelineArticle(body, bodyInterceptor, httpClient, converterFactory);
  }

  public static PostCommentForTimelineArticle of(String timelineArticleId, long previousCommentId,
      String text, BodyInterceptor<BaseBody> bodyInterceptor, OkHttpClient httpClient,
      Converter.Factory converterFactory) {
    Body body = new Body(timelineArticleId, text, previousCommentId);
    return new PostCommentForTimelineArticle(body, bodyInterceptor, httpClient, converterFactory);
  }

  @Override
  protected Observable<SetComment> loadDataFromNetwork(Interfaces interfaces, boolean bypassCache) {
    return interfaces.postTimelineComment(body, true);
  }

  @Data @Accessors(chain = false) @EqualsAndHashCode(callSuper = true) public static class Body
      extends BaseBody {

    @JsonProperty("card_uid") private String timelineArticleId;
    @JsonProperty("comment_id") private Long previousCommentId;
    private String body;
    //private String commentType = CommentType.TIMELINE.name();

    public Body(String timelineArticleId, String text, long previousCommentId) {
      this(timelineArticleId, text);
      this.previousCommentId = previousCommentId;
    }

    public Body(String timelineArticleId, String text) {
      this.timelineArticleId = timelineArticleId;
      this.body = text;
    }
  }
}
