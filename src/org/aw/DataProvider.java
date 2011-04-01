/*
 * App List Widget
 * Copyright (C) Marc Boulanger 2011
 * 
 * AppWidget is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * App List Widget is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.aw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

public class DataProvider extends ContentProvider {

    public static final String TAG = "org.aw.DataProvider";

    private static final String AUTHORITY_BASE = "org.aw.widgets.applications";
    public static final String AUTHORITY = AUTHORITY_BASE + ".provider";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    public static final Uri CONTENT_URI_MESSAGES = CONTENT_URI.buildUpon()
            .appendEncodedPath("data").build();

    private static final UriMatcher URI_MATCHER = new UriMatcher(
            UriMatcher.NO_MATCH);
    private static final int URI_DATA = 0;
    static {
        URI_MATCHER.addURI(AUTHORITY, "data/*", URI_DATA);
    }

    public enum DataProviderColumns {
        _id, name, icon, appuri
    }

    public static final String[] PROJECTION_APPWIDGETS = new String[] {
            DataProviderColumns._id.toString(),
            DataProviderColumns.name.toString(),
            DataProviderColumns.icon.toString(),
            DataProviderColumns.appuri.toString() };

    private Context mContext;

    private final BroadcastReceiver mApplicationsReceiver = new ApplicationsIntentReceiver();

    private static ArrayList<ApplicationInfo> mApplications = null;

    private class ContObserver extends ContentObserver {

        public ContObserver() {
            super(null);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            AppWidgetManager awm = AppWidgetManager.getInstance(mContext);
            int[] ids = awm.getAppWidgetIds(new ComponentName(mContext,
                    AppWidget.class));
            for (int id : ids) {
                Uri widgetUri = CONTENT_URI_MESSAGES.buildUpon()
                        .appendEncodedPath(Integer.toString(id)).build();
                mContext.getContentResolver().notifyChange(widgetUri, null);
            }
        }
    }

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate() START");
        if (mContext == null) {
            mContext = getContext();
            mContext.getContentResolver().registerContentObserver(
                    RawContacts.CONTENT_URI, true, new ContObserver());
        }

        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            mContext.registerReceiver(mApplicationsReceiver, filter);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "onCreate() FINISHED");
        return false;
    }
    
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "Cursor query(...) START");
        AppCursor cursor = new AppCursor(projection);
        if (mApplications == null) {
            loadApplications(true);
        }
        for (String s : projection)
            Log.d(TAG, s);
        for (ApplicationInfo appInfo : mApplications) {
            Log.d(TAG, appInfo.getTitle().toString());
            Object[] values = new Object[projection.length];
            values[0] = appInfo.getId();
            values[1] = appInfo.getTitle();
            values[2] = appInfo.getIconBytes();
            values[3] = appInfo.getIntent().toUri(0);
            cursor.addRow(values);
        }
        Log.d(TAG, "Cursor query(...) FINISHED");
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private class ApplicationsIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadApplications(false);
        }
    }

    /**
     * Loads the list of installed applications in mApplications.
     */
    public synchronized void loadApplications(boolean isLaunching) {
        PackageManager manager = mContext.getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));
        if (apps != null) {
            if (mApplications != null) {
                mApplications.clear();
            }
            else {
                mApplications = new ArrayList<ApplicationInfo>();
            }
            int id = 0;
            for (ResolveInfo info : apps) {
                ApplicationInfo application = new ApplicationInfo();
                application.setId(id);
                application.setTitle(info.loadLabel(manager));
                application.setActivity(new ComponentName(
                        info.activityInfo.applicationInfo.packageName,
                        info.activityInfo.name), Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                application.setIcon(((BitmapDrawable) info.activityInfo
                        .loadIcon(manager)).getBitmap());
                mApplications.add(application);
                id++;
            }
        }
    }
}
