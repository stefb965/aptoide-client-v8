/*
 * Copyright (c) 2017.
 * Modified by Marcelo Benites on 17/02/2017.
 */

package cm.aptoide.pt.v8engine.presenter;

import android.content.SharedPreferences;
import cm.aptoide.pt.v8engine.billing.view.PaymentView;
import java.util.List;
import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.schedulers.Schedulers;

public class PaymentSelector {

  private static final String SELECTED_PAYMENT_ID = "selectedPaymentId";
  private final int defaultPaymentId;
  private final SharedPreferences preferences;

  public PaymentSelector(int defaultPaymentId, SharedPreferences preferences) {
    this.defaultPaymentId = defaultPaymentId;
    this.preferences = preferences;
  }

  public Single<PaymentView.PaymentViewModel> selectedPayment(
      List<PaymentView.PaymentViewModel> payments) {
    return getSelectedPaymentId().flatMap(
        selectedPaymentId -> payment(payments, selectedPaymentId).switchIfEmpty(
            payment(payments, defaultPaymentId))
            .switchIfEmpty(Observable.error(new IllegalStateException("No payment selected.")))
            .toSingle());
  }

  public Completable selectPayment(PaymentView.PaymentViewModel selectedPayment) {
    return Completable.fromAction(() -> preferences.edit()
        .putInt(SELECTED_PAYMENT_ID, selectedPayment.getId())
        .commit())
        .subscribeOn(Schedulers.io());
  }

  private Single<Integer> getSelectedPaymentId() {
    return Single.fromCallable(() -> preferences.getInt(SELECTED_PAYMENT_ID, 0))
        .subscribeOn(Schedulers.io());
  }

  private Observable<PaymentView.PaymentViewModel> payment(
      List<PaymentView.PaymentViewModel> payments, int paymentId) {
    return Observable.from(payments)
        .filter(payment -> paymentId != 0 && paymentId == payment.getId());
  }
}
