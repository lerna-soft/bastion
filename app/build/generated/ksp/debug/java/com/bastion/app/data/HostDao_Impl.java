package com.bastion.app.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HostDao_Impl implements HostDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Host> __insertionAdapterOfHost;

  private final EntityDeletionOrUpdateAdapter<Host> __deletionAdapterOfHost;

  private final EntityDeletionOrUpdateAdapter<Host> __updateAdapterOfHost;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public HostDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHost = new EntityInsertionAdapter<Host>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `hosts` (`id`,`name`,`hostname`,`port`,`username`,`authType`,`useAgentForwarding`,`createdAt`,`updatedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Host entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getHostname());
        statement.bindLong(4, entity.getPort());
        statement.bindString(5, entity.getUsername());
        statement.bindString(6, __AuthType_enumToString(entity.getAuthType()));
        final int _tmp = entity.getUseAgentForwarding() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getCreatedAt());
        statement.bindLong(9, entity.getUpdatedAt());
      }
    };
    this.__deletionAdapterOfHost = new EntityDeletionOrUpdateAdapter<Host>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `hosts` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Host entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfHost = new EntityDeletionOrUpdateAdapter<Host>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `hosts` SET `id` = ?,`name` = ?,`hostname` = ?,`port` = ?,`username` = ?,`authType` = ?,`useAgentForwarding` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Host entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getHostname());
        statement.bindLong(4, entity.getPort());
        statement.bindString(5, entity.getUsername());
        statement.bindString(6, __AuthType_enumToString(entity.getAuthType()));
        final int _tmp = entity.getUseAgentForwarding() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getCreatedAt());
        statement.bindLong(9, entity.getUpdatedAt());
        statement.bindLong(10, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM hosts WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final Host host, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfHost.insertAndReturnId(host);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final Host host, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfHost.handle(host);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final Host host, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfHost.handle(host);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Host>> getAllHosts() {
    final String _sql = "SELECT * FROM hosts ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"hosts"}, new Callable<List<Host>>() {
      @Override
      @NonNull
      public List<Host> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfHostname = CursorUtil.getColumnIndexOrThrow(_cursor, "hostname");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfAuthType = CursorUtil.getColumnIndexOrThrow(_cursor, "authType");
          final int _cursorIndexOfUseAgentForwarding = CursorUtil.getColumnIndexOrThrow(_cursor, "useAgentForwarding");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<Host> _result = new ArrayList<Host>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Host _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpHostname;
            _tmpHostname = _cursor.getString(_cursorIndexOfHostname);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final AuthType _tmpAuthType;
            _tmpAuthType = __AuthType_stringToEnum(_cursor.getString(_cursorIndexOfAuthType));
            final boolean _tmpUseAgentForwarding;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfUseAgentForwarding);
            _tmpUseAgentForwarding = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new Host(_tmpId,_tmpName,_tmpHostname,_tmpPort,_tmpUsername,_tmpAuthType,_tmpUseAgentForwarding,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object getHostById(final long id, final Continuation<? super Host> $completion) {
    final String _sql = "SELECT * FROM hosts WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Host>() {
      @Override
      @Nullable
      public Host call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfHostname = CursorUtil.getColumnIndexOrThrow(_cursor, "hostname");
          final int _cursorIndexOfPort = CursorUtil.getColumnIndexOrThrow(_cursor, "port");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfAuthType = CursorUtil.getColumnIndexOrThrow(_cursor, "authType");
          final int _cursorIndexOfUseAgentForwarding = CursorUtil.getColumnIndexOrThrow(_cursor, "useAgentForwarding");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final Host _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpHostname;
            _tmpHostname = _cursor.getString(_cursorIndexOfHostname);
            final int _tmpPort;
            _tmpPort = _cursor.getInt(_cursorIndexOfPort);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final AuthType _tmpAuthType;
            _tmpAuthType = __AuthType_stringToEnum(_cursor.getString(_cursorIndexOfAuthType));
            final boolean _tmpUseAgentForwarding;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfUseAgentForwarding);
            _tmpUseAgentForwarding = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new Host(_tmpId,_tmpName,_tmpHostname,_tmpPort,_tmpUsername,_tmpAuthType,_tmpUseAgentForwarding,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __AuthType_enumToString(@NonNull final AuthType _value) {
    switch (_value) {
      case PASSWORD: return "PASSWORD";
      case PUBLIC_KEY: return "PUBLIC_KEY";
      case AGENT_FORWARD: return "AGENT_FORWARD";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private AuthType __AuthType_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "PASSWORD": return AuthType.PASSWORD;
      case "PUBLIC_KEY": return AuthType.PUBLIC_KEY;
      case "AGENT_FORWARD": return AuthType.AGENT_FORWARD;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
