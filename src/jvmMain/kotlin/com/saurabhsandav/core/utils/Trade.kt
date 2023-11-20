package com.saurabhsandav.core.utils

import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.TradingProfile
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.TradingRecord
import com.saurabhsandav.core.trades.model.ProfileId
import kotlinx.coroutines.flow.*

suspend fun FlowSettings.putCurrentTradingProfileId(id: ProfileId) {
    putLong(PrefKeys.CurrentTradingProfile, id.value)
}

internal fun FlowSettings.getCurrentTradingProfile(tradingProfiles: TradingProfiles): Flow<TradingProfile> {
    return getLongOrNullFlow(PrefKeys.CurrentTradingProfile).flatMapLatest { profileId ->

        if (profileId == null) {

            // Current profile not set. Set default profile as current.
            putCurrentTradingProfileId(tradingProfiles.getDefaultProfile().first().id)

            // Return empty flow. New emission from prefs will follow.
            return@flatMapLatest emptyFlow()
        }

        val id = ProfileId(profileId)

        val profileExists = tradingProfiles.exists(id)

        if (!profileExists) {

            // Profile doesn't exist. Set default profile as current.
            putCurrentTradingProfileId(tradingProfiles.allProfiles.first().first().id)

            // Return empty flow. New emission from prefs will follow.
            return@flatMapLatest emptyFlow()
        }

        tradingProfiles.getProfile(id)
    }
}

internal fun FlowSettings.getCurrentTradingRecord(
    tradingProfiles: TradingProfiles,
): Flow<TradingRecord> {
    return getCurrentTradingProfile(tradingProfiles).map { tradingProfiles.getRecord(it.id) }
}
