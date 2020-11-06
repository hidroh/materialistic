package io.github.hidroh.materialistic

import android.content.Context
import java.net.InetSocketAddress
import java.net.Proxy

class DelegatingProxy : Proxy(Type.SOCKS, InetSocketAddress.createUnresolved("127.0.0.1", 1)) {
    override fun type() = activeProxy.type()
    override fun address() = activeProxy.address()

    companion object {
        var activeProxy: Proxy = NO_PROXY

        @JvmStatic
        fun updateFromPreferences(context: Context) {
            activeProxy = Preferences.getProxy(context)
        }
    }
}
