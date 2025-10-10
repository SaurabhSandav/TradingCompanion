package com.saurabhsandav.core.di

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.datastore.DataStoreSettings
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.AppPaths
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import okio.Path.Companion.toOkioPath
import kotlin.coroutines.CoroutineContext

@ContributesTo(AppScope::class)
@BindingContainer
object CommonBindings {

    @IOCoroutineContext
    @Provides
    fun provideIOCoroutineContext(appDispatchers: AppDispatchers): CoroutineContext = appDispatchers.IO

    @SingleIn(AppScope::class)
    @AppPrefs
    @Provides
    fun provideAppPrefs(
        @AppCoroutineScope appScope: CoroutineScope,
        @IOCoroutineContext ioCoroutineContext: CoroutineContext,
        appPaths: AppPaths,
    ): FlowSettings = DataStoreSettings(
        datastore = PreferenceDataStoreFactory.createWithPath(
            scope = appScope + ioCoroutineContext,
            corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() }),
            produceFile = { appPaths.prefsPath.resolve("app.preferences_pb").toOkioPath() },
        ),
    )
}
