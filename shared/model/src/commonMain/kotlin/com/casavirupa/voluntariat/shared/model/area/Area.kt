package com.casavirupa.voluntariat.shared.model.area

import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlin.jvm.JvmInline

data class Area(
    val id: AreaId,
    val name: String,
    val responsibleId: UserId,
)

@JvmInline
value class AreaId(val value: String)
