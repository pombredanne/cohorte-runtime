{
    "id":"org.psem2m.internals.isolates.monitor-isolate-one",
    "kind":"felix",
    "node":"central",
    "httpPort":9100,
    "vmArgs":[
        "-Xms32M",
        "-Xmx128M"
    ],
    "bundles":[
        {
            "symbolicName":"org.psem2m.isolates.ui.admin",
            "optional":true,
            "properties":{
                "psem2m.demo.ui.viewer.top":"0scr",
                "psem2m.demo.ui.viewer.left":"0scr",
                "psem2m.demo.ui.viewer.width":"0.5scr",
                "psem2m.demo.ui.viewer.height":"1scr",
                "psem2m.demo.ui.viewer.color":"SkyBlue"
            }
        },
        {
            "symbolicName":"org.psem2m.composer.ui"
        },
        {
            "symbolicName":"org.apache.felix.shell"
        },
        {
            "symbolicName":"org.apache.felix.shell.remote",
            "properties":{
                "osgi.shell.telnet.port":"6000"
            }
        },
        {
            "from":"signals-http.js"
        },
        {
            "from":"jsonrpc.js"
        },
        {
            "symbolicName":"org.psem2m.forker.api"
        },
        {
            "symbolicName":"org.psem2m.forkers.aggregator"
        },
        {
            "from":"remote-services.js",
            "overriddenProperties":{
                    "org.psem2m.remote.filters.exclude":"*.demo.*"
            }
        },
        {
            "symbolicName":"org.psem2m.isolates.master.manager",
            "optional":true
        },
        {
            "symbolicName":"org.psem2m.isolates.monitor"
        },
        {
            "symbolicName":"org.psem2m.composer.api"
        },
        {
            "symbolicName":"org.psem2m.libs.xerces"
        },
        {
            "symbolicName":"org.psem2m.sca.converter",
            "optional":true
        },
        {
            "symbolicName":"org.psem2m.composer.config"
        },
        {
            "symbolicName":"org.psem2m.composer.core"
        },
        {
            "from":"bundles-demo-compo.js"
        }
    ]
}