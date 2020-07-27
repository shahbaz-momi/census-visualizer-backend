package com.beanz.censusviz

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CensusVizApplication

fun main(args: Array<String>) {
	runApplication<CensusVizApplication>(*args)
}
