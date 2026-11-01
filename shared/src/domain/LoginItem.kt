package com.usuallycoding.openadhd.domain

/**
 * Whether the system starts the app when the user logs in. The system holds the truth, so [isEnabled] asks it each
 * time instead of remembering what was last saved. A platform without one passes no [LoginItem] at all.
 */
interface LoginItem {
    fun isEnabled(): Boolean

    fun setEnabled(enabled: Boolean)
}
