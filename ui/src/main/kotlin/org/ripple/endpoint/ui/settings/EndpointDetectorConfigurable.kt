package org.ripple.endpoint.ui.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class EndpointDetectorConfigurable(private val project: Project) : Configurable {
    
    private var panel: EndpointDetectorSettingsPanel? = null
    
    override fun getDisplayName(): String = "流量入口检测"
    
    override fun createComponent(): JComponent {
        panel = EndpointDetectorSettingsPanel(project)
        return panel!!.createPanel()
    }
    
    override fun isModified(): Boolean {
        return panel?.isModified() ?: false
    }
    
    override fun apply() {
        panel?.apply()
    }
    
    override fun reset() {
        panel?.reset()
    }
    
    override fun disposeUIResources() {
        panel = null
    }
}