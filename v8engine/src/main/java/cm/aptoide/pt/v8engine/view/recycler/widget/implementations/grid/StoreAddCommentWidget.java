package cm.aptoide.pt.v8engine.view.recycler.widget.implementations.grid;

import android.app.FragmentManager;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.crashreports.CrashReports;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.utils.design.ShowMessage;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.util.StoreThemeEnum;
import cm.aptoide.pt.v8engine.view.recycler.displayable.implementations.grid.StoreAddCommentDisplayable;
import cm.aptoide.pt.v8engine.view.recycler.widget.Widget;
import cm.aptoide.pt.viewRateAndCommentReviews.CommentDialogFragment;
import com.jakewharton.rxbinding.view.RxView;
import com.trello.rxlifecycle.FragmentEvent;
import rx.Observable;

public class StoreAddCommentWidget extends Widget<StoreAddCommentDisplayable> {

  private static final String TAG = StoreAddCommentWidget.class.getName();

  private Button commentStore;

  public StoreAddCommentWidget(View itemView) {
    super(itemView);
  }

  @Override protected void assignViews(View itemView) {
    commentStore = (Button) itemView.findViewById(R.id.comment_store_button);
  }

  private int getColorOrDefault(StoreThemeEnum theme, Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return context.getResources().getColor(theme.getStoreHeader(), context.getTheme());
    } else {
      return context.getResources().getColor(theme.getStoreHeader());
    }
  }

  @Override public void bindView(StoreAddCommentDisplayable displayable) {

    final Context context = getContext();

    @ColorInt int color = getColorOrDefault(displayable.getStoreTheme(), context);
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      Drawable d = context.getDrawable(R.drawable.dialog_bg_2);
      d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
      commentStore.setBackground(d);
    } else {
      Drawable d = context.getResources().getDrawable(R.drawable.dialog_bg_2);
      d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
      commentStore.setBackgroundDrawable(d);
    }

    compositeSubscription.add(RxView.clicks(commentStore)
        .flatMap(a -> showStoreCommentFragment(displayable.getStoreId(), displayable.getStoreName(),
            getContext().getFragmentManager(), commentStore))
        .subscribe(a -> {
          // all done when we get here.
        }, err -> {
          Logger.e(TAG, err);
          CrashReports.logException(err);
        }));
  }

  private Observable<Void> showStoreCommentFragment(final long storeId,
      @NonNull final String storeName, @NonNull final FragmentManager fragmentManager,
      @NonNull final View view) {

    return Observable.just(AptoideAccountManager.isLoggedIn()).flatMap(isLoggedIn -> {

      if (isLoggedIn) {
        // show fragment CommentDialog
        CommentDialogFragment commentDialogFragment =
            CommentDialogFragment.newInstanceStoreComment(storeId, storeName);

        return commentDialogFragment.lifecycle()
            .doOnSubscribe(
                () -> commentDialogFragment.show(fragmentManager, "fragment_comment_dialog"))
            .filter(event -> event.equals(FragmentEvent.DESTROY_VIEW))
            .doOnNext(a -> reloadComments())
            .flatMap(event -> Observable.empty());
      }

      return showSignInMessage(view);
    });
  }

  private void reloadComments() {
    // TODO: 5/12/2016 sithengineer
    Logger.d(TAG, "TODO: reload the comments");
  }

  private Observable<Void> showSignInMessage(@NonNull final View view) {
    return ShowMessage.asObservableSnack(view, R.string.you_need_to_be_logged_in, R.string.login,
        snackView -> {
          AptoideAccountManager.openAccountManager(view.getContext());
        }).flatMap(a -> Observable.empty());
  }
}