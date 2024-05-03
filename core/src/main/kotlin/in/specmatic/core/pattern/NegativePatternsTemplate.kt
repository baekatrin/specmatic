package `in`.specmatic.core.pattern

import `in`.specmatic.core.Resolver

abstract class NegativePatternsTemplate {
    fun negativeBasedOn(
        patternMap: Map<String, Pattern>,
        row: Row,
        resolver: Resolver
    ): Sequence<ReturnValue<Map<String, Pattern>>> {
        val eachKeyMappedToPatternMap: Map<String, Map<String, Pattern>> = patternMap.mapValues { patternMap }
        val negativePatternsForEachKey: Map<String, Sequence<ReturnValue<Pattern>>> = getNegativePatterns(patternMap, resolver, row)

        val modifiedPatternMap: Map<String, Map<String, Sequence<ReturnValue<Pattern>>>?> =
            eachKeyMappedToPatternMap.mapValues { (keyToNegate, patterns) ->
                val negativePatterns: Sequence<ReturnValue<Pattern>> = negativePatternsForEachKey.getValue(keyToNegate)

                if(!negativePatterns.any())
                    return@mapValues null

                patterns.mapValues { (key, pattern) ->
                    when (key) {
                        keyToNegate -> {
                            val result: Sequence<ReturnValue<Pattern>> = negativePatterns
                            result
                        }
                        else -> newBasedOn(row, key, pattern, resolver).map { HasValue(it) }
                    }.map {
                        it.breadCrumb(withoutOptionality(key))
                    }
                }
            }

        if (modifiedPatternMap.values.isEmpty())
            return sequenceOf(HasValue(emptyMap()))

        return modifiedPatternMap.values.asSequence().filterNotNull().flatMap {
            patternListR(it)
        }

    }

    abstract fun getNegativePatterns(
        patternMap: Map<String, Pattern>,
        resolver: Resolver,
        row: Row
    ): Map<String, Sequence<ReturnValue<Pattern>>>

    abstract fun negativePatternsForKey(
        key: String,
        negativePattern: Pattern,
        resolver: Resolver,
    ): Sequence<Pattern>
}
