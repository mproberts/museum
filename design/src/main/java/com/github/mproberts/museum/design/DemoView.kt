package com.github.mproberts.museum.design

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.TextView
import com.github.mproberts.museum.annotations.Exhibit

@Exhibit("Demo View", "Misc", [])
fun demoDemoView(parent: ViewGroup) = DemoView(parent.context).also {
    it.text = "Another Text View Demo"
    parent.addView(it)
}

class DemoView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TextView(context, attrs, defStyleAttr) {

}