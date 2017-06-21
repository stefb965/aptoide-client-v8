/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 29/08/2016.
 */

package cm.aptoide.pt.v8engine.billing.view;

import android.os.Bundle;
import cm.aptoide.accountmanager.Account;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.v8engine.billing.AptoideBilling;
import cm.aptoide.pt.v8engine.billing.PaymentAnalytics;
import cm.aptoide.pt.v8engine.billing.Product;
import cm.aptoide.pt.v8engine.billing.exception.PaymentLocalProcessingRequiredException;
import cm.aptoide.pt.v8engine.billing.exception.PaymentNotAuthorizedException;
import cm.aptoide.pt.v8engine.presenter.PaymentSelector;
import cm.aptoide.pt.v8engine.presenter.Presenter;
import cm.aptoide.pt.v8engine.presenter.View;
import cm.aptoide.pt.v8engine.view.account.AccountNavigator;
import java.io.IOException;
import java.util.List;
import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by marcelobenites on 8/19/16.
 */
public class PaymentPresenter implements Presenter {

  private static final int LOGIN_REQUEST_CODE = 2001;

  private final PaymentView view;
  private final AptoideBilling aptoideBilling;
  private final AptoideAccountManager accountManager;
  private final PaymentSelector paymentSelector;
  private final AccountNavigator accountNavigator;
  private final PaymentNavigator paymentNavigator;
  private final ProductProvider productProvider;
  private final PaymentAnalytics paymentAnalytics;

  public PaymentPresenter(PaymentView view, AptoideBilling aptoideBilling,
      AptoideAccountManager accountManager, PaymentSelector paymentSelector,
      AccountNavigator accountNavigator, PaymentNavigator paymentNavigator,
      PaymentAnalytics paymentAnalytics, ProductProvider productProvider) {
    this.view = view;
    this.aptoideBilling = aptoideBilling;
    this.accountManager = accountManager;
    this.paymentSelector = paymentSelector;
    this.accountNavigator = accountNavigator;
    this.paymentNavigator = paymentNavigator;
    this.paymentAnalytics = paymentAnalytics;
    this.productProvider = productProvider;
  }

  @Override public void present() {

    onViewCreatedShowLogin();

    onViewCreatedShowPaymentInformation();

    onViewCreatedCheckPurchase();

    handlePaymentSelection();

    handleCancellationSelection();

    handleTapOutsideSelection();

    handleBuySelection();

    onViewDestroyedHideAllErrors();
  }

