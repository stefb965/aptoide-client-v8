package cm.aptoide.pt.dataprovider.ws.v7;

import android.support.annotation.Nullable;
import cm.aptoide.pt.model.v7.GetFollowers;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Observable;

/**
 * Created by trinkes on 16/12/2016.
 */

public class GetFollowingRequest extends V7<GetFollowers, GetFollowersRequest.Body> {

  protected GetFollowingRequest(GetFollowersRequest.Body body,
      BodyInterceptor<BaseBody> bodyInterceptor, OkHttpClient httpClient,
      Converter.Factory converterFactory) {
    super(body, BASE_HOST, httpClient, converterFactory, bodyInterceptor);
  }

  public static GetFollowingRequest of(BodyInterceptor<BaseBody> bodyInterceptor,
      @Nullable Long userId, @Nullable Long storeId, OkHttpClient httpClient,
      Converter.Factory converterFactory) {
    GetFollowersRequest.Body body = new GetFollowersRequest.Body();
    body.setUserId(userId);
    body.setStoreId(storeId);
    return new GetFollowingRequest(body, bodyInterceptor, httpClient, converterFactory);
  }

  @Override protected Observable<GetFollowers> loadDataFromNetwork(Interfaces interfaces,
      boolean bypassCache) {
    return interfaces.getTimelineGetFollowing(body, bypassCache);
  }
}
