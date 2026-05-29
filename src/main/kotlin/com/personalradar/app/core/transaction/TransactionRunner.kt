package com.personalradar.app.core.transaction

interface TransactionRunner {
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}
