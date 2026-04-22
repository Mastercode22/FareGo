package com.farego.app.db.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.farego.app.db.entity.FareRate;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class FareRateDao_Impl implements FareRateDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<FareRate> __insertionAdapterOfFareRate;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public FareRateDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFareRate = new EntityInsertionAdapter<FareRate>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `fare_rates` (`id`,`transport_type`,`base_rate`,`per_km_rate`,`minimum_fare`,`peak_multiplier`,`traffic_multiplier`,`last_updated`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final FareRate entity) {
        statement.bindLong(1, entity.id);
        if (entity.transportType == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.transportType);
        }
        statement.bindDouble(3, entity.baseRate);
        statement.bindDouble(4, entity.perKmRate);
        statement.bindDouble(5, entity.minimumFare);
        statement.bindDouble(6, entity.peakMultiplier);
        statement.bindDouble(7, entity.trafficMultiplier);
        statement.bindLong(8, entity.lastUpdated);
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM fare_rates";
        return _query;
      }
    };
  }

  @Override
  public void insertAll(final List<FareRate> rates) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfFareRate.insert(rates);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void clearAll() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfClearAll.release(_stmt);
    }
  }

  @Override
  public LiveData<List<FareRate>> getAllRates() {
    final String _sql = "SELECT * FROM fare_rates ORDER BY id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"fare_rates"}, false, new Callable<List<FareRate>>() {
      @Override
      @Nullable
      public List<FareRate> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTransportType = CursorUtil.getColumnIndexOrThrow(_cursor, "transport_type");
          final int _cursorIndexOfBaseRate = CursorUtil.getColumnIndexOrThrow(_cursor, "base_rate");
          final int _cursorIndexOfPerKmRate = CursorUtil.getColumnIndexOrThrow(_cursor, "per_km_rate");
          final int _cursorIndexOfMinimumFare = CursorUtil.getColumnIndexOrThrow(_cursor, "minimum_fare");
          final int _cursorIndexOfPeakMultiplier = CursorUtil.getColumnIndexOrThrow(_cursor, "peak_multiplier");
          final int _cursorIndexOfTrafficMultiplier = CursorUtil.getColumnIndexOrThrow(_cursor, "traffic_multiplier");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "last_updated");
          final List<FareRate> _result = new ArrayList<FareRate>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FareRate _item;
            _item = new FareRate();
            _item.id = _cursor.getInt(_cursorIndexOfId);
            if (_cursor.isNull(_cursorIndexOfTransportType)) {
              _item.transportType = null;
            } else {
              _item.transportType = _cursor.getString(_cursorIndexOfTransportType);
            }
            _item.baseRate = _cursor.getDouble(_cursorIndexOfBaseRate);
            _item.perKmRate = _cursor.getDouble(_cursorIndexOfPerKmRate);
            _item.minimumFare = _cursor.getDouble(_cursorIndexOfMinimumFare);
            _item.peakMultiplier = _cursor.getDouble(_cursorIndexOfPeakMultiplier);
            _item.trafficMultiplier = _cursor.getDouble(_cursorIndexOfTrafficMultiplier);
            _item.lastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public FareRate getByType(final String type) {
    final String _sql = "SELECT * FROM fare_rates WHERE transport_type = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (type == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, type);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTransportType = CursorUtil.getColumnIndexOrThrow(_cursor, "transport_type");
      final int _cursorIndexOfBaseRate = CursorUtil.getColumnIndexOrThrow(_cursor, "base_rate");
      final int _cursorIndexOfPerKmRate = CursorUtil.getColumnIndexOrThrow(_cursor, "per_km_rate");
      final int _cursorIndexOfMinimumFare = CursorUtil.getColumnIndexOrThrow(_cursor, "minimum_fare");
      final int _cursorIndexOfPeakMultiplier = CursorUtil.getColumnIndexOrThrow(_cursor, "peak_multiplier");
      final int _cursorIndexOfTrafficMultiplier = CursorUtil.getColumnIndexOrThrow(_cursor, "traffic_multiplier");
      final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "last_updated");
      final FareRate _result;
      if (_cursor.moveToFirst()) {
        _result = new FareRate();
        _result.id = _cursor.getInt(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfTransportType)) {
          _result.transportType = null;
        } else {
          _result.transportType = _cursor.getString(_cursorIndexOfTransportType);
        }
        _result.baseRate = _cursor.getDouble(_cursorIndexOfBaseRate);
        _result.perKmRate = _cursor.getDouble(_cursorIndexOfPerKmRate);
        _result.minimumFare = _cursor.getDouble(_cursorIndexOfMinimumFare);
        _result.peakMultiplier = _cursor.getDouble(_cursorIndexOfPeakMultiplier);
        _result.trafficMultiplier = _cursor.getDouble(_cursorIndexOfTrafficMultiplier);
        _result.lastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public long getLatestTimestamp() {
    final String _sql = "SELECT MAX(last_updated) FROM fare_rates";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final long _result;
      if (_cursor.moveToFirst()) {
        _result = _cursor.getLong(0);
      } else {
        _result = 0L;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
