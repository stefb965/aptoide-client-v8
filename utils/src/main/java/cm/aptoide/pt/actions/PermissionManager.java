/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 25/07/2016.
 */

package cm.aptoide.pt.actions;

import rx.Observable;

/**
 * Created by marcelobenites on 7/25/16.
 */
public class PermissionManager {

  public Observable<Void> requestExternalStoragePermission(PermissionService permissionRequest) {
    return Observable.create(new RequestAccessToExternalFileSystemOnSubscribe(permissionRequest));
  }

  public Observable<Void> requestDownloadAccess(PermissionService permissionRequest) {
    return Observable.create(new RequestDownloadAccessOnSubscribe(permissionRequest));
  }

  public Observable<Boolean> requestContactsAccess(PermissionService permissionRequest) {
    return Observable.create(new RequestContactsAccessOnSubscribe(permissionRequest));
  }
}
