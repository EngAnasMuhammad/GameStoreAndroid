package com.anasdev.gamestoreandroid.domain.model

import androidx.annotation.StringRes
import com.anasdev.gamestoreandroid.R

enum class AppLanguage(
    val tag: String,
    @StringRes val titleResId: Int
) {
    SYSTEM("", R.string.lang_system),
    ENGLISH("en", R.string.lang_english),
    ARABIC("ar", R.string.lang_arabic)
}
