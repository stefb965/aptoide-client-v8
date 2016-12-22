/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 08/07/2016.
 */

package cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.timeline;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import cm.aptoide.pt.dataprovider.ws.v7.SendEventRequest;
import cm.aptoide.pt.model.v7.listapp.App;
import cm.aptoide.pt.model.v7.timeline.Similar;
import cm.aptoide.pt.preferences.Application;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.repository.SocialRepository;
import cm.aptoide.pt.v8engine.repository.TimelineMetricsManager;
import cm.aptoide.pt.v8engine.view.recycler.displayable.Displayable;
import cm.aptoide.pt.v8engine.view.recycler.displayable.SpannableFactory;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.grid.DateCalculator;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Getter;

/**
 * Created by marcelobenites on 7/8/16.
 */
public class SimilarDisplayable extends CardDisplayable {

  @Getter private int avatarResource;
  @Getter private int titleResource;
  @Getter private long appId;
  @Getter private String packageName;
  @Getter private String appName;
  @Getter private String appIcon;

  @Getter private String abUrl;
  private List<String> similarAppsNames;
  private List<String> similarAppsPackageNames;
  private Date date;
  private Date timestamp;
  private DateCalculator dateCalculator;
  private SpannableFactory spannableFactory;
  private TimelineMetricsManager timelineMetricsManager;
  private SocialRepository socialRepository;

  public SimilarDisplayable() {
  }

  public SimilarDisplayable(Similar similar, int avatarResource,
      int titleResource, long appId, String packageName, String appName, String appIcon,
      String abUrl, List<String> similarAppsNames, List<String> similarAppsPackageNames, Date date,
      Date timestamp, DateCalculator dateCalculator, SpannableFactory spannableFactory,
      TimelineMetricsManager timelineMetricsManager, SocialRepository socialRepository) {
    super(similar);
    this.avatarResource = avatarResource;
    this.titleResource = titleResource;
    this.appId = appId;
    this.packageName = packageName;
    this.appName = appName;
    this.appIcon = appIcon;
    this.abUrl = abUrl;
    this.similarAppsNames = similarAppsNames;
    this.similarAppsPackageNames = similarAppsPackageNames;
    this.date = date;
    this.timestamp = timestamp;
    this.dateCalculator = dateCalculator;
    this.spannableFactory = spannableFactory;
    this.timelineMetricsManager = timelineMetricsManager;
    this.socialRepository = socialRepository;
  }

  public static Displayable from(Similar similar, DateCalculator dateCalculator,
      SpannableFactory spannableFactory, TimelineMetricsManager timelineMetricsManager,
      SocialRepository socialRepository) {
    final List<String> similarAppsNames = new ArrayList<>();
    final List<String> similarAppsPackageNames = new ArrayList<>();
    for (App similarApp : similar.getSimilarApps()) {
      similarAppsNames.add(similarApp.getName());
      similarAppsPackageNames.add(similarApp.getPackageName());
    }

    String abTestingURL = null;

    if (similar.getAb() != null
        && similar.getAb().getConversion() != null
        && similar.getAb().getConversion().getUrl() != null) {
      abTestingURL = similar.getAb().getConversion().getUrl();
    }

    return new SimilarDisplayable(similar, Application.getConfiguration().getIcon(),
        R.string.displayable_social_timeline_recommendation_atptoide_team_recommends,
        similar.getRecommendedApp().getId(), similar.getRecommendedApp().getPackageName(),
        similar.getRecommendedApp().getName(), similar.getRecommendedApp().getIcon(), abTestingURL,
        similarAppsNames, similarAppsPackageNames, similar.getRecommendedApp().getUpdated(),
        similar.getTimestamp(), dateCalculator, spannableFactory, timelineMetricsManager,
        socialRepository);
  }

  public String getTitle() {
    return AptoideUtils.StringU.getFormattedString(titleResource,
        Application.getConfiguration().getMarketName());
  }

  public Spannable getStyledTitle(Context context) {
    String aptoide = Application.getConfiguration().getMarketName();
    return spannableFactory.createColorSpan(getTitle(),
        ContextCompat.getColor(context, R.color.appstimeline_recommends_title), aptoide);
  }

  public Spannable getSimilarAppsText(Context context) {
    StringBuilder similarAppsText = new StringBuilder(
        context.getString(R.string.displayable_social_timeline_similar_to,
            similarAppsNames.get(0)));
    for (int i = 1; i < similarAppsNames.size() - 1; i++) {
      similarAppsText.append(", ");
      similarAppsText.append(similarAppsNames.get(i));
    }
    if (similarAppsNames.size() > 1) {
      similarAppsText.append(" ");
      similarAppsText.append(
          context.getString(R.string.displayable_social_timeline_recommendation_similar_and));
      similarAppsText.append(" ");
      similarAppsText.append(similarAppsNames.get(similarAppsNames.size() - 1));
    }
    return spannableFactory.createStyleSpan(similarAppsText.toString(), Typeface.BOLD,
        similarAppsNames.toArray(new String[similarAppsNames.size()]));
  }

  public Spannable getAppText(Context context) {
    return spannableFactory.createColorSpan(
        context.getString(R.string.displayable_social_timeline_article_get_app_button, ""),
        ContextCompat.getColor(context, R.color.appstimeline_grey), "");
  }

  public String getTimeSinceLastUpdate(Context context) {
    return dateCalculator.getTimeSinceDate(context, date);
  }

  public String getTimeSinceRecommendation(Context context) {
    return dateCalculator.getTimeSinceDate(context, timestamp);
  }

  @Override public int getViewLayout() {
    return R.layout.displayable_social_timeline_similar;
  }

  public String getSimilarToAppPackageName() {
    if (similarAppsPackageNames.size() != 0) {
      return similarAppsPackageNames.get(0);
    }
    return "";
  }

  public void sendClickEvent(SendEventRequest.Body.Data data, String eventName) {
    timelineMetricsManager.sendEvent(data, eventName);
  }

  @Override public void share(Context context, boolean privacyResult) {
    socialRepository.share(getTimelineCard(), context, privacyResult);
  }
}