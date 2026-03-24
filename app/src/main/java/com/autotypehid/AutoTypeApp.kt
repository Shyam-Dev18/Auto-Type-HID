package com.autotypehid

import android.app.Application
import com.autotypehid.core.managers.AppContainer

class AutoTypeApp : Application() {
	override fun onCreate() {
		super.onCreate()
		AppContainer.initialize(this)
	}
}
