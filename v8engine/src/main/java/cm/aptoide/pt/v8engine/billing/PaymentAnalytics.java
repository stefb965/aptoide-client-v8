package cm.aptoide.pt.v8engine.billing;

import android.os.Bundle;
import cm.aptoide.pt.v8engine.analytics.Analytics;
import cm.aptoide.pt.v8engine.analytics.Event;
import cm.aptoide.pt.v8engine.analytics.events.FacebookEvent;
import cm.aptoide.pt.v8engine.billing.product.InAppProduct;
import com.facebook.appevents.AppEventsLogger;

public class PaymentAnalytics {

  private final Analytics analytics;
  private final AppEventsLogger facebook;
  private final String aptoidePackageName;

  public PaymentAnalytics(Analytics analytics, AppEventsLogger facebook,
      String aptoidePackageName) {
    this.analytics = analytics;
    this.facebook = facebook;
    this.aptoidePackageName = aptoidePackageName;
  }

  public void sendPaidAppBuyButtonPressedEvent(double price, String currency) {
    analytics.sendEvent(new FacebookEvent(facebook, "Clicked_On_Buy_Button",
        getProductBundle(price, currency, aptoidePackageName)));
  }

  public void sendPaymentCancelButtonPressedEvent(Product product, String paymentName) {
    analytics.sendEvent(getPaymentEvent("Payment_Pop_Up", "Cancel", product, paymentName));
  }

  public void sendPaymentBuyButtonPressedEvent(Product product, String paymentName) {
    analytics.sendEvent(getPaymentEvent("Payment_Pop_Up", "Buy", product, paymentName));
  }

  public void sendPaymentTapOutsideEvent(Product product, String paymentName) {
    analytics.sendEvent(getPaymentEvent("Payment_Pop_Up", "Tap Outside", product, paymentName));
  }

  public void sendBackToStoreButtonPressedEvent(Product product) {
    final Bundle bundle = getProductBundle(product);
    bundle.putString("action", "Voltar para loja button");
    analytics.sendEvent(new FacebookEvent(facebook, "Payment_Authorization_Page", bundle));
  }

  public void sendPurchaseNetworkRetryEvent(Product product) {
    analytics.sendEvent(
        new FacebookEvent(facebook, "Payment_Purchase_Retry", getProductBundle(product)));
  }

  public void sendPurchaseStatusEvent(PaymentConfirmation paymentConfirmation, Product product) {

    // We only send analytics about failed or completed payment confirmations
    if (paymentConfirmation.isPending() || paymentConfirmation.isNew()) {
      return;
    }

    final Bundle bundle = getProductBundle(product);
    bundle.putString("status", getPurchaseStatus(paymentConfirmation));
    analytics.sendEvent(new FacebookEvent(facebook, "Payment_Purchase_Complete", bundle));
  }

  public void sendPaymentAuthorizationBackButtonPressedEvent(Product product) {
    final Bundle bundle = getProductBundle(product);
    bundle.putString("action", "Android back button");
    analytics.sendEvent(new FacebookEvent(facebook, "Payment_Authorization_Page", bundle));
  }

  public void sendPaymentAuthorizationNetworkRetryEvent() {
    analytics.sendEvent(new FacebookEvent(facebook, "Payment_Authorization_Retry"));
  }

  public void sendAuthorizationCompleteEvent(Authorization paymentAuthorization) {
    if (paymentAuthorization.isFailed() || paymentAuthorization.isAuthorized()) {
      final Bundle bundle = new Bundle();
      bundle.putString("status", getAuthorizationStatus(paymentAuthorization));
      analytics.sendEvent(new FacebookEvent(facebook, "Payment_Authorization_Complete", bundle));
    }
  }

  private String getAuthorizationStatus(Authorization paymentAuthorization) {

    if (paymentAuthorization.isAuthorized()) {
      return "success";
    }

    if (paymentAuthorization.isFailed()) {
      return "failed";
    }

    throw new IllegalArgumentException("Can NOT determine payment authorization analytics status.");
  }

  private String getPurchaseStatus(PaymentConfirmation paymentConfirmation) {

    if (paymentConfirmation.isFailed()) {
      return "failed";
    }

    if (paymentConfirmation.isCompleted()) {
      return "success";
    }

    throw new IllegalArgumentException("Can NOT determine payment confirmation analytics status.");
  }

  private Event getPaymentEvent(String eventName, String action, Product product,
      String paymentName) {
    final Bundle bundle = getProductBundle(product);
    bundle.putString("action", action);
    bundle.putString("payment_method", paymentName);
    return new FacebookEvent(facebook, eventName, bundle);
  }

  private Bundle getProductBundle(Product product) {

    final String packageName;
    if (product instanceof InAppProduct) {
      packageName = ((InAppProduct) product).getPackageName();
    } else {
      packageName = aptoidePackageName;
    }
    return getProductBundle(product.getPrice()
        .getAmount(), product.getPrice()
        .getCurrency(), packageName);
  }

  private Bundle getProductBundle(double price, String currency, String packageName) {
    final Bundle bundle = new Bundle();
    bundle.putDouble("purchase_value", price);
    bundle.putString("purchase_currency", currency);
    bundle.putString("package_name_seller", packageName);
    return bundle;
  }
}
