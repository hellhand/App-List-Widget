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

import android.database.AbstractCursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.CursorWindow;
import android.util.Log;

public class AppCursor extends AbstractCursor {

    String[] mColumnNames;
    ArrayList<Object> data;
    int columnCount;
    int rowCount;

    public AppCursor(String[] columnNames) {
        this.mColumnNames = columnNames;
        this.columnCount = mColumnNames.length;
        this.data = new ArrayList<Object>(); 
        this.rowCount = 0;
    }

    public void addRow(Object[] columnValues) {
        Log.d(getClass().toString(), "addRow(" + columnValues[0] + ")");
        for (Object elem : columnValues)
            data.add(elem);
        rowCount++;
    }
    
    @Override
    public String[] getColumnNames() {
        return mColumnNames;
    }
        
    private Object get(int column) {
        Log.d(getClass().toString(), "get(" + column + ")");
        if (column < 0 || column >= columnCount) {
            throw new CursorIndexOutOfBoundsException("Requested column: "
                    + column + ", # of columns: " + columnCount);
        }
        if (mPos < 0) {
            throw new CursorIndexOutOfBoundsException("Before first row.");
        }
        if (mPos >= rowCount) {
            throw new CursorIndexOutOfBoundsException("After last row.");
        }
        return data.get(mPos * columnCount + column);
    }

    @Override
    public int getCount() {
        return rowCount;
    }

    @Override
    public double getDouble(int column) {
        Object value = get(column);
        return (value instanceof String) ? Double.valueOf((String) value)
                : ((Number) value).doubleValue();
    }

    @Override
    public float getFloat(int column) {
        Object value = get(column);
        return (value instanceof String) ? Float.valueOf((String) value)
                : ((Number) value).floatValue();
    }

    @Override
    public int getInt(int column) {
        Object value = get(column);
        return (value instanceof String) ? Integer.valueOf((String) value)
                : ((Number) value).intValue();
    }

    @Override
    public long getLong(int column) {
        Object value = get(column);
        return (value instanceof String) ? Long.valueOf((String) value)
                : ((Number) value).longValue();
    }

    @Override
    public short getShort(int column) {
        Object value = get(column);
        return (value instanceof String) ? Short.valueOf((String) value)
                : ((Number) value).shortValue();
    }

    @Override
    public String getString(int column) {
        Log.d(getClass().toString(), "getString(" + column + ") -> " + String.valueOf(get(column)));
        return String.valueOf(get(column));
    }

    @Override
    public byte[] getBlob(int column) {
        Log.d(getClass().toString(), "getBlob(" + column + ")");
        Object value = get(column);
        return (value instanceof byte[]) ? (byte[])value : new byte[0];
    }
    
    @Override
    public boolean isNull(int column) {
        Log.d(getClass().toString(), "isNull(" + column + ")");
        return get(column) == null;
    }

    @Override
    public void fillWindow(int position, CursorWindow window) {
        Log.d(getClass().toString(), "fillWindow(" + position + "," + window + ")");
        if (position < 0 || position > getCount()) {
            return;
        }
        window.acquireReference();
        try {
            int oldpos = mPos;
            mPos = position - 1;
            window.clear();
            window.setStartPosition(position);
            int columnNum = getColumnCount();
            window.setNumColumns(columnNum);
            while (moveToNext() && window.allocRow()) {            
                for (int i = 0; i < columnNum; i++) {
                    Object value = get(i);
                    
                    if (value == null) {
                        if (!window.putNull(mPos, i)) {
                            window.freeLastRow();
                            break;
                        }
                    }                   
                    else if (value instanceof byte[])
                    {                       
                        byte[] val = (byte[])value;
                        window.putBlob(val, mPos, i);
                    }
                    else
                    {
                        String field = getString(i);
                        if (field != null) {
                            if (!window.putString(field, mPos, i)) {
                                window.freeLastRow();
                                break;
                            }
                        }
                    } 
                }
            }
            
            mPos = oldpos;
        } catch (IllegalStateException e){
            e.printStackTrace();
        } finally {
            window.releaseReference();
        }
    }
}
