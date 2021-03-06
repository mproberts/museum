package com.github.mproberts.museum.showcase

import android.content.Context
import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mproberts.museum.annotations.Exhibit
import dalvik.system.DexFile
import java.io.IOException
import java.lang.reflect.Method
import kotlin.concurrent.thread
import kotlin.math.absoluteValue
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import java.lang.reflect.InvocationTargetException

private var globalDensity: Float = 0f

fun Context.configureDensity() {
    globalDensity = resources.displayMetrics.density
}

val Int.dpToPx: Int get() = (globalDensity * this + 0.5f).toInt()
val Float.dpToPx: Int get() = (globalDensity * this + 0.5f).toInt()

data class ExhibitWrapper(
    val title: String,
    val pathComponents: List<String>,
    val tags: List<String>,
    val method: Method,
    val description: String,
    val backgroundColor: Int
) {
    val sortKey = pathComponents.joinToString("/").toLowerCase() + "/" + title.toLowerCase()
}

open class ExhibitRecyclerViewAdapter(private val context: Context, private val backgroundColor: Int, private val primaryTextColor: Int, private val secondaryTextColor: Int): RecyclerView.Adapter<ExhibitRecyclerViewAdapter.ViewHolder>() {

    private var exhibits = emptyList<ExhibitWrapper>()
    private var filter = ""

    protected open fun enumerateExhibitMethods(): List<Method> = try {
        val discoveredClasses = mutableSetOf<Class<*>>()
        val classLoader = context.classLoader
        val df = DexFile(context.packageCodePath)

        Log.d("MUSEUM", context.packageCodePath)
        val entries = df.entries().toList()

        Log.d("MUSEUM", "sze=${entries.size}")

        for (entry in entries) {
            try {
                val shouldInclude = skipPrefices.find { prefix -> entry.startsWith(prefix) } == null
                val loadedClass = if (shouldInclude) {
                    try {
                        classLoader.loadClass(entry)
                    } catch (ex: Throwable) {
                        Log.e("MUSEUM", "loader error", ex)
                        null
                    }
                } else {
                    null
                } ?: continue

                Log.d("MUSEUM", entry)

                val searchList = mutableListOf(loadedClass)

                while (searchList.isNotEmpty()) {
                    val classLookup = searchList.removeAt(0)

                    if (!discoveredClasses.contains(classLookup)) {
                        discoveredClasses.add(classLookup)

                        searchList.addAll(classLookup.classes)
                    }
                }
            } catch (ex: Throwable) {
                Log.e("MUSEUM", "big error", ex)
            }
        }

        val annotatedMethods = discoveredClasses.fold(mutableListOf<Method>()) { previous, value ->
            try {
                previous.addAll(value.declaredMethods.filter { method ->
                    method.annotations.firstOrNull { it is Exhibit } != null
                })
            } catch (ex: Throwable) {
            }

            previous
        }

        annotatedMethods
    } catch (e: IOException) {
        emptyList()
    }

    init {
        thread {
            updateExhibits(enumerateExhibitMethods().map { method ->
                val annotation = method.annotations.first { it is Exhibit } as Exhibit

                ExhibitWrapper(
                    annotation.title,
                    annotation.path.split("/"),
                    annotation.tags.asList(),
                    method,
                    annotation.description,
                    annotation.backgroundColor
                )
            })
        }
    }

    private var parent: RecyclerView? = null
    private val colors = listOf(
        0xffef5350.toInt(),
        0xffec407a.toInt(),
        0xffab47bc.toInt(),
        0xff7e57c2.toInt(),
        0xff5c6bc0.toInt(),
        0xff42a5f5.toInt(),
        0xff29b6f6.toInt(),
        0xff26c6da.toInt(),
        0xff26a69a.toInt(),
        0xff66bb6a.toInt(),
        0xff9ccc65.toInt(),
        0xffd4e157.toInt(),
        0xffffee58.toInt(),
        0xffffca28.toInt(),
        0xffffa726.toInt(),
        0xffff7043.toInt()
    )

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        parent = recyclerView

