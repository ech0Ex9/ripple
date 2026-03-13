package org.ripple.endpoint.core.git

import org.ripple.endpoint.detector.api.model.ChangeType
import org.ripple.endpoint.detector.api.model.ChangedFile

data class BranchDiffResult(
    val sourceBranch: String,
    val targetBranch: String,
    val changes: List<ChangedFile>,
    val ahead: Int,
    val behind: Int
)

interface GitChangeResolver {
    
    fun resolveWorkingChanges(projectPath: String): List<ChangedFile>
    
    fun resolveStagedChanges(projectPath: String): List<ChangedFile>
    
    fun resolveBranchDiff(
        projectPath: String,
        sourceBranch: String,
        targetBranch: String
    ): BranchDiffResult
    
    fun getCurrentBranch(projectPath: String): String?
    
    fun getBranches(projectPath: String): List<String>
}