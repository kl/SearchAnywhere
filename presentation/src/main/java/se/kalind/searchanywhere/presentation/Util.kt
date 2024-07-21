package se.kalind.searchanywhere.presentation

import android.graphics.drawable.Drawable
import android.util.Log
import se.kalind.searchanywhere.domain.repo.AppIconDrawable

fun AppIconDrawable.asDrawable(): Drawable =
    this.icon as? Drawable ?: error("AppIconDrawable icon was not a Drawable")

fun <T> dbg(what: T): T {
    Log.d("DBG", what.toString());
    return what
}
