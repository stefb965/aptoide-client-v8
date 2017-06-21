/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 22/11/2016.
 */

package cm.aptoide.pt.v8engine.billing.repository.sync;

import android.content.SharedPreferences;
import android.content.SyncResult;
import cm.aptoide.pt.database.accessors.PaymentConfirmationAccessor;
import cm.aptoide.pt.dataprovider.NetworkOperatorManager;
import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.dataprovider.ws.v3.BaseBody;
import cm.aptoide.pt.dataprovider.ws.v3.GetPaymentConfirmationRequest;
import cm.aptoide.pt.dataprovider.ws.v3.V3;
import cm.aptoide.pt.model.v3.PaymentConfirmationResponse;
import cm.aptoide.pt.v8engine.billing.Payer;
import cm.aptoide.pt.v8engine.billing.PaymentAnalytics;
import cm.aptoide.pt.v8engine.billing.PaymentConfirmation;
import cm.aptoide.pt.v8engine.billing.Product;
import cm.aptoide.pt.v8engine.billing.product.InAppProduct;
import cm.aptoide.pt.v8engine.billing.repository.PaymentConfirmationFactory;
import cm.aptoide.pt.v8engine.repository.exception.RepositoryItemNotFoundException;
import cm.aptoide.pt.v8engine.sync.ScheduledSync;
import java.io.IOException;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Single;

public class ConfirmationSync extends ScheduledSync {

  private final Product product;
  private final NetworkOperatorManager operatorManager;
  private final PaymentConfirmationAccessor confirmationAccessor;
  private final PaymentConfirmationFactory confirmationFactory;
  private final Payer payer;
  private final BodyInterceptor<BaseBody> bodyInterceptorV3;
  private final Converter.Factory converterFactory;
  private final OkHttpClient httpClient;
  private final PaymentAnalytics analytics;
  private final TokenInvalidator tokenInvalidator;
  private final SharedPreferences sharedPreferences;

  public ConfirmationSync(Product product, NetworkOperatorManager operatorManager,
      PaymentConfirmationAccessor confirmationAccessor,
      PaymentConfirmationFactory confirmationFactory, Payer payer,
      BodyInterceptor<BaseBody> bodyInterceptorV3, Converter.Factory converterFactory,
      OkHttpClient httpClient, PaymentAnalytics analytics, TokenInvalidator tokenInvalidator,
      SharedPreferences sharedPreferences) {
    this.product = product;
    this.operatorManager = operatorManager;
    this.confirmationAccessor = confirmationAccessor;
    this.confirmationFactory = confirmationFactory;
    this.payer = payer;
    this.bodyInterceptorV3 = bodyInterceptorV3;
    this.converterFactory = converterFactory;
    this.httpClient = httpClient;
    this.analytics = analytics;
    this.tokenInvalidator = tokenInvalidator;
    this.sharedPreferences = sharedPreferences;
  }

  @Override public void sync(SyncResult syncResult) {
    try {
      payer.getId()
          .flatMap(payerId -> getServerPaymentConfirmation(product, payerId).doOnSuccess(
              paymentConfirmation -> {
                analytics.sendPurchaseStatusEvent(paymentConfirmation, product);
                savePaymentConfirmation(paymentConfirmation);
                reschedulePendingConfirmation(paymentConfirmation, syncResult);
              })
              .doOnError(throwable -> {
                saveAsNewConfirmationOnServerError(payerId, throwable);
                rescheduleOnNetworkError(syncResult, throwable);
              }))
          .toCompletable()
          .onErrorComplete()
          .await();
    } catch (RuntimeException e) {
      rescheduleSync(syncResult);
    }
  }

  private Single<PaymentConfirmation> getServerPaymentConfirmation(Product product,
      String payerId) {
    return Single.just(product instanceof InAppProduct)
        .flatMap(isInAppBilling -> {
          if (isInAppBilling) {
            return GetPaymentConfirmationRequest.of(product.getId(), operatorManager,
                ((InAppProduct) product).getApiVersion(), bodyInterceptorV3, httpClient,
                converterFactory, tokenInvalidator, sharedPreferences)
                .observe()
                .cast(PaymentConfirmationResponse.class)
                .toSingle();
          }
          return GetPaymentConfirmationRequest.of(product.getId(), operatorManager,
              bodyInterceptorV3, httpClient, converterFactory, tokenInvalidator, sharedPreferences)
              .observe()
              .toSingle();
        })
        .flatMap(response -> {
          if (response != null && response.isOk()) {
            return Single.just(
                confirmationFactory.convertToPaymentConfirmation(product.getId(), response,
                    payerId));
          }
          return Single.error(new RepositoryItemNotFoundException(V3.getErrorMessage(response)));
        });
  }

  private void reschedulePendingConfirmation(PaymentConfirmation paymentConfirmation,
      SyncResult syncResult) {
    if (paymentConfirmation.isPending()) {
      rescheduleSync(syncResult);
    }
  }

  private void rescheduleOnNetworkError(SyncResult syncResult, Throwable throwable) {
    if (throwable instanceof IOException) {
      analytics.sendPurchaseNetworkRetryEvent(product);
      rescheduleSync(syncResult);
    }
  }

  private void saveAsNewConfirmationOnServerError(String payerId, Throwable throwable) {
    if (!(throwable instanceof IOException)) {
      savePaymentConfirmation(
          confirmationFactory.create(product.getId(), "", PaymentConfirmation.Status.NEW, payerId));
    }
  }

  private void savePaymentConfirmation(PaymentConfirmation paymentConfirmation) {
    confirmationAccessor.insert(
        confirmationFactory.convertToDatabasePaymentConfirmation(paymentConfirmation));
  }
}
