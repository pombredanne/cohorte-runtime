{
    "id":"isolate-raspberrypi-temper",
    "kind":"pelix",
    "node":"raspberrypi",
    "httpPort":9300,
    "bundles":[
		{
		    "symbolicName":"pelix.shell.core"
		},
		{
		    "symbolicName":"pelix.shell.remote"
		},
		{
		    "symbolicName":"pelix.shell.ipopo"
		},
		{
			"symbolicName":"base.psem2m_shell"
		},
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
            "symbolicName":"base.remoteservices"
        },
        {
            "symbolicName":"base.composer"
        },
        {
            "symbolicName":"demo.temperature"
        }
    ],
    "environment":{
        "PYTHONPATH":"/Users/ogattaz/workspaces/PSEM2M_SDK_2012/_REPO_git/demos/demo-july2012/demo.july2012.python",
        "shell.port":"4202"
    }
}