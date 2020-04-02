package run.qontract.core.value

import run.qontract.core.utilities.valueMapToPrettyJsonString

data class JSONObjectValue(val jsonObject: Map<String, Value>) : Value {
    override val httpContentType = "application/json"
    override val value: Any = jsonObject
    override fun toString() = valueMapToPrettyJsonString(jsonObject)
}