  private void onViewCreatedShowLogin() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMapSingle(__ -> accountManager.accountStatus()
            .first()
            .toSingle())
        .filter(account -> !account.isLoggedIn())
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap(__ -> accountNavigator.navigateToLoginViewForResult(LOGIN_REQUEST_CODE))
        .filter(loggedIn -> !loggedIn)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(__ -> view.dismiss())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> view.dismiss(throwable));
  }

  private void onViewCreatedShowPaymentInformation() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMapSingle(created -> userLoggedIn())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(loggedIn -> view.showLoading())
        .flatMapSingle(loading -> productProvider.getProduct())
        .flatMapCompletable(
            product -> getPayments(product).observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(payments -> showPaymentInformation(product, payments))
                .flatMapCompletable(payments -> showSelectedPayment(payments))
                .doOnCompleted(() -> view.hideLoading()))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(__ -> {
        }, throwable -> {
          view.hideLoading();
          view.dismiss(throwable);
        });
  }

  private void onViewCreatedCheckPurchase() {
    view.getLifecycle()
        .filter(event -> event.equals(View.LifecycleEvent.CREATE))
        .flatMapSingle(created -> userLoggedIn())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(loggedIn -> view.showLoading())
        .flatMapSingle(loading -> productProvider.getProduct())
        .flatMap(product -> aptoideBilling.getConfirmation(product)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext(confirmation -> {
              if (confirmation.isFailed()) {
                view.hideLoading();
                view.showUnknownError();
              } else if (confirmation.isNew()) {
                view.hideLoading();
              } else if (confirmation.isPending()) {
                view.showLoading();
              } else if (confirmation.isCompleted()) {
                view.showLoading();
              }
            })
            .first(confirmation -> confirmation.isCompleted())
            .flatMapSingle(confirmation -> aptoideBilling.getPurchase(product)))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(purchase -> view.dismiss(purchase), throwable -> {
          view.hideLoading();
          showError(throwable);
        });
  }

  private void handleTapOutsideSelection() {
    view.getLifecycle()
        .filter(event -> View.LifecycleEvent.CREATE.equals(event))
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap(product -> view.tapOutsideSelection())
        .flatMapSingle(cancellation -> productProvider.getProduct())
        .flatMapCompletable(
            product -> sendTapOutsideAnalytics().observeOn(AndroidSchedulers.mainThread())
                .doOnCompleted(() -> view.dismiss()))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> view.dismiss(throwable));
  }

  private void handleCancellationSelection() {
    view.getLifecycle()
        .filter(event -> View.LifecycleEvent.CREATE.equals(event))
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap(product -> view.cancellationSelection())
        .flatMapCompletable(
            product -> sendCancellationAnalytics().observeOn(AndroidSchedulers.mainThread())
                .doOnCompleted(() -> view.dismiss()))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> view.dismiss());
  }

  private void handlePaymentSelection() {
    view.getLifecycle()
        .filter(event -> View.LifecycleEvent.CREATE.equals(event))
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap(created -> view.paymentSelection())
        .flatMapCompletable(payment -> paymentSelector.selectPayment(payment))
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> view.dismiss(throwable));
  }

  private void handleBuySelection() {
    view.getLifecycle()
        .filter(event -> View.LifecycleEvent.CREATE.equals(event))
        .flatMap(__ -> view.buySelection()
            .flatMapSingle(selection -> productProvider.getProduct())
            .flatMapCompletable(product -> getSelectedPayment(product).doOnSuccess(
                payment -> paymentAnalytics.sendPaymentBuyButtonPressedEvent(product,
                    payment.getName()))
                .flatMapCompletable(
                    payment -> aptoideBilling.processPayment(payment.getId(), product)
                        .observeOn(AndroidSchedulers.mainThread())
                        .onErrorResumeNext(throwable -> {
                          if (throwable instanceof PaymentNotAuthorizedException) {
                            paymentNavigator.navigateToAuthorizationView(payment, product);
                            view.hideLoading();
                            return Completable.complete();
                          }

                          if (throwable instanceof PaymentLocalProcessingRequiredException) {
                            paymentNavigator.navigateToLocalPaymentView(payment, product);
                            view.hideLoading();
                            return Completable.complete();
                          }

                          return Completable.error(throwable);
                        })))
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError(throwable -> {
              view.hideLoading();
              showError(throwable);
            })
            .retry())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> view.dismiss(throwable));
  }

  private void onViewDestroyedHideAllErrors() {
    view.getLifecycle()
        .filter(event -> View.LifecycleEvent.DESTROY.equals(event))
        .doOnNext(destroyed -> view.hideAllErrors())
        .compose(view.bindUntilEvent(View.LifecycleEvent.DESTROY))
        .subscribe(__ -> {
        }, throwable -> view.dismiss(throwable));
  }

  private void showError(Throwable throwable) {
    if (throwable instanceof IOException) {
      view.showNetworkError();
    } else {
      view.showUnknownError();
    }
  }

  private void showPaymentInformation(Product product,
      List<PaymentView.PaymentViewModel> payments) {
    view.showProduct(product);
    if (payments.isEmpty()) {
      view.showPaymentsNotFoundMessage();
    } else {
      view.showPayments(payments);
    }
  }

  private Completable sendCancellationAnalytics() {
    return productProvider.getProduct()
        .flatMapCompletable(product -> getSelectedPayment(product).doOnSuccess(
            payment -> paymentAnalytics.sendPaymentCancelButtonPressedEvent(product,
                payment.getName()))
            .toCompletable());
  }

  private Completable sendTapOutsideAnalytics() {
    return productProvider.getProduct()
        .flatMapCompletable(product -> getSelectedPayment(product).doOnSuccess(
            payment -> paymentAnalytics.sendPaymentTapOutsideEvent(product, payment.getName()))
            .toCompletable());
  }

  private Single<List<PaymentView.PaymentViewModel>> getPayments(Product product) {
    return aptoideBilling.getPayments(product)
        .flatMapObservable(payments -> Observable.from(payments))
        .map(payment -> new PaymentView.PaymentViewModel(payment.getId(), payment.getName(),
            payment.getDescription()))
        .toList()
        .toSingle();
  }

  private Single<PaymentView.PaymentViewModel> getSelectedPayment(Product product) {
    return getPayments(product).flatMap(payments -> paymentSelector.selectedPayment(payments));
  }

  private Completable showSelectedPayment(List<PaymentView.PaymentViewModel> payments) {
    return paymentSelector.selectedPayment(payments)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSuccess(payment -> view.selectPayment(payment))
        .toCompletable();
  }

  private Single<Account> userLoggedIn() {
    return accountManager.accountStatus()
        .first(account -> account.isLoggedIn())
        .toSingle();
  }

  @Override public void saveState(Bundle state) {

  }

  @Override public void restoreState(Bundle state) {

  }
}
