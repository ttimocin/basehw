package com.taytek.basehw.ui.util

import com.taytek.basehw.R

object AvatarUtil {
    /**
     * List of standard avatar resource IDs.
     * Order here matches the avatarId (index + 1).
     */
    val DEFAULT_AVATARS = listOf(
        R.drawable.direksiyon, // ID 1
        R.drawable.e30,        // ID 2
        R.drawable.ferrari,    // ID 3
        R.drawable.golf,       // ID 4
        R.drawable.hw,         // ID 5
        R.drawable.merso,      // ID 6
        R.drawable.supra,      // ID 7
        R.drawable.vw,         // ID 8
        R.drawable.audi1,      // ID 9
        R.drawable.audi2       // ID 10
    )

    /**
     * Returns the drawable resource for a given avatarId.
     * @param avatarId The ID stored in DB (1-based index)
     * @return Resource ID, or a default fallback if index is out of bounds
     */
    fun getAvatarResource(avatarId: Int): Int {
        val index = avatarId - 1
        return if (index in DEFAULT_AVATARS.indices) {
            DEFAULT_AVATARS[index]
        } else {
            // Default fallback if somehow an invalid ID is passed
            DEFAULT_AVATARS[0]
        }
    }

    /**
     * Returns the total number of standard avatars.
     */
    fun getAvatarCount(): Int = DEFAULT_AVATARS.size
}
