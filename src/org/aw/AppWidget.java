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

import java.net.URISyntaxException;
import java.util.Set;

import mobi.intuitit.android.content.LauncherIntent;
import mobi.intuitit.android.widget.BoundRemoteViews;
import mobi.intuitit.android.widget.SimpleRemoteViews;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

public class AppWidget extends AppWidgetProvider {
    
    private static final String TAG = "AppWidget";
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        Log.d(getClass().toString(), "onUpdate");
        for (int appWidgetId : appWidgetIds) {
            updateView(context, appWidgetManager, appWidgetId);
        }
    }
    
    public void updateView(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.home);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    
    private void logIntent(Intent intent, boolean extended) {
        if (extended)
            Log.d(TAG, "------------Log Intent------------");
        Log.d(TAG, "Action       : " + intent.getAction());
        if (!extended)
            return;
        Log.d(TAG, "Data         : " + intent.getDataString());
        Log.d(TAG, "Component    : " + intent.getComponent().toString());
        Log.d(TAG, "Package      : " + intent.getPackage());
        Log.d(TAG, "Flags        : " + intent.getFlags());
        Log.d(TAG, "Scheme       : " + intent.getScheme());
        Log.d(TAG, "SourceBounds : " + intent.getSourceBounds());
        Log.d(TAG, "Type         : " + intent.getType());
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Log.d(TAG, "--Extras--");

            for(String key : extras.keySet()) {
                Log.d(TAG, key + " --> " + extras.get(key));
            }
            Log.d(TAG, "----------");
        }
        Set<String> cats = intent.getCategories();
        if (cats != null) {
            Log.d(TAG, "--Categories--");
            for(String cat : cats) {
                Log.d(TAG, " --> " + cat);
            }
            Log.d(TAG, "--------------");
        }
        Log.d(TAG, "----------------------------------");
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        logIntent(intent, true);
        if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
            final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                this.onDeleted(context, new int[] { appWidgetId });
            }
        }
        else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_READY)) {
            onAppWidgetReady(context, intent);
        }
        else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_ITEM_CLICK)) {
            onItemClick(context, intent);
        }
        else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_VIEW_CLICK)) {
            onClick(context, intent);
        }
        else {
            super.onReceive(context, intent);
        }
    }
    
    /**
     * Receive ready intent from Launcher, prepare scroll view resources
     */
    public void onAppWidgetReady(Context context, Intent intent) {

        Log.d(TAG, "onAppWidgetReady");
        
        int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        
        if (appWidgetId < 0) {
            Log.d(TAG, "Cannot get app widget id from ready intent");
            return;
        }

        Intent replaceDummy = CreateMakeScrollableIntent(context, appWidgetId);
        // Send it out
        Log.d(TAG, "----- push replaceDummy Intent");
        context.sendBroadcast(replaceDummy);
    }

    public Intent CreateMakeScrollableIntent(Context context, int appWidgetId) {
        String widgeturi = DataProvider.CONTENT_URI_MESSAGES.buildUpon().appendEncodedPath(
                Integer.toString(appWidgetId)).toString();
        
        Intent replaceDummy = new Intent(LauncherIntent.Action.ACTION_SCROLL_WIDGET_START);

        replaceDummy.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        replaceDummy.putExtra(LauncherIntent.Extra.EXTRA_VIEW_ID, R.id.dummy_text_view);

        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_PROVIDER_ALLOW_REQUERY, true);

        final int colCount = 4;
        
        // Give a layout resource to be inflated. If this is not given, Home++
        // will create one
        SimpleRemoteViews gridViews = new SimpleRemoteViews(R.layout.gridview);
        gridViews.setInt(R.id.gridview, "setNumColumns", colCount);
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_REMOTEVIEWS, gridViews);
        
        BoundRemoteViews itemViews = new BoundRemoteViews(R.layout.icon);
        itemViews.setBoundCharSequence(R.id.text_view, "setText",
                DataProvider.DataProviderColumns.name.ordinal(),0);
        itemViews.setBoundBitmap(R.id.image_view, "setImageBitmap",
                DataProvider.DataProviderColumns.icon.ordinal(), R.drawable.icon);

        Intent intent = new Intent(context, this.getClass());
        intent.setAction(LauncherIntent.Action.ACTION_ITEM_CLICK);
        intent.setData(Uri.parse(widgeturi));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        itemViews.SetBoundOnClickIntent(R.id.image_view, pendingIntent,
                LauncherIntent.Extra.Scroll.EXTRA_DATA_URI,
                DataProvider.DataProviderColumns.appuri.ordinal());
        itemViews.SetBoundOnClickIntent(R.id.text_view, pendingIntent,
                LauncherIntent.Extra.Scroll.EXTRA_DATA_URI,
                DataProvider.DataProviderColumns.appuri.ordinal());

        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_REMOTEVIEWS, itemViews);
        putProvider(replaceDummy, widgeturi);

        // Launcher can set onClickListener for each children of an item. Without
        // explictly put this
        // extra, it will just set onItemClickListener by default
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_CHILDREN_CLICKABLE, true);
        
        return replaceDummy;
    }
    
    /**
     * Put provider info as extras in the specified intent
     * 
     * @param intent
     */
    public static void putProvider(Intent intent, String widgetUri) {
        if (intent == null)
            return;

        String whereClause = null;
        String orderBy = null;
        String[] selectionArgs = null;

        intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_URI, widgetUri);
        Log.d(TAG, "widgetUri pushed to Launcher : " + widgetUri);

        intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_PROJECTION, DataProvider.PROJECTION_APPWIDGETS);
        intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION, whereClause);
        intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION_ARGUMENTS, selectionArgs);
        intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SORT_ORDER, orderBy);
    }
    
    /**
     * Put mapping info as extras in intent
     */
    public static void putMapping(Intent intent) {
        if (intent == null)
            return;
    }
    
    private void onClick(Context context, Intent intent) {
        Log.d(TAG, "onClick(Context context, Intent intent)");
    }
    
    private void onItemClick(Context context, Intent intent) {
        Log.d(TAG, "onItemClick(Context context, Intent intent)");
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Log.d(TAG, "--Extras--");

            for(String key : extras.keySet()) {
                if (key.equals(LauncherIntent.Extra.Scroll.EXTRA_DATA_URI)) {
                    try {
                        Intent action = Intent.getIntent((String) extras.get(key));
                        action.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(action);
                    }
                    catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            }
            Log.d(TAG, "----------");
        }
    }
}
