package cm.aptoide.pt.shareappsandroid;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.widget.Toast;
import cm.aptoide.pt.shareapps.socket.entities.AndroidAppInfo;
import cm.aptoide.pt.shareapps.socket.entities.Host;
import cm.aptoide.pt.shareapps.socket.interfaces.FileClientLifecycle;
import cm.aptoide.pt.shareapps.socket.interfaces.FileServerLifecycle;
import cm.aptoide.pt.shareapps.socket.interfaces.HostsChangedCallback;
import cm.aptoide.pt.shareapps.socket.message.client.AptoideMessageClientController;
import cm.aptoide.pt.shareapps.socket.message.client.AptoideMessageClientSocket;
import cm.aptoide.pt.shareapps.socket.message.interfaces.StorageCapacity;
import cm.aptoide.pt.shareapps.socket.message.messages.RequestPermissionToSend;
import cm.aptoide.pt.shareapps.socket.message.server.AptoideMessageServerSocket;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TimerTask;

import static android.R.attr.id;

/**
 * Created by filipegoncalves on 10-02-2017.
 */

public class HighwayServerService extends Service {

  private int port;

  private NotificationManager mNotifyManager;
  private Object mBuilderSend;
  private Object mBuilderReceive;
  private long lastTimestampReceive;
  private long lastTimestampSend;
  private FileClientLifecycle<AndroidAppInfo> fileClientLifecycle;
  private FileServerLifecycle fileServerLifecycle;

  private List<App> listOfApps;
  private AptoideMessageClientController aptoideMessageClientController;

  @Override public void onCreate() {
    super.onCreate();
    if (mNotifyManager == null) {
      mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }
    fileClientLifecycle = new FileClientLifecycle<AndroidAppInfo>() {
      @Override public void onError(IOException e) {
        e.printStackTrace();
      }

      @Override public void onStartReceiving(AndroidAppInfo androidAppInfo) {
        System.out.println(" Started receiving ");
        showToast(" Started receiving ");

        //show notification
        createReceiveNotification(androidAppInfo.getAppName());

        Intent i = new Intent();
        i.putExtra("FinishedReceiving", false);
        i.putExtra("appName", androidAppInfo.getAppName());
        i.setAction("RECEIVEAPP");
        sendBroadcast(i);
      }

      @Override public void onFinishReceiving(AndroidAppInfo androidAppInfo) {
        System.out.println(" Finished receiving ");
        showToast(" Finished receiving ");

        finishReceiveNotification(androidAppInfo.getApk().getFilePath());

        Intent i = new Intent();
        i.putExtra("FinishedReceiving", true);
        i.putExtra("needReSend", false);
        i.putExtra("appName", androidAppInfo.getAppName());
        i.putExtra("packageName", androidAppInfo.getPackageName());
        i.putExtra("filePath", androidAppInfo.getApk().getFilePath());
        i.setAction("RECEIVEAPP");
        sendBroadcast(i);
      }

      @Override
      public void onProgressChanged(float progress) {//todo add AndroidAPpInfo - to get appname
        System.out.println("onProgressChanged() called with: " + "progress = [" + progress + "]");
        //        showToast("onProgressChanged() called with: " + "progress = [" + progress + "]");
        int actualProgress = Math.round(progress * 100);
        showReceiveProgress("insertAppName", actualProgress);
      }
    };

    fileServerLifecycle = new FileServerLifecycle<AndroidAppInfo>() {

      @Override public void onStartSending(AndroidAppInfo androidAppInfo) {
        System.out.println("Server : started sending");
        showToast("Server : started sending");

        //create notification

        createSendNotification();

        Intent i = new Intent();
        i.putExtra("isSent", false);
        i.putExtra("needReSend",
            false);//add field with pos to resend and change its value only if it is != 100000 (onstartcommand)
        i.putExtra("appName", androidAppInfo.getAppName());
        i.putExtra("packageName", androidAppInfo.getPackageName());
        i.putExtra("positionToReSend", 100000);
        i.setAction("SENDAPP");
        sendBroadcast(i);
      }

      @Override public void onError(IOException e) {
        e.printStackTrace();
      }

      @Override public void onFinishSending(AndroidAppInfo androidAppInfo) {
        System.out.println("Server : finished sending");
        showToast("Server : finished sending");

        finishSendNotification();

        Intent i = new Intent();
        i.putExtra("isSent", true);
        i.putExtra("needReSend", false);
        i.putExtra("appName", androidAppInfo.getAppName());
        i.putExtra("packageName", androidAppInfo.getPackageName());
        i.putExtra("positionToReSend", 100000);
        i.setAction("SENDAPP");
        sendBroadcast(i);
      }

      @Override public void onProgressChanged(float progress) {
        System.out.println("onProgressChanged() called with: progress = [" + progress + "]");
        //        showToast("onProgressChanged() called with: progress = [" + progress + "]");
        int actualProgress = Math.round(progress * 100);
        showSendProgress("insertAppName", actualProgress);
      }
    };

    System.out.println(" Inside the service of the server");
  }

