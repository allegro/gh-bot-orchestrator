package pl.allegro.tech.common.pullrequestmanager

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode

class KotestProjectConfig : AbstractProjectConfig() {

    override val isolationMode = IsolationMode.InstancePerLeaf
}
