package com.usuallycoding.openadhd.domain

/** The version line at the end of Settings, which every platform builds the same way from what its installed app reports. */
object AppVersion {
    /** What the label says when the app has no version of its own, such as the desktop app run from the build tool. */
    const val Unpackaged = "OpenAdhd dev"

    /**
     * "OpenAdhd 1.0.0 (1)" for a [name] and a [build], "OpenAdhd 1.0.0" without a build, and [Unpackaged] when the
     * name is missing or blank. A blank build is left out rather than shown as "()".
     */
    fun label(name: String?, build: String? = null): String {
        val version = name?.trim().orEmpty()
        if (version.isEmpty()) return Unpackaged
        val number = build?.trim().orEmpty()
        return if (number.isEmpty()) "OpenAdhd $version" else "OpenAdhd $version ($number)"
    }
}
