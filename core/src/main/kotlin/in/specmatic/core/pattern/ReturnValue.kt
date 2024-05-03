package `in`.specmatic.core.pattern

import `in`.specmatic.core.Result

sealed interface ReturnValue<T> {
    val value: T

    abstract fun <U> withDefault(default: U, fn: (T) -> U): U
    abstract fun <U> ifValue(fn: (T) -> U): ReturnValue<U>
    fun update(fn: (T) -> T): ReturnValue<T>
    fun <U> combineWith(valueResult: ReturnValue<U>, fn: (T, U) -> T): ReturnValue<T>
    fun <U> realise(hasValue: (T, String?) -> U, orFailure: (HasFailure<T>) -> U, orException: (HasException<T>) -> U): U
    fun addDetails(message: String, breadCrumb: String): ReturnValue<T>
}

fun <ReturnType> returnValue(errorMessage: String = "", breadCrumb: String = "", f: ()->Sequence<ReturnValue<ReturnType>>): Sequence<ReturnValue<ReturnType>> {
    return try {
        f().map { it.addDetails(errorMessage, breadCrumb) }
    }
    catch(contractException: ContractException) {
        val failure =
            Result.Failure(message = errorMessage, breadCrumb = breadCrumb, cause = contractException.failure())
        sequenceOf(HasFailure(failure))
    }
    catch(throwable: Throwable) {
        sequenceOf(HasException(throwable, errorMessage, breadCrumb))
    }
}

fun <K, ValueType> Map<K, ReturnValue<ValueType>>.mapFold(): ReturnValue<Map<K, ValueType>> {
    val initial: ReturnValue<Map<K, ValueType>> = HasValue<Map<K, ValueType>>(emptyMap())

    return this.entries.fold(initial) { accR: ReturnValue<Map<K, ValueType>>, (key: K, valueR: ReturnValue<ValueType>) ->
        accR.combineWith(valueR) { acc, value ->
            acc.plus(key to value)
        }
    }
}

fun <T> Sequence<List<ReturnValue<out T>>>.sequenceListFold(): Sequence<ReturnValue<List<T>>> {
    val data: Sequence<List<ReturnValue<out T>>> = this

    return data.map { listOfReturnValues ->
        val error: ReturnValue<out T>? = listOfReturnValues.firstOrNull { it !is HasValue }

        if (error != null)
            listOf(error)

        val valueDetails: List<ValueDetails> = listOfReturnValues.map { (it as HasValue).valueDetails }.flatten()
        HasValue(listOfReturnValues.map { it.value }, valueDetails)
    }
}

fun <T> ReturnValue<T>.breadCrumb(breadCrumb: String?): ReturnValue<T> {
    if(breadCrumb == null)
        return this

    return this.addDetails("", breadCrumb)
}

fun returnValueSequence(fn: () -> Sequence<ReturnValue<Pattern>>): Sequence<ReturnValue<Pattern>> {
    return try {
        fn()
    } catch(t: Throwable) {
        return sequenceOf(HasException(t, ""))
    }
}

fun <T> Sequence<ReturnValue<T>>.filterValueIsNot(fn: (T) -> Boolean): Sequence<ReturnValue<T>> {
    return this.filterNot { returnValue ->
        returnValue.withDefault(false) { value ->
            fn(value)
        }
    }
}