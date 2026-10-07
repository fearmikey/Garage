package com.fearmikey.garage.data.remote.lubelogger

import com.google.gson.JsonElement

/**
 * Flattens a JSON tree into ASP.NET MVC form-binding keys so a full object can be posted
 * to a `[FromForm]`-style endpoint, e.g.
 * `{"tags":["a"],"extraFields":[{"name":"x","value":"1"}]}` becomes
 * `tags[0]=a`, `extraFields[0].name=x`, `extraFields[0].value=1`.
 *
 * JSON nulls are skipped so that the server-side defaults apply.
 */
fun flattenJsonToFormFields(element: JsonElement, prefix: String = ""): Map<String, String> {
    val out = LinkedHashMap<String, String>()
    flattenInto(element, prefix, out)
    return out
}

private fun flattenInto(element: JsonElement, prefix: String, out: MutableMap<String, String>) {
    when {
        element.isJsonNull -> Unit
        element.isJsonPrimitive -> if (prefix.isNotEmpty()) out[prefix] = element.asString
        element.isJsonArray -> element.asJsonArray.forEachIndexed { index, child ->
            flattenInto(child, "$prefix[$index]", out)
        }
        element.isJsonObject -> element.asJsonObject.entrySet().forEach { (key, child) ->
            flattenInto(child, if (prefix.isEmpty()) key else "$prefix.$key", out)
        }
    }
}
