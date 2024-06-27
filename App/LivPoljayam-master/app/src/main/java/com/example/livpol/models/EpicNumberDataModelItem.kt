package com.example.livpol.models

data class EpicNumberDataModelItem(
    val content: Content,
    val explanation: Any,
    val highlightFields: HighlightFields,
    val id: String,
    val index: String,
    val innerHits: InnerHits,
    val matchedQueries: List<Any>,
    val nestedMetaData: Any,
    val routing: Any,
    val score: Double,
    val sortValues: List<Any>
)