package com.codinginflow.mvvmtodo.ui.tasks

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.codinginflow.mvvmtodo.data.PreferencesManager
import com.codinginflow.mvvmtodo.data.SortOrder
import com.codinginflow.mvvmtodo.data.Task
import com.codinginflow.mvvmtodo.data.TaskDao
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


class TasksViewModel @ViewModelInject constructor(
    private val  taskDao: TaskDao,
    private val preferencesManager : PreferencesManager,
    @Assisted private val state : SavedStateHandle
) : ViewModel () {

    val searchQuery = state.getLiveData("searchQuery","")


    private val taskEventChannel = Channel<TaskEvent>()
    val taskEvent = taskEventChannel.receiveAsFlow()

    val preferencesFlow = preferencesManager.preferencesFlow
/*
    val sortOrder = MutableStateFlow(SortOrder.BY_DATE)
    val hideCompleted = MutableStateFlow(false)*/

    private  val taskFlow = combine(
        searchQuery.asFlow(),
        preferencesFlow
    ) { query, filterPreferences  ->
        Pair(query,filterPreferences)
    }.flatMapLatest { (query ,filterPreferences) ->
        taskDao.getTasks(query,filterPreferences.sortOrder,filterPreferences.hideCompleted)
    }
    val tasks = taskFlow.asLiveData()

    fun onSortOrderSelected(sortOrder : SortOrder) = viewModelScope.launch {
        preferencesManager.updateSortOrder(sortOrder)
    }

    fun onHideCompletedClick(hideCompleted : Boolean) = viewModelScope.launch {
        preferencesManager.updateHideCompleted(hideCompleted)
    }

fun onTaskSelected(task : Task) = viewModelScope.launch {


    taskEventChannel.send(TaskEvent.NavigateToEditTaskScreen(task))
}

    fun onTaskCheckedChanged(task: Task,isChecked : Boolean) = viewModelScope.launch {
        taskDao.update(task.copy(completed = isChecked))
    }

    fun onTaskSwiped(task: Task) = viewModelScope.launch {
taskDao.delete(task)
     taskEventChannel.send(TaskEvent.ShowUndoDeleteTaskMessage(task))
    }


    fun onUndoDeleteClick (task : Task) = viewModelScope.launch {
        taskDao.insert(task)
    }

    fun onAddNewTaskClick () = viewModelScope.launch {
        taskEventChannel.send(TaskEvent.NavigateToAddTaskScreen)

    }
    sealed class TaskEvent {
        object NavigateToAddTaskScreen : TaskEvent()
        data class  NavigateToEditTaskScreen(val task : Task) : TaskEvent()
        data class ShowUndoDeleteTaskMessage(val task : Task) : TaskEvent()
    }

}
