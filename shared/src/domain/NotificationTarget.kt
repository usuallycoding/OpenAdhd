package com.usuallycoding.openadhd.domain

/**
 * What identifies the tap target of one reminder notification. A system pending intent is told apart by its data and
 * request code but not by its extras, so each entry gets its own, or every notification would open the same entry.
 */
object NotificationTarget {
    /** The data address of the entry's tap target, such as `openadhd://entry/<id>`. */
    fun uri(entryId: String): String = "openadhd://entry/$entryId"

    /** The request code of the entry's tap target. */
    fun requestCode(entryId: String): Int = entryId.hashCode()
}
