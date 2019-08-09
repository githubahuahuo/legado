package io.legado.app.ui.search

import android.app.Application
import android.util.Log
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.WebBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.concurrent.CancellationException

class SearchViewModel(application: Application) : BaseViewModel(application) {
    private var task: Coroutine<*>? = null

    var searchPage = 0

    fun search(
        key: String,
        start: ((startTime: Long) -> Unit)? = null,
        finally: (() -> Unit)? = null
    ) {
        if (key.isEmpty()) return
        task?.cancel()
        start?.invoke(System.currentTimeMillis())
        task = execute {
            //onCleared时自动取消
            val bookSourceList = App.db.bookSourceDao().allEnabled
            for (item in bookSourceList) {
                //task取消时自动取消 by （scope = this@execute）
                WebBook(item).searchBook(key, searchPage, scope = this@execute)
                    .timeout(30000L)
                    .onSuccess(Dispatchers.IO) {
                        it?.let { list ->
                            App.db.searchBookDao().insert(*list.toTypedArray())
                        }
                    }
            }

        }.onError {
            it.printStackTrace()
        }

        task?.invokeOnCompletion {
            finally?.invoke()
        }


    }

    fun stop() {
        task?.cancel()
    }

}
