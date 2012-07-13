{
    "appId":"demo-app",
    "multicast":"239.0.0.1",
    "isolates":[
        {
            "id":"org.psem2m.internals.isolates.forker.stratus",
            "kind":"pelix",
            "httpPort":9001,
            "node":"stratus",
            "bundles":[
                {
                    "symbolicName":"base.httpsvc"
                },
                {
                    "symbolicName":"base.signals.directory"
                },
                {
                    "symbolicName":"base.signals.directory_updater"
                },
                {
                    "symbolicName":"base.signals.http"
                },
                {
                    "symbolicName":"psem2m.forker.config_broker"
                },
                {
                    "symbolicName":"psem2m.forker.core"
                },
                {
                    "symbolicName":"psem2m.runner.python"
                },
                {
                    "symbolicName":"psem2m.runner.java"
                },
                {
                    "symbolicName":"psem2m.forker.heartbeat"
                }
            ]
        }
    ]
}