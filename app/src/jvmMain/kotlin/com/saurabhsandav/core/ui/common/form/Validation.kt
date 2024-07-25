package com.saurabhsandav.core.ui.common.form

fun interface Validation<T> {

    context(ValidationScope)
    suspend fun (@ValidationDsl T).validate()
}

@Target(AnnotationTarget.TYPE)
@DslMarker
annotation class ValidationDsl
