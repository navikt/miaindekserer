package no.nav.fo.miaindekserer.config

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import no.nav.fo.miaindekserer.helpers.getProp
import java.util.*

fun getSftpChanal(): Session {
    val j = JSch()
    val session = j.getSession("http://a30drvl002.oera.no/")
    session.setConfig("PreferredAuthentications", "publickey")
    j.addIdentity(getProp("SSH_KEY"))

    val config = java.util.Properties()
    config["StrictHostKeyChecking"] = "no"

    session.setConfig(config)

    return session
}

inline fun <T> sftp(session: Session, func: (ChannelSftp) -> T): T {
    session.connect()

    val sftp = session.openChannel("sftp") as ChannelSftp
    sftp.connect()

    val value = func(sftp)
    sftp.exit()

    return value
}
