{
    "name":"application",
    "composets":[
        {
            "name":"entry",
            "components":[
                {
                    "name":"entry-point",
                    "type":"test-entry",
                    "isolate":"isolate-2",
                    "properties":{
                        "nbIterations":"20"
                    },
                    "wires":{
                        "next":"obi-wan_Kenobi"
                    }
                },
                {
                    "name":"obi-wan_Kenobi",
                    "type":"fall-back",
                    "isolate":"isolate-1",
                    "wires":{
                        "next":"normal.get-cache",
                        "second":"fallback.get-cache"
                    }
                }
            ]
        },
        {
            "name":"fallback",
            "components":[
                {
                    "name":"get-cache",
                    "type":"get-cache",
                    "isolate":"isolate-1",
                    "properties":{
                        "channelName":"testCache",
                        "channelEntryName":"toto"
                    }
                }
            ]
        },
        {
            "name":"normal",
            "components":[
                {
                    "name":"get-cache",
                    "type":"get-cache-if-recent",
                    "isolate":"isolate-1",
                    "properties":{
                        "channelName":"testCache",
                        "channelEntryName":"toto",
                        "maxCacheAge":5000
                    },
                    "wires":{
                        "next":"store-to-cache"
                    }
                },
                {
                    "name":"store-to-cache",
                    "type":"store-cache",
                    "isolate":"isolate-1",
                    "properties":{
                        "channelName":"testCache",
                        "channelEntryName":"toto"
                    },
                    "wires":{
                        "next":"safe-erp-caller"
                    }
                }
            ]
        },
        {
            "name":"erp-caller",
            "components":[
                {
                    "name":"safe-erp-caller",
                    "type":"exception-catcher",
                    "isolate":"isolate-1",
                    "wires":{
                        "next":"test-end"
                    }
                },
                {
                    "name":"test-end",
                    "type":"test-end",
                    "isolate":"isolate-2"
                }
            ]
        }
    ]
}