        notifyDataSetChanged()
    }

    fun updateExhibits(updatedExhibits: List<ExhibitWrapper>) {
        val parent = parent

        if (parent != null) {
            parent.post {
                exhibits = updatedExhibits.sortedBy { it.sortKey }
                notifyDataSetChanged()
            }
        } else {
            exhibits = updatedExhibits.sortedBy { it.sortKey }
        }
    }

    fun updateFilter(filterText: String) {
        val parent = parent

        if (parent != null) {
            parent.post {
                filter = filterText
                notifyDataSetChanged()
            }
        } else {
            filter = filterText
        }
    }

    private fun selectedExhibits() = if (filter.isEmpty()) {
        exhibits
    } else {
        var filtered = exhibits

        filter.toLowerCase().split(" ").forEach { lowercaseFilter ->
            if (lowercaseFilter.isNotBlank()) {
                filtered = filtered.filter { exhibit ->
                    exhibit.title.toLowerCase().indexOf(lowercaseFilter) >= 0
                            || exhibit.pathComponents.firstOrNull {
                        it.toLowerCase().startsWith(lowercaseFilter)
                    } != null
                            || exhibit.tags.firstOrNull {
                        it.toLowerCase().startsWith(lowercaseFilter)
                    } != null
                }
            }
        }

        filtered
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent.context)

    override fun getItemCount() = selectedExhibits().size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(selectedExhibits()[position])
    }

    inner class ViewHolder(context: Context) : RecyclerView.ViewHolder(LinearLayout(context)) {
        private lateinit var exhibit: ExhibitWrapper
        private val root = (itemView as LinearLayout).also {
            it.setBackgroundColor(backgroundColor)
            it.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            it.orientation = LinearLayout.VERTICAL
        }
        private val titleSection = LinearLayout(context).also {
            it.orientation = LinearLayout.HORIZONTAL
            it.gravity = Gravity.CENTER_VERTICAL

            root.addView(it, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                it.topMargin = 8.dpToPx
                it.leftMargin = 8.dpToPx
                it.rightMargin = 8.dpToPx
            })
        }
        private val descriptionView = TextView(context).also {
            it.maxLines = 3
            it.ellipsize = TextUtils.TruncateAt.END
            it.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimensionPixelSize(R.dimen.description_text_size).toFloat()
            )
            it.setTextColor(secondaryTextColor)

            root.addView(it, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                it.topMargin = 8.dpToPx
                it.leftMargin = 8.dpToPx
                it.rightMargin = 8.dpToPx
            })
        }
        private val breadCrumbs = LinearLayout(context).also {
            it.orientation = LinearLayout.HORIZONTAL
            titleSection.addView(it, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).also {
            })
        }
        private val titleView = TextView(context).also {
            it.setSingleLine(true)
            it.maxLines = 1
            it.ellipsize = TextUtils.TruncateAt.END
            it.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimensionPixelSize(R.dimen.title_text_size).toFloat()
            )
            it.setTextColor(primaryTextColor)

            titleSection.addView(it, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }
        private val wrapperBackground = FrameLayout(context).also {
            it.setPadding(8.dpToPx, 8.dpToPx, 8.dpToPx, 8.dpToPx)

            root.addView(it, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        private val divider = View(context).also {
            it.setBackgroundColor(context.resources.getColor(R.color.exhibit_outline))

            root.addView(it, LinearLayout.LayoutParams(MATCH_PARENT, 0.5f.dpToPx))
        }
        private val wrapper = HighlightFrameLayout(context).also {
            wrapperBackground.addView(it, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }

        fun bind(exhibit: ExhibitWrapper) {
            val context = itemView.context

            this.exhibit = exhibit

            titleView.text = exhibit.title
            descriptionView.text = exhibit.description
            descriptionView.visibility = if (exhibit.description.isNotBlank()) View.VISIBLE else View.GONE

            breadCrumbs.removeAllViews()

            exhibit.pathComponents.forEach { text ->
                val componentColor = colors[text.hashCode().absoluteValue.rem(colors.size)]
                TextView(context).also {
                    it.text = text
                    it.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        context.resources.getDimensionPixelSize(R.dimen.title_text_size).toFloat()
                    )
                    it.setTextColor(componentColor)
                    breadCrumbs.addView(it)
                }

                TextView(context).also {
                    it.text = "›"
                    it.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        context.resources.getDimensionPixelSize(R.dimen.title_text_size).toFloat()
                    )
                    it.setPadding(4.dpToPx, 0, 4.dpToPx, 0)
                    it.setTextColor(componentColor)
                    breadCrumbs.addView(it)
                }
            }

            wrapper.removeAllViews()

            wrapper.clipToPadding = false
            wrapper.clipChildren = false

            (wrapper.parent as ViewGroup).clipToPadding = false
            (wrapper.parent as ViewGroup).clipChildren = false
            (wrapper.parent as ViewGroup).setBackgroundColor(exhibit.backgroundColor)

            try {
                val view = exhibit.method.invoke(null, wrapper) as View?

                if (view != null && view.parent == null) {
                    wrapper.addView(view)
                }
            } catch (ex: InvocationTargetException) {
                throw ex.targetException
            }
        }
    }

    companion object {
        private val skipPrefices = listOf(
            "android",
            "java",
            "com.google",
            "com.android",
            "org.intellij",
            "org.jetbrains",
            "kotlin",
            "com.github.bumptech"
        )
    }
}

