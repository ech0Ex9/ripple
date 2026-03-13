package org.ripple.endpoint.ui.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import javax.swing.JComponent

class EndpointDetectorConfigurable : SearchableConfigurable {
    
    private var panel: EndpointDetectorSettingsPanel? = null
    
    override fun getId(): String = "org.ripple.endpoint.EndpointDetectorConfigurable"
    
    override fun getDisplayName(): String = "Ripple"
    
    override fun createComponent(): JComponent {
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        panel = EndpointDetectorSettingsPanel(project)
        return panel!!.createPanel()
    }
    
    override fun isModified(): Boolean = panel?.isModified() ?: false
    
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