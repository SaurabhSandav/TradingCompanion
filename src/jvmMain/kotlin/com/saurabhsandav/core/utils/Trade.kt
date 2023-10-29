package com.saurabhsandav.core.utils

import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.TradingProfile
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.TradingRecord
import kotlinx.coroutines.flow.*

suspend fun FlowSettings.putCurrentTradingProfileId(id: Long) {
    putLong(PrefKeys.CurrentTradingProfile, id)
}

internal fun FlowSettings.getCurrentTradingProfile(tradingProfiles: TradingProfiles): Flow<TradingProfile> {
    return getLongOrNullFlow(PrefKeys.CurrentTradingProfile).flatMapLatest { profileId ->

        if (profileId == null) {
            // Current profile not set. Set first profile (from stored profiles) as current.
            putCurrentTradingProfileId(tradingProfiles.allProfiles.first().first().id)
            return@flatMapLatest flowOf(null)
        }

        val profileExists = tradingProfiles.exists(profileId).first()

        if (!profileExists) {
            // Profile doesn't exist. Set first profile (from stored profiles) as current.
            putCurrentTradingProfileId(tradingProfiles.allProfiles.first().first().id)
            return@flatMapLatest flowOf(null)
        }

        tradingProfiles.getProfile(profileId)
    }.filterNotNull()
}

internal fun FlowSettings.getCurrentTradingRecord(
    tradingProfiles: TradingProfiles,
): Flow<TradingRecord> {
    return getCurrentTradingProfile(tradingProfiles).map { tradingProfiles.getRecord(it.id) }
}
