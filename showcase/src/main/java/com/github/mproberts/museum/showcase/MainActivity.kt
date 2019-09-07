package com.github.mproberts.museum.showcase

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.mproberts.museum.annotations.Exhibit
import dalvik.system.DexFile
import java.io.IOException
import java.lang.reflect.Method
import kotlin.concurrent.thread

@Exhibit("My Awesome View", "Views/Text", ["Text"])
fun myAwesomeView(parent: ViewGroup) = TextView(parent.context)
    .also {
        parent.addView(it)
    }

data class ExhibitWrapper(
    val title: String,
    val pathComponents: List<String>,
    val tags: List<String>,
    val method: Method
) {
    val sortKey = pathComponents.joinToString("/").toLowerCase() + title.toLowerCase()
}

class ExhibitRecyclerViewAdapter(private val context: Context): RecyclerView.Adapter<ExhibitRecyclerViewAdapter.ViewHolder>() {

    private var exhibits = emptyList<ExhibitWrapper>()
    private var filter = ""

    private fun enumerateExhibitMethods(): List<Method> = try {
        val discoveredClasses = mutableSetOf<Class<*>>()
        val classLoader = context.classLoader
        val df = DexFile(context.packageCodePath)

        for (entry in df.entries().asSequence()) {
            val shouldInclude = skipPrefices.find { prefix -> entry.startsWith(prefix) } == null
            val loadedClass = if (shouldInclude) {
                try {
                    classLoader.loadClass(entry)
                } catch (ex: Throwable) {
                    null
                }
            } else {
                null
            } ?: continue

            val searchList = mutableListOf(loadedClass)

            while (searchList.isNotEmpty()) {
                val classLookup = searchList.removeAt(0)

                if (!discoveredClasses.contains(classLookup)) {
                    discoveredClasses.add(classLookup)

                    searchList.addAll(classLookup.classes)
                }
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
                    method
                )
            })
        }
    }

    fun updateExhibits(updatedExhibits: List<ExhibitWrapper>) {
        exhibits = updatedExhibits.sortedBy { it.sortKey }
        notifyDataSetChanged()
    }

    fun updateFilter(filterText: String) {
        filter = filterText
        notifyDataSetChanged()
    }

    private fun selectedExhibits() = if (filter.isEmpty()) {
        exhibits
    } else {
        exhibits
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent.context)

    override fun getItemCount() = selectedExhibits().size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(selectedExhibits().get(position))
    }

    inner class ViewHolder(context: Context) : RecyclerView.ViewHolder(FrameLayout(context)) {
        val dp10 = 15
        val dp40 = 60
        val dp80 = 120

        private lateinit var exhibit: ExhibitWrapper
        private val root = itemView as ViewGroup
        private val titleView = TextView(context).also {
            it.setSingleLine(true)
            it.maxLines = 1
            it.ellipsize = TextUtils.TruncateAt.END
            root.addView(it, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                it.topMargin = dp10
            })
        }
        private val breadCrumbs = LinearLayout(context).also {
            it.orientation = LinearLayout.HORIZONTAL
            root.addView(it, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                it.topMargin = dp40
            })
        }
        private val wrapper = FrameLayout(context).also {
            root.addView(it, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
                it.topMargin = dp80
            })
        }

        fun bind(exhibit: ExhibitWrapper) {
            val context = itemView.context

            this.exhibit = exhibit

            titleView.text = exhibit.title

            breadCrumbs.removeAllViews()

            exhibit.pathComponents.forEach { text ->
                if (breadCrumbs.childCount != 0) {
                    TextView(context).also {
                        it.text = "/"
                        breadCrumbs.addView(it)
                    }
                }

                TextView(context).also {
                    it.text = text
                    breadCrumbs.addView(it)
                }
            }

            wrapper.removeAllViews()

            exhibit.method.invoke(null, wrapper)
        }
    }

    companion object {
        private val skipPrefices = listOf(
            "android",
            "java",
            "com.google",
            "org.intellij",
            "kotlin",
            "com.github.bumptech"
        )
    }
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val adapter = ExhibitRecyclerViewAdapter(this)
    }
}
