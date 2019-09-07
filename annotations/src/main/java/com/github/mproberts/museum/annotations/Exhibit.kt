package com.github.mproberts.museum.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Exhibit(val title: String, val path: String = "", val tags: Array<String> = [], val description: String = "")