  @Deprecated private void showToast(final String str) {
    new Handler(Looper.getMainLooper()).post(new TimerTask() {
      @Override public void run() {
        Toast.makeText(getBaseContext(), str, Toast.LENGTH_LONG).show();
      }
    });
  }

  private void createReceiveNotification(String receivingAppName) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      mBuilderReceive = new Notification.Builder(this);
      ((Notification.Builder) mBuilderReceive).setContentTitle(
          this.getResources().getString(R.string.shareApps) + " - " + this.getResources()
              .getString(R.string.receive))
          .setContentText(
              this.getResources().getString(R.string.receiving) + " " + receivingAppName)
          .setSmallIcon(R.mipmap.lite);
    }
  }

  private void finishReceiveNotification(String receivedApkFilePath) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      ((Notification.Builder) mBuilderReceive).setContentText(
          this.getResources().getString(R.string.transfCompleted))
          // Removes the progress bar
          .setSmallIcon(android.R.drawable.stat_sys_download_done)
          .setProgress(0, 0, false)
          .setAutoCancel(true);

      File f = new File(receivedApkFilePath);
      Intent install = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(f),
          "application/vnd.android.package-archive");
      PendingIntent contentIntent = PendingIntent.getActivity(this, 0, install, 0);

      ((Notification.Builder) mBuilderReceive).setContentIntent(contentIntent);
      if (mNotifyManager == null) {
        mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      }
      mNotifyManager.notify(id, ((Notification.Builder) mBuilderReceive).getNotification());
    }
  }

  private void showReceiveProgress(String receivingAppName, int actual) {

    if (System.currentTimeMillis() - lastTimestampReceive > 1000 / 3) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        ((Notification.Builder) mBuilderReceive).setContentText(
            this.getResources().getString(R.string.receiving) + " " + receivingAppName);

        ((Notification.Builder) mBuilderReceive).setProgress(100, actual, false);
        if (mNotifyManager == null) {
          mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        mNotifyManager.notify(id, ((Notification.Builder) mBuilderReceive).getNotification());
      }
      lastTimestampReceive = System.currentTimeMillis();
    }
  }

  /**
   * Method to be called after getting the callback of finishSending
   */
  //  public void finishedSending(String appName, String packageName) {
  //    Intent finishedSending = new Intent();
  //    finishedSending.setAction("SENDAPP");
  //    finishedSending.putExtra("isSent", false);
  //    finishedSending.putExtra("needReSend", false);
  //    finishedSending.putExtra("appName", appName);
  //    finishedSending.putExtra("packageName", packageName);
  //    finishedSending.putExtra("positionToReSend", 100000);
  //  }
  private void createSendNotification() {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      mBuilderSend = new Notification.Builder(this);
      ((Notification.Builder) mBuilderSend).setContentTitle(
          this.getResources().getString(R.string.shareApps) + " - " + this.getResources()
              .getString(R.string.send))
          .setContentText(this.getResources().getString(R.string.preparingSend))
          .setSmallIcon(R.mipmap.lite);
    }
  }

  private void showSendProgress(String sendingAppName, int actual) {

    if (System.currentTimeMillis() - lastTimestampSend > 1000 / 3) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        ((Notification.Builder) mBuilderSend).setContentText(
            this.getResources().getString(R.string.sending) + " " + sendingAppName);
        ((Notification.Builder) mBuilderSend).setProgress(100, actual, false);
        if (mNotifyManager == null) {
          mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        mNotifyManager.notify(id, ((Notification.Builder) mBuilderSend).getNotification());
      }
      lastTimestampSend = System.currentTimeMillis();
    }
  }

  private void finishSendNotification() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      ((Notification.Builder) mBuilderSend).setContentText(
          this.getResources().getString(R.string.transfCompleted))
          // Removes the progress bar
          .setSmallIcon(android.R.drawable.stat_sys_download_done)
          .setProgress(0, 0, false)
          .setAutoCancel(true);
      if (mNotifyManager == null) {
        mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      }
      mNotifyManager.notify(id, ((Notification.Builder) mBuilderSend).getNotification());
    }
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null) {
      lastTimestampReceive = System.currentTimeMillis();
      System.out.println("inside of startcommand in the service");
      if (intent.getAction() != null && intent.getAction().equals("RECEIVE")) {
        //port = intent.getIntExtra("port", 0);
        System.out.println("Going to start serving");
        AptoideMessageServerSocket aptoideMessageServerSocket =
            new AptoideMessageServerSocket(55555, 500000);
        aptoideMessageServerSocket.setHostsChangedCallbackCallback(new HostsChangedCallback() {
          @Override public void hostsChanged(List<Host> hostList) {
            System.out.println("hostsChanged() called with: " + "hostList = [" + hostList + "]");
            DataHolder.getInstance().setConnectedClients(hostList);
          }
        });
        aptoideMessageServerSocket.startAsync();
        String s = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            .toString();
        StorageCapacity storageCapacity = new StorageCapacity() {
          @Override public boolean hasCapacity(long bytes) {
            return true;
          }
        };

        // TODO: 22-02-2017 fix this hardcoded ip

        aptoideMessageClientController =
            new AptoideMessageClientController(s, storageCapacity, fileServerLifecycle,
                fileClientLifecycle);
        (new AptoideMessageClientSocket("192.168.43.1", 55555,
            aptoideMessageClientController)).startAsync();

        System.out.println("Connected 342");
      } else if (intent.getAction() != null && intent.getAction().equals("SEND")) {
        //read parcelable
        Bundle b = intent.getBundleExtra("bundle");
        //        if (listOfApps == null || listOfApps.get(listOfApps.size() - 1)
        //                .isOnChat()) { //null ou ultimo elemento ja acabado de enviar.
        listOfApps = b.getParcelableArrayList("listOfAppsToInstall");
        System.out.println(
            "serverComm : Just received the list of Apps :  the list of apps size is  :"
                + listOfApps.size());

        //create the mesage and send it.

        for (int i = 0; i < listOfApps.size(); i++) {
          String filePath = listOfApps.get(i).getFilePath();
          String appName = listOfApps.get(i).getAppName();
          String packageName = listOfApps.get(i).getPackageName();
          String obbsFilePath = listOfApps.get(i).getObbsFilePath();

          System.out.println(" Filepath from app 0 (test) is:  " + filePath);
          File apk = new File(filePath);

          File mainObb = null;
          File patchObb = null;

          AndroidAppInfo appInfo;
          if (!obbsFilePath.equals("noObbs")) {

            appInfo = new AndroidAppInfo(appName, packageName, apk, mainObb, patchObb);
          } else {
            appInfo = new AndroidAppInfo(appName, packageName, apk);
          }

          aptoideMessageClientController.send(
              new RequestPermissionToSend(aptoideMessageClientController.getLocalhost(), appInfo));
        }
      }
    }
    return START_STICKY;
  }

  @Nullable @Override public IBinder onBind(Intent intent) {
    return null;
  }
}