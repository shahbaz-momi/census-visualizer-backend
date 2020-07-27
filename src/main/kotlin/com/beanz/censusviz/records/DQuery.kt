package com.beanz.censusviz.records

data class DQuery(
    val dataset: String,
    val params: List<Int>,
    val age: List<Int>,
    val sex: Int
)