/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.ui

import dev.solyanka.utility.*

/** Each valid edit is persisted immediately. Failed edits never replace last-good live settings. */
class InstantSettings(profile:CommandProfile,general:GeneralSettings,
    private val persist:(CommandProfile,GeneralSettings)->String?) {
    var profile=profile;private set
    var general=general;private set
    var feedback=general.chat.feedbackLimit.toString();private set
    var lines=general.chat.lines.toString();private set
    var error:String?=null;private set
    private var savedProfile=profile
    private var savedGeneral=general
    fun pending()=error!=null || profile!=savedProfile || general!=savedGeneral
    fun updateProfile(change:(CommandProfile)->CommandProfile)=edit { profile=change(profile) }
    fun updateGeneral(change:(GeneralSettings)->GeneralSettings)=edit { general=change(general) }
    fun feedback(value:String)=edit { feedback=value }
    fun lines(value:String)=edit { lines=value }
    private fun edit(change:()->Unit):Boolean {
        return try {
            change()
            val candidate=general.copy(chat=general.chat.copy(
                feedbackLimit=feedback.trim().toIntOrNull() ?: throw IllegalArgumentException("Feedback limit must be an integer (0–512)"),
                lines=lines.trim().toIntOrNull() ?: throw IllegalArgumentException("History limit must be an integer (100–5000)")))
            candidate.validate();CommandRules.validate(profile)
            val failure=persist(profile,candidate)
            error=failure
            if(failure==null) { general=candidate;savedProfile=profile;savedGeneral=candidate }
            failure==null
        } catch(e:Exception) { error=e.message ?: "Settings not saved";false }
    }
}
