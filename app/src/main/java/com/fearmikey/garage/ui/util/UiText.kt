package com.fearmikey.garage.ui.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * User-visible text a ViewModel can hold without resolving it, so the UI shows it in the current
 * app language (and unit tests can assert on the resource id instead of English text).
 * [args] may themselves be [UiText] (e.g. a translated sensor name inside "Reading %1$s…").
 */
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    /** Text that has no translation, e.g. a raw exception message. */
    data class Raw(val text: String) : UiText

    companion object {
        fun res(@StringRes id: Int, vararg args: Any): UiText = Res(id, args.toList())
    }
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Raw -> text
    is UiText.Res -> {
        val resolved = args.map { if (it is UiText) it.asString() else it }
        stringResource(id, *resolved.toTypedArray())
    }
}

fun UiText.asString(context: Context): String = when (this) {
    is UiText.Raw -> text
    is UiText.Res -> context.getString(id, *args.map { if (it is UiText) it.asString(context) else it }.toTypedArray())
}
