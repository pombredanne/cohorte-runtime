{
    "name":"CartsApplier",
    "components":[
        {
            "name":"useCartQueue",
            "type":"cache-queue-handler",
            "isolate":"isolate-cache",
            "properties":{
                "cacheChannel":"carts",
                "cartIdKey":"id",
                "cartLinesKey":"lines",
                "timeout":3000
            },
            "wires":{
                "next":"safeErpCaller"
            }
        },
        {
            "name":"safeErpCaller",
            "type":"exception-catcher",
            "wires":{
                "next":"erpCaller"
            }
        },
        {
            "name":"erpCaller",
            "type":"erp-caller",
            "properties":{
                "method":"applyCart"
            },
            "wires":{
                "next":"erpProxy"
            }
        }
    ]
}