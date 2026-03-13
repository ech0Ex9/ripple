package org.ripple.endpoint.ui.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import org.ripple.endpoint.detector.api.model.CustomDetectorRule
import org.ripple.endpoint.detector.api.model.DetectorRuleState

@State(
    name = "RippleDetectorRuleConfig",
    storages = [Storage("ripple-detector-rules.xml")]
)
class DetectorRuleConfig : PersistentStateComponent<DetectorRuleState> {
    
    private var internalState = DetectorRuleState()
    
    override fun getState(): DetectorRuleState = internalState
    
    override fun loadState(state: DetectorRuleState) {
        XmlSerializerUtil.copyBean(state, internalState)
    }
    
    val rules: List<CustomDetectorRule>
        get() = internalState.rules.ifEmpty { DetectorRuleState.DEFAULT_RULES }
    
    fun addRule(rule: CustomDetectorRule) {
        val current = internalState.rules.toMutableList()
        current.removeAll { it.id == rule.id }
        current.add(rule)
        internalState.rules = current
    }
    
    fun removeRule(ruleId: String) {
        internalState.rules = internalState.rules.filter { it.id != ruleId }
    }
    
    fun updateRule(rule: CustomDetectorRule) {
        val current = internalState.rules.toMutableList()
        val index = current.indexOfFirst { it.id == rule.id }
        if (index >= 0) {
            current[index] = rule
            internalState.rules = current
        }
    }
    
    fun setRules(rules: List<CustomDetectorRule>) {
        internalState.rules = rules
    }
    
    fun getRule(id: String): CustomDetectorRule? = rules.find { it.id == id }
    
    fun resetToDefaults() {
        internalState.rules = DetectorRuleState.DEFAULT_RULES
    }
    
    companion object {
        fun getInstance(): DetectorRuleConfig =
            ApplicationManager.getApplication().getService(DetectorRuleConfig::class.java)
    }
}