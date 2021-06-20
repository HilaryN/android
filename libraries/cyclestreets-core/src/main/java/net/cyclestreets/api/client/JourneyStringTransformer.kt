package net.cyclestreets.api.client

import com.bazaarvoice.jolt.Chainr
import com.bazaarvoice.jolt.JsonUtils

import fr.arnaudguyon.xmltojsonlib.XmlToJson

fun fromV1ApiXml(inputXml: String): String {
    val xmlToJson = XmlToJson.Builder(inputXml).build()
    val intermediateJson = xmlToJson.toString()
    return joltTransform(intermediateJson, V1API_XML_JOLT_SPEC)
}

fun fromV1ApiJson(inputJson: String): String {
    return joltTransform(inputJson, V1API_JSON_JOLT_SPEC)
}

// Uses https://github.com/bazaarvoice/jolt to transform JSON strings, without
// the need for us to define intermediate object representations.
private fun joltTransform(inputJson: String, specJson: String): String {
    val inputJsonObject = JsonUtils.jsonToObject(inputJson)

    val chainrSpecJson = JsonUtils.jsonToList(specJson)
    val chainr = Chainr.fromSpec(chainrSpecJson)

    val transformedOutput = chainr.transform(inputJsonObject)
    return JsonUtils.toJsonString(transformedOutput)
}

private const val V1API_XML_JOLT_SPEC = """[{
  "operation": "shift",
  "spec": {
    "markers": {
      "waypoint": { "@": "waypoints" },
      "marker": {
        "*": {
          "type": {
            "route": { "@(2)": "route" },
            "segment": { "@(2)": "segments[]" }
          }
        }
      }
    }
  }
}]
"""
// For A-B route, input json will have array of waypoints.
// For circular route, input json has a single waypoint.
// It may also have a single poi or an array of pois.
// Convert these to arrays, do the transformation, then convert back to arrays if single
private const val V1API_JSON_JOLT_SPEC = """
[// First, convert single waypoint / poi to array
{
    "operation": "cardinality",
    "spec": {
    "waypoint": "MANY"
    "poi": "MANY"
    }
},
// Now do the transformation, but note that 1-element arrays will get converted back to strings...
{
  "operation": "shift",
  "spec": {
    
    "waypoint": {
      "*": { "\\@attributes": "waypoints" }},
    
    "marker": {
      "*": {
        "\\@attributes": {
          "type": {
            "route": { "@(2)": "route" },
            "segment": { "@(2)": "segments[]" }
          }
        }
      }
    },
    "poi": {
      "*": { "\\@attributes": "pois" }
    },
   
    "error": "Error"
  }
},
// ... so we need to convert the strings back to arrays!
// (N.B. could use cardinality again here, though it puts waypoints and pois at the end)
{
    "operation": "shift",
    "spec": {
        "waypoints": "waypoints[#1]",   // converts to array if not already an array
        "pois": "pois[#1]",             // ditto
        "*": "&"    // leaves everything else as it is
    }
}
]"""


// Nope, doesn't work - it literally puts "MANY" in output
private const val V1API_JSON_JOLT_SPEC6 = """
[
{
  "operation": "shift",
  "spec": {
    
    "waypoint": {
      "@" : "MANY",  // make waypoint an array
      "*": { "\\@attributes": "waypoints" }},
    
    "marker": {
      "*": {
        "\\@attributes": {
          "type": {
            "route": { "@(2)": "route" },
            "segment": { "@(2)": "segments[]" }
          }
        }
      }
    },
    "poi": {
      "*": { "\\@attributes": "pois" }
    },
   
    "error": "Error"
  }
}]"""


// Can I chain 2 shift's? Yes I probably can, but I would have to repeat all the rest of the spec.

