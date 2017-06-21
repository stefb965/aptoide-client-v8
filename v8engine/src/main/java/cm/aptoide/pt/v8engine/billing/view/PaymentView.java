/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 19/08/2016.
 */

package cm.aptoide.pt.v8engine.billing.view;

import cm.aptoide.pt.v8engine.billing.Product;
import cm.aptoide.pt.v8engine.billing.Purchase;
import cm.aptoide.pt.v8engine.presenter.View;
import java.util.List;
import rx.Observable;

public interface PaymentView extends View {

  Observable<PaymentViewModel> paymentSelection();

  Observable<Void> cancellationSelection();

  Observable<Void> tapOutsideSelection();

  Observable<Void> buySelection();

  void selectPayment(PaymentViewModel payment);

  void showLoading();

  void showPayments(List<PaymentViewModel> paymentList);

  void showProduct(Product product);

  void hideLoading();

  void dismiss(Purchase purchase);

  void dismiss(Throwable throwable);

  void dismiss();

  void showPaymentsNotFoundMessage();

  void showNetworkError();

  void showUnknownError();

  void hideAllErrors();

  class PaymentViewModel {

    private final int id;
    private final String name;
    private final String description;

    public PaymentViewModel(int id, String name, String description) {
      this.id = id;
      this.name = name;
      this.description = description;
    }

    public int getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

  }
}
