package com.yy.medtrace.common

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

fun <T> Flow<T>.asResult(): Flow<Result<T>> {
    return this
        .map<T, Result<T>> { Result.Success(it) }
        .onStart { emit(Result.Loading) }
        .catch { e ->
            Log.e("ErrorHandler", "Error occurred", e)
            emit(Result.Error(e.message ?: "Unknown error", e))
        }
}

fun <T> Flow<T>.asResultWithoutLoading(): Flow<Result<T>> {
    return this
        .map<T, Result<T>> { Result.Success(it) }
        .catch { e ->
            Log.e("ErrorHandler", "Error occurred", e)
            emit(Result.Error(e.message ?: "Unknown error", e))
        }
}