// Output:
// "waypoints":[{"longitude":"0.18871","latitude":"51.29565","sequenceId":"1"}] (at end of json)
private const val V1API_JSON_JOLT_SPEC5 = """
[// First, convert single waypoint to array
{
    "operation": "cardinality",
    "spec": {
    "waypoint": "MANY"
    }
},
// Now do the transformation, but note that 1-element array will get converted back to string...
{
  "operation": "shift",
  "spec": {
    
    "waypoint": {
      "*": { "\\@attributes": "waypoints" }},
    
    "marker": {
      "*": {
        "\\@attributes": {
          "type": {
            "route": { "@(2)": "route" },
            "segment": { "@(2)": "segments[]" }
          }
        }
      }
    },
    "poi": {
      "*": { "\\@attributes": "pois" }
    },
   
    "error": "Error"
  }
},
// ... so we need to convert the string back to an array!
{
    "operation": "cardinality",
    "spec": {
        "waypoints": "MANY"
    }
}
]"""


// Ouput for circ route (correct - it has placed single item in array):
// "waypoint":[{"@attributes":{"longitude":"0.17117","latitude":"51.30253","sequenceId":"1"}}]
// Output for A-B route (correct - it has left array as array):
// "waypoint":[{"@attributes":{"longitude":"0.17117","latitude":"51.30253","sequenceId":"1"}},{"@attributes":{"longitude":"0.14790","latitude":"51.35256","sequenceId":"2"}}]
private const val V1API_JSON_JOLT_SPEC3 = """
[
 {
    "operation": "cardinality",
    "spec": {
    "waypoint": "MANY"
    }
}]"""

// Output for A-B route (correct):
// {"waypoints":[{"longitude":"0.16592","latitude":"51.31914","sequenceId":"1"},{"longitude":"0.14786","latitude":"51.37289","sequenceId":"2"}],
// Output for circ route (why is there no array?!):
// {"waypoints":{"longitude":"0.17117","latitude":"51.30253","sequenceId":"1"},
// (Circ route doesn't display - must have errored due to incorrect output of waypoints
private const val V1API_JSON_JOLT_SPEC2 = """
[
 {
    "operation": "cardinality",
    "spec": {
    "waypoint": "MANY"
    }
},
{
  "operation": "shift",
  "spec": {
    
    "waypoint": {
      "*": { "\\@attributes": "waypoints" }},
    
    "marker": {
      "*": {
        "\\@attributes": {
          "type": {
            "route": { "@(2)": "route" },
            "segment": { "@(2)": "segments[]" }
          }
        }
      }
    },
    "poi": {
      "*": { "\\@attributes": "pois" }
    },
   
    "error": "Error"
  }
}]"""
// "Original" spec - Output has no waypoint for circ route / single waypoint (not array)
// How does it treat array of 1 element?  (Will need to hard-code that):
// "waypoint":[{"@attributes":{"longitude":"0.17117","latitude":"51.30253","sequenceId":"1"}}],
// Output (it removes the array and puts it back to string!):
// {"waypoints":{"longitude":"0.17117","latitude":"51.30253","sequenceId":"1"},
// So maybe I have to repeat the cardinality again after the shift... hmmm, better to do 2 shift specs?

private const val V1API_JSON_JOLT_SPEC4 = """
[
{
  "operation": "shift",
  "spec": {
    
    "waypoint": {
      "*": { "\\@attributes": "waypoints" }},
    
    "marker": {
      "*": {
        "\\@attributes": {
          "type": {
            "route": { "@(2)": "route" },
            "segment": { "@(2)": "segments[]" }
          }
        }
      }
    },
    "poi": {
      "*": { "\\@attributes": "pois" }
    },
   
    "error": "Error"
  }
}]"""

//
//  "poi": {"\\@attributes": "pois[#1]"},

// "waypoint": {"\\@attributes": "waypoints[#1]"},
/*
{
    "operation": "cardinality",
    "spec": {
    "waypoint": "MANY"
    }
}]"""
 */

/* {
    "operation": "cardinality",
    "spec": {
    "waypoint": "MANY"
    }
}, */