package com.aiassistant.data.repository

import com.aiassistant.domain.model.Action
import com.aiassistant.domain.model.UINode
import com.aiassistant.domain.repository.ScreenRepository
import com.aiassistant.data.mapper.ScreenParser
import com.aiassistant.framework.accessibility.AccessibilityServiceBridge
import com.aiassistant.framework.accessibility.ActionPerformer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
class ScreenRepositoryImpl @Inject constructor(
    private val screenParser: ScreenParser,
    private val serviceBridge: AccessibilityServiceBridge,
    private val actionPerformer: ActionPerformer
) : ScreenRepository {

    override suspend fun captureScreen(): List<UINode> {
        val rootNode = serviceBridge.getRootNode() ?: return emptyList()
        return screenParser.parse(rootNode)
    }

    override suspend fun performAction(action: Action): Boolean {
        return actionPerformer.execute(action, screenParser.nodeMap)
    }

    override fun isServiceConnected(): StateFlow<Boolean> {
        return serviceBridge.isConnected()
    }
}