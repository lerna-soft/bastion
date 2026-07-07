package com.bastion.app

import android.app.Application
import com.bastion.app.data.AppDatabase
import com.bastion.app.data.VaultRepository
import com.bastion.app.data.crypto.SecretsStore

class BastionApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var secretsStore: SecretsStore
        private set
    lateinit var repository: VaultRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        secretsStore = SecretsStore(this)
        repository = VaultRepository(database, secretsStore)
    }
}
