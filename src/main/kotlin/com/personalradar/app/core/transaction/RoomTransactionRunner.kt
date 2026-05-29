package com.personalradar.app.core.transaction

import androidx.room.withTransaction
import com.personalradar.app.core.database.AppDatabase

class RoomTransactionRunner(
    private val database: AppDatabase
) : TransactionRunner {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return database.withTransaction { block() }
    }
}
