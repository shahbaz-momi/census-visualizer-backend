package com.beanz.censusviz.records

data class QueryDTO(
    val dataset: String,
    val params: List<Int>,
    val age: List<Int>,
    val sex: Int
)