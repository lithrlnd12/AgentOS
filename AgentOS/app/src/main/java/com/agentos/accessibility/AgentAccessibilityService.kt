package com.agentos.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Core AccessibilityService for AgentOS.
 * Handles screen reading and action execution.
 */
class AgentAccessibilityService : AccessibilityService() {

    companion object {
        private val _instance = MutableStateFlow<AgentAccessibilityService?>(null)
        val instance: StateFlow<AgentAccessibilityService?> = _instance.asStateFlow()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _instance.value = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events
        event?.let {
            // Process events as needed for context awareness
        }
    }

    override fun onInterrupt() {
        // Called when the system wants to interrupt feedback
    }

    override fun onDestroy() {
        super.onDestroy()
        _instance.value = null
    }

    // ==================== Screen Reading ====================

    /**
     * Get the root node of the current active window.
     */
    fun getRootNode(): AccessibilityNodeInfo? = rootInActiveWindow

    /**
     * Find nodes by resource ID.
     * @param viewId Fully qualified resource ID (e.g., "com.example.app:id/button")
     */
    fun findNodesById(viewId: String): List<AccessibilityNodeInfo> {
        val root = rootInActiveWindow ?: return emptyList()
        return root.findAccessibilityNodeInfosByViewId(viewId)
    }

    /**
     * Find nodes by text content.
     * @param text Text to search for (case-insensitive containment match)
     */
    fun findNodesByText(text: String): List<AccessibilityNodeInfo> {
        val root = rootInActiveWindow ?: return emptyList()
        return root.findAccessibilityNodeInfosByText(text)
    }

    // ==================== Action Execution ====================

    /**
     * Perform a tap gesture at the specified coordinates.
     */
    fun tap(x: Float, y: Float, callback: GestureResultCallback? = null) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, callback, null)
    }

    /**
     * Perform a swipe gesture.
     */
    fun swipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long = 300,
        callback: GestureResultCallback? = null
    ) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        dispatchGesture(gesture, callback, null)
    }

    /**
     * Perform a long press at the specified coordinates.
     */
    fun longPress(x: Float, y: Float, duration: Long = 1000, callback: GestureResultCallback? = null) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        dispatchGesture(gesture, callback, null)
    }

    /**
     * Click on a specific node.
     */
    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        var currentNode: AccessibilityNodeInfo? = node
        while (currentNode != null) {
            if (currentNode.isClickable) {
                return currentNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            currentNode = currentNode.parent
        }
        return false
    }

    /**
     * Set text in an editable field.
     */
    fun setNodeText(node: AccessibilityNodeInfo, text: String): Boolean {
        val arguments = android.os.Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    // ==================== Global Actions ====================

    fun pressBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun pressHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun pressRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun openNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    fun openQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
}