class HighlightFrameLayout(context: Context) : FrameLayout(context) {

    private val borderPaint = Paint().also {
        it.color = context.resources.getColor(R.color.exhibit_outline)
        it.strokeWidth = 1f.dpToPx.toFloat()
        it.pathEffect = DashPathEffect(floatArrayOf(1f.dpToPx.toFloat(), 2f.dpToPx.toFloat()), 0f)
        it.style = Paint.Style.STROKE
    }
//
//    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
//        val rect = Rect()
//
//        child.getDrawingRect(rect)
//        rect.inset(0.75f.dpToPx, 0.75f.dpToPx)
//
//        canvas.drawRect(rect, borderPaint)
//
//        return super.drawChild(canvas, child, drawingTime)
//    }
}

open class MainActivity : AppCompatActivity() {

    open protected val backgroundColor: Int = 0xffffffff.toInt()
    open protected val primaryTextColor: Int = 0xff000000.toInt()
    open protected val secondaryTextColor: Int = 0xffa0a0a0.toInt()

    open fun createAdapter(): ExhibitRecyclerViewAdapter {
        return ExhibitRecyclerViewAdapter(applicationContext, backgroundColor, primaryTextColor, secondaryTextColor)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureDensity()

        val root = LinearLayout(this).also {
            it.orientation = LinearLayout.VERTICAL
        }
        val exhibitAdapter = createAdapter()
        val recyclerView = RecyclerView(this)

        val searchTextWrapper = FrameLayout(this).also {
            it.setPadding(8.dpToPx, 8.dpToPx, 8.dpToPx, 8.dpToPx)
            it.setBackgroundColor(backgroundColor)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                it.elevation = 10f
                it.translationZ = 10f
            }
        }
        val searchText = EditText(this).also {
            it.background = null
            it.hint = "Search by title or tags"
            it.setTextColor(primaryTextColor)
            it.setHintTextColor(secondaryTextColor)
            it.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(text: Editable?) {
                    exhibitAdapter.updateFilter(text?.toString() ?: "")
                }

                override fun beforeTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

            })
            searchTextWrapper.addView(it)
        }

        recyclerView.adapter = exhibitAdapter
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        root.addView(searchTextWrapper, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(recyclerView, LinearLayout.LayoutParams(MATCH_PARENT, 0).also {
            it.weight = 1f
        })

        setContentView(root, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
    }
}
