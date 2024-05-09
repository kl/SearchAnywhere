package se.kalind.searchanywhere.presentation

import android.graphics.drawable.Drawable
import se.kalind.searchanywhere.domain.repo.AppIconDrawable

fun AppIconDrawable.asDrawable(): Drawable =
    this.icon as? Drawable ?: error("AppIconDrawable icon was not a Drawable")