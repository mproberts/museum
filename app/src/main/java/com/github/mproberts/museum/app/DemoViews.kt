package com.github.mproberts.museum.app

import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import com.github.mproberts.museum.showcase.R
import com.github.mproberts.museum.annotations.Exhibit

@Exhibit("My Awesome View", "Text", ["Text"], "This is the story of a girl, who cried a river and drowned the whole world")
fun myAwesomeView(parent: ViewGroup) = TextView(parent.context)
    .also {
        it.text = "This is a test"
        it.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            parent.context.resources.getDimensionPixelSize(R.dimen.title_text_size).toFloat()
        )
        it.setTextColor(0xff000000.toInt())
        parent.addView(it)
    }

@Exhibit("Dark Titlebar", "Titlebars", ["Titlebar", "Dark"])
fun myAwesomeView2(parent: ViewGroup) = TextView(parent.context)
    .also {
        it.text = "This is a test"
        parent.addView(it)
    }

@Exhibit("Light Titlebar", "Titlebars", ["Titlebar", "Light"], "This is the story of a girl, who cried a river and drowned the whole world")
fun myAwesomeView3(parent: ViewGroup) = TextView(parent.context)
    .also {
        it.text = "This is a test"
        parent.addView(it)
    }

