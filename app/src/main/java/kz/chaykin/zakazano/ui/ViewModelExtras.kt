package kz.chaykin.zakazano.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kz.chaykin.zakazano.ZakazanoApp
import kz.chaykin.zakazano.di.AppContainer

/** Достаёт контейнер зависимостей внутри `viewModelFactory { initializer { ... } }`. */
val CreationExtras.appContainer: AppContainer
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ZakazanoApp).container
