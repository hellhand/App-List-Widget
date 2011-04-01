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

import mobi.intuitit.android.content.LauncherIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ListViewManager {

    private static final String TAG = "ListViewManager";
    
    /**
     * Receive ready intent from Launcher, prepare scroll view resources
     */
    public static void onAppWidgetReady(Context context, Intent intent) {

        Log.d(TAG, "onAppWidgetReady");
        
        int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        
        if (appWidgetId < 0) {
            Log.d(TAG, "Cannot get app widget id from ready intent");
            return;
        }

        Intent replaceDummy = new Intent(LauncherIntent.Action.ACTION_SCROLL_WIDGET_START);

        replaceDummy.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        replaceDummy.putExtra(LauncherIntent.Extra.EXTRA_VIEW_ID, R.id.dummy_text_view);

        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_PROVIDER_ALLOW_REQUERY, true);

        // Give a layout resource to be inflated. If this is not given, Home++
        // will create one

        // Put adapter info
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_LAYOUT_ID,
                R.layout.list);

        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_ID, R.layout.gridview);
        putProvider(replaceDummy, DataProvider.CONTENT_URI_MESSAGES.buildUpon().appendEncodedPath(
                Integer.toString(appWidgetId)).toString());
        putMapping(replaceDummy);

        // Launcher can set onClickListener for each children of an item. Without
        // explictly put this
        // extra, it will just set onItemClickListener by default
        replaceDummy.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_CHILDREN_CLICKABLE, true);

        Log.d(TAG, "----- push replaceDummy Intent");

        // Send it out
        context.sendBroadcast(replaceDummy);
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
}
