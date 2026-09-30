package com.planetofoses989.kwideo.ui.browser.networkstreaming.clients

import com.planetofoses989.kwideo.domain.network.NetworkConnection
import com.planetofoses989.kwideo.domain.network.NetworkProtocol

object NetworkClientFactory {
  fun createClient(connection: NetworkConnection): NetworkClient =
    when (connection.protocol) {
      NetworkProtocol.SMB -> SmbClient(connection)
      NetworkProtocol.FTP -> FtpClient(connection)
      NetworkProtocol.WEBDAV -> WebDavClient(connection)
    }
}
