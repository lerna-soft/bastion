package com.bastion.app.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile HostDao _hostDao;

  private volatile AppSettingsDao _appSettingsDao;

  private volatile SshKeyDao _sshKeyDao;

  private volatile ApiKeyDao _apiKeyDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `hosts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `hostname` TEXT NOT NULL, `port` INTEGER NOT NULL, `username` TEXT NOT NULL, `authType` TEXT NOT NULL, `useAgentForwarding` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `app_settings` (`id` INTEGER NOT NULL, `serverName` TEXT NOT NULL, `timezone` TEXT NOT NULL, `language` TEXT NOT NULL, `fontSize` REAL NOT NULL, `twoFactorEnabled` INTEGER NOT NULL, `sessionTimeout` TEXT NOT NULL, `webhookUrl` TEXT NOT NULL, `emailAlerts` INTEGER NOT NULL, `colorMode` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `ssh_keys` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `servers` TEXT NOT NULL, `created` INTEGER NOT NULL, `lastUsed` INTEGER NOT NULL, `isActive` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `api_keys` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `label` TEXT NOT NULL, `keyValue` TEXT NOT NULL, `created` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'ea63dad880227ed257b73ad40410950e')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `hosts`");
        db.execSQL("DROP TABLE IF EXISTS `app_settings`");
        db.execSQL("DROP TABLE IF EXISTS `ssh_keys`");
        db.execSQL("DROP TABLE IF EXISTS `api_keys`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsHosts = new HashMap<String, TableInfo.Column>(9);
        _columnsHosts.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHosts.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHosts.put("hostname", new TableInfo.Column("hostname", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHosts.put("port", new TableInfo.Column("port", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHosts.put("username", new TableInfo.Column("username", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHosts.put("authType", new TableInfo.Column("authType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHosts.put("useAgentForwarding", new TableInfo.Column("useAgentForwarding", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHosts.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHosts.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHosts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHosts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHosts = new TableInfo("hosts", _columnsHosts, _foreignKeysHosts, _indicesHosts);
        final TableInfo _existingHosts = TableInfo.read(db, "hosts");
        if (!_infoHosts.equals(_existingHosts)) {
          return new RoomOpenHelper.ValidationResult(false, "hosts(com.bastion.app.data.Host).\n"
                  + " Expected:\n" + _infoHosts + "\n"
                  + " Found:\n" + _existingHosts);
        }
        final HashMap<String, TableInfo.Column> _columnsAppSettings = new HashMap<String, TableInfo.Column>(10);
        _columnsAppSettings.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("serverName", new TableInfo.Column("serverName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("timezone", new TableInfo.Column("timezone", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("language", new TableInfo.Column("language", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("fontSize", new TableInfo.Column("fontSize", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("twoFactorEnabled", new TableInfo.Column("twoFactorEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("sessionTimeout", new TableInfo.Column("sessionTimeout", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("webhookUrl", new TableInfo.Column("webhookUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("emailAlerts", new TableInfo.Column("emailAlerts", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAppSettings.put("colorMode", new TableInfo.Column("colorMode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAppSettings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAppSettings = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAppSettings = new TableInfo("app_settings", _columnsAppSettings, _foreignKeysAppSettings, _indicesAppSettings);
        final TableInfo _existingAppSettings = TableInfo.read(db, "app_settings");
        if (!_infoAppSettings.equals(_existingAppSettings)) {
          return new RoomOpenHelper.ValidationResult(false, "app_settings(com.bastion.app.data.AppSettings).\n"
                  + " Expected:\n" + _infoAppSettings + "\n"
                  + " Found:\n" + _existingAppSettings);
        }
        final HashMap<String, TableInfo.Column> _columnsSshKeys = new HashMap<String, TableInfo.Column>(8);
        _columnsSshKeys.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSshKeys.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSshKeys.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSshKeys.put("fingerprint", new TableInfo.Column("fingerprint", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSshKeys.put("servers", new TableInfo.Column("servers", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSshKeys.put("created", new TableInfo.Column("created", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSshKeys.put("lastUsed", new TableInfo.Column("lastUsed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSshKeys.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSshKeys = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSshKeys = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSshKeys = new TableInfo("ssh_keys", _columnsSshKeys, _foreignKeysSshKeys, _indicesSshKeys);
        final TableInfo _existingSshKeys = TableInfo.read(db, "ssh_keys");
        if (!_infoSshKeys.equals(_existingSshKeys)) {
          return new RoomOpenHelper.ValidationResult(false, "ssh_keys(com.bastion.app.data.SshKey).\n"
                  + " Expected:\n" + _infoSshKeys + "\n"
                  + " Found:\n" + _existingSshKeys);
        }
        final HashMap<String, TableInfo.Column> _columnsApiKeys = new HashMap<String, TableInfo.Column>(4);
        _columnsApiKeys.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsApiKeys.put("label", new TableInfo.Column("label", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsApiKeys.put("keyValue", new TableInfo.Column("keyValue", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsApiKeys.put("created", new TableInfo.Column("created", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysApiKeys = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesApiKeys = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoApiKeys = new TableInfo("api_keys", _columnsApiKeys, _foreignKeysApiKeys, _indicesApiKeys);
        final TableInfo _existingApiKeys = TableInfo.read(db, "api_keys");
        if (!_infoApiKeys.equals(_existingApiKeys)) {
          return new RoomOpenHelper.ValidationResult(false, "api_keys(com.bastion.app.data.ApiKey).\n"
                  + " Expected:\n" + _infoApiKeys + "\n"
                  + " Found:\n" + _existingApiKeys);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "ea63dad880227ed257b73ad40410950e", "a3d764ed361658c484f24382d77e2cff");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "hosts","app_settings","ssh_keys","api_keys");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `hosts`");
      _db.execSQL("DELETE FROM `app_settings`");
      _db.execSQL("DELETE FROM `ssh_keys`");
      _db.execSQL("DELETE FROM `api_keys`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(HostDao.class, HostDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AppSettingsDao.class, AppSettingsDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SshKeyDao.class, SshKeyDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ApiKeyDao.class, ApiKeyDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public HostDao hostDao() {
    if (_hostDao != null) {
      return _hostDao;
    } else {
      synchronized(this) {
        if(_hostDao == null) {
          _hostDao = new HostDao_Impl(this);
        }
        return _hostDao;
      }
    }
  }

  @Override
  public AppSettingsDao appSettingsDao() {
    if (_appSettingsDao != null) {
      return _appSettingsDao;
    } else {
      synchronized(this) {
        if(_appSettingsDao == null) {
          _appSettingsDao = new AppSettingsDao_Impl(this);
        }
        return _appSettingsDao;
      }
    }
  }

  @Override
  public SshKeyDao sshKeyDao() {
    if (_sshKeyDao != null) {
      return _sshKeyDao;
    } else {
      synchronized(this) {
        if(_sshKeyDao == null) {
          _sshKeyDao = new SshKeyDao_Impl(this);
        }
        return _sshKeyDao;
      }
    }
  }

  @Override
  public ApiKeyDao apiKeyDao() {
    if (_apiKeyDao != null) {
      return _apiKeyDao;
    } else {
      synchronized(this) {
        if(_apiKeyDao == null) {
          _apiKeyDao = new ApiKeyDao_Impl(this);
        }
        return _apiKeyDao;
      }
    }
  }
}
