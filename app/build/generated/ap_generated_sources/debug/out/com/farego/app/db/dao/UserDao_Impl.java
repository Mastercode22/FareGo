package com.farego.app.db.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.farego.app.db.entity.User;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class UserDao_Impl implements UserDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<User> __insertionAdapterOfUser;

  private final EntityInsertionAdapter<User> __insertionAdapterOfUser_1;

  private final EntityDeletionOrUpdateAdapter<User> __updateAdapterOfUser;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLastLogin;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public UserDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUser = new EntityInsertionAdapter<User>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `users` (`id`,`username`,`password_hash`,`email`,`is_admin`,`created_at`,`preferred_transport`,`last_login`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final User entity) {
        statement.bindLong(1, entity.id);
        if (entity.username == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.username);
        }
        if (entity.passwordHash == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.passwordHash);
        }
        if (entity.email == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.email);
        }
        final int _tmp = entity.isAdmin ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.createdAt);
        if (entity.preferredTransport == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.preferredTransport);
        }
        statement.bindLong(8, entity.lastLogin);
      }
    };
    this.__insertionAdapterOfUser_1 = new EntityInsertionAdapter<User>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `users` (`id`,`username`,`password_hash`,`email`,`is_admin`,`created_at`,`preferred_transport`,`last_login`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final User entity) {
        statement.bindLong(1, entity.id);
        if (entity.username == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.username);
        }
        if (entity.passwordHash == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.passwordHash);
        }
        if (entity.email == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.email);
        }
        final int _tmp = entity.isAdmin ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.createdAt);
        if (entity.preferredTransport == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.preferredTransport);
        }
        statement.bindLong(8, entity.lastLogin);
      }
    };
    this.__updateAdapterOfUser = new EntityDeletionOrUpdateAdapter<User>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `users` SET `id` = ?,`username` = ?,`password_hash` = ?,`email` = ?,`is_admin` = ?,`created_at` = ?,`preferred_transport` = ?,`last_login` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement, final User entity) {
        statement.bindLong(1, entity.id);
        if (entity.username == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.username);
        }
        if (entity.passwordHash == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.passwordHash);
        }
        if (entity.email == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.email);
        }
        final int _tmp = entity.isAdmin ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.createdAt);
        if (entity.preferredTransport == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.preferredTransport);
        }
        statement.bindLong(8, entity.lastLogin);
        statement.bindLong(9, entity.id);
      }
    };
    this.__preparedStmtOfUpdateLastLogin = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE users SET last_login = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM users WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public long insertUser(final User user) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfUser.insertAndReturnId(user);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public long insert(final User user) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      final long _result = __insertionAdapterOfUser_1.insertAndReturnId(user);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateUser(final User user) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfUser.handle(user);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateLastLogin(final int id, final long time) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLastLogin.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, time);
    _argIndex = 2;
    _stmt.bindLong(_argIndex, id);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfUpdateLastLogin.release(_stmt);
    }
  }

  @Override
  public void deleteById(final int userId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, userId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfDeleteById.release(_stmt);
    }
  }

  @Override
  public User login(final String username, final String hash) {
    final String _sql = "SELECT * FROM users WHERE username = ? AND password_hash = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (username == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, username);
    }
    _argIndex = 2;
    if (hash == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, hash);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
      final int _cursorIndexOfPasswordHash = CursorUtil.getColumnIndexOrThrow(_cursor, "password_hash");
      final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
      final int _cursorIndexOfIsAdmin = CursorUtil.getColumnIndexOrThrow(_cursor, "is_admin");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
      final int _cursorIndexOfPreferredTransport = CursorUtil.getColumnIndexOrThrow(_cursor, "preferred_transport");
      final int _cursorIndexOfLastLogin = CursorUtil.getColumnIndexOrThrow(_cursor, "last_login");
      final User _result;
      if (_cursor.moveToFirst()) {
        _result = new User();
        _result.id = _cursor.getInt(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfUsername)) {
          _result.username = null;
        } else {
          _result.username = _cursor.getString(_cursorIndexOfUsername);
        }
        if (_cursor.isNull(_cursorIndexOfPasswordHash)) {
          _result.passwordHash = null;
        } else {
          _result.passwordHash = _cursor.getString(_cursorIndexOfPasswordHash);
        }
        if (_cursor.isNull(_cursorIndexOfEmail)) {
          _result.email = null;
        } else {
          _result.email = _cursor.getString(_cursorIndexOfEmail);
        }
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsAdmin);
        _result.isAdmin = _tmp != 0;
        _result.createdAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        if (_cursor.isNull(_cursorIndexOfPreferredTransport)) {
          _result.preferredTransport = null;
        } else {
          _result.preferredTransport = _cursor.getString(_cursorIndexOfPreferredTransport);
        }
        _result.lastLogin = _cursor.getLong(_cursorIndexOfLastLogin);
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
  public User findByUsername(final String username) {
    final String _sql = "SELECT * FROM users WHERE username = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (username == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, username);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
      final int _cursorIndexOfPasswordHash = CursorUtil.getColumnIndexOrThrow(_cursor, "password_hash");
      final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
      final int _cursorIndexOfIsAdmin = CursorUtil.getColumnIndexOrThrow(_cursor, "is_admin");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
      final int _cursorIndexOfPreferredTransport = CursorUtil.getColumnIndexOrThrow(_cursor, "preferred_transport");
      final int _cursorIndexOfLastLogin = CursorUtil.getColumnIndexOrThrow(_cursor, "last_login");
      final User _result;
      if (_cursor.moveToFirst()) {
        _result = new User();
        _result.id = _cursor.getInt(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfUsername)) {
          _result.username = null;
        } else {
          _result.username = _cursor.getString(_cursorIndexOfUsername);
        }
        if (_cursor.isNull(_cursorIndexOfPasswordHash)) {
          _result.passwordHash = null;
        } else {
          _result.passwordHash = _cursor.getString(_cursorIndexOfPasswordHash);
        }
        if (_cursor.isNull(_cursorIndexOfEmail)) {
          _result.email = null;
        } else {
          _result.email = _cursor.getString(_cursorIndexOfEmail);
        }
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsAdmin);
        _result.isAdmin = _tmp != 0;
        _result.createdAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        if (_cursor.isNull(_cursorIndexOfPreferredTransport)) {
          _result.preferredTransport = null;
        } else {
          _result.preferredTransport = _cursor.getString(_cursorIndexOfPreferredTransport);
        }
        _result.lastLogin = _cursor.getLong(_cursorIndexOfLastLogin);
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
  public User findById(final int userId) {
    final String _sql = "SELECT * FROM users WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, userId);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
      final int _cursorIndexOfPasswordHash = CursorUtil.getColumnIndexOrThrow(_cursor, "password_hash");
      final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
      final int _cursorIndexOfIsAdmin = CursorUtil.getColumnIndexOrThrow(_cursor, "is_admin");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
      final int _cursorIndexOfPreferredTransport = CursorUtil.getColumnIndexOrThrow(_cursor, "preferred_transport");
      final int _cursorIndexOfLastLogin = CursorUtil.getColumnIndexOrThrow(_cursor, "last_login");
      final User _result;
      if (_cursor.moveToFirst()) {
        _result = new User();
        _result.id = _cursor.getInt(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfUsername)) {
          _result.username = null;
        } else {
          _result.username = _cursor.getString(_cursorIndexOfUsername);
        }
        if (_cursor.isNull(_cursorIndexOfPasswordHash)) {
          _result.passwordHash = null;
        } else {
          _result.passwordHash = _cursor.getString(_cursorIndexOfPasswordHash);
        }
        if (_cursor.isNull(_cursorIndexOfEmail)) {
          _result.email = null;
        } else {
          _result.email = _cursor.getString(_cursorIndexOfEmail);
        }
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsAdmin);
        _result.isAdmin = _tmp != 0;
        _result.createdAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        if (_cursor.isNull(_cursorIndexOfPreferredTransport)) {
          _result.preferredTransport = null;
        } else {
          _result.preferredTransport = _cursor.getString(_cursorIndexOfPreferredTransport);
        }
        _result.lastLogin = _cursor.getLong(_cursorIndexOfLastLogin);
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
  public User findByCredentials(final String username, final String hash) {
    final String _sql = "SELECT * FROM users WHERE username = ? AND password_hash = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (username == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, username);
    }
    _argIndex = 2;
    if (hash == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, hash);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
      final int _cursorIndexOfPasswordHash = CursorUtil.getColumnIndexOrThrow(_cursor, "password_hash");
      final int _cursorIndexOfEmail = CursorUtil.getColumnIndexOrThrow(_cursor, "email");
      final int _cursorIndexOfIsAdmin = CursorUtil.getColumnIndexOrThrow(_cursor, "is_admin");
      final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
      final int _cursorIndexOfPreferredTransport = CursorUtil.getColumnIndexOrThrow(_cursor, "preferred_transport");
      final int _cursorIndexOfLastLogin = CursorUtil.getColumnIndexOrThrow(_cursor, "last_login");
      final User _result;
      if (_cursor.moveToFirst()) {
        _result = new User();
        _result.id = _cursor.getInt(_cursorIndexOfId);
        if (_cursor.isNull(_cursorIndexOfUsername)) {
          _result.username = null;
        } else {
          _result.username = _cursor.getString(_cursorIndexOfUsername);
        }
        if (_cursor.isNull(_cursorIndexOfPasswordHash)) {
          _result.passwordHash = null;
        } else {
          _result.passwordHash = _cursor.getString(_cursorIndexOfPasswordHash);
        }
        if (_cursor.isNull(_cursorIndexOfEmail)) {
          _result.email = null;
        } else {
          _result.email = _cursor.getString(_cursorIndexOfEmail);
        }
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsAdmin);
        _result.isAdmin = _tmp != 0;
        _result.createdAt = _cursor.getLong(_cursorIndexOfCreatedAt);
        if (_cursor.isNull(_cursorIndexOfPreferredTransport)) {
          _result.preferredTransport = null;
        } else {
          _result.preferredTransport = _cursor.getString(_cursorIndexOfPreferredTransport);
        }
        _result.lastLogin = _cursor.getLong(_cursorIndexOfLastLogin);
      } else {
        _result = null;
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
