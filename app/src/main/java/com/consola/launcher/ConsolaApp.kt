package com.consola.launcher

import android.app.Application

class ConsolaApp : Application() {
    val graph: LauncherGraph by lazy { LauncherGraph(this) }
}
