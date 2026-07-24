package com.yy.medtrace.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.yy.medtrace.R
import com.yy.medtrace.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MedicationWidget : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_medication)
            
            val today = LocalDate.now()
            val dateText = today.format(DateTimeFormatter.ofPattern("M月d日 EEEE"))
            views.setTextViewText(R.id.widget_date, dateText)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = AppDatabase.getDatabase(context)
                    val todos = database.healthTodoDao().getPendingByDate(today)
                    
                    if (todos.isEmpty()) {
                        views.setTextViewText(R.id.widget_status, "今日无待服药物")
                        views.setViewVisibility(R.id.widget_todo_list, android.view.View.GONE)
                    } else {
                        views.setTextViewText(R.id.widget_status, "今日待服 ${todos.size} 项")
                        views.setViewVisibility(R.id.widget_todo_list, android.view.View.VISIBLE)
                        
                        val todoTexts = todos.take(3).joinToString("\n") { todo ->
                            "• ${todo.content}"
                        }
                        views.setTextViewText(R.id.widget_todo_list, todoTexts)
                    }
                    
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    views.setTextViewText(R.id.widget_status, "加载失败")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}