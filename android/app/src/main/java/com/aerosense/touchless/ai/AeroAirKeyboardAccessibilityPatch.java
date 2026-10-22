package com.aerosense.touchless.ai;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

/**
 * Copy-Paste these methods into your AeroAccessibilityService.java
 * and connect them in MainActivity.java's AeroNativeBridge!
 */
public class AeroAirKeyboardAccessibilityPatch {

    /**
     * Injects text typed via 1-Hand or 2-Hand Air Keyboard directly into the currently
     * focused input field (Instagram Comment box, YouTube Search bar, WhatsApp, etc.).
     */
    public static boolean injectTextIntoFocusedAppInput(
            AccessibilityService service,
            String typedText,
            boolean submitAfterTyping
    ) {
        if (service == null) return false;
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root == null) return false;

        AccessibilityNodeInfo targetNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (targetNode == null) {
            targetNode = findFirstEditableNode(root);
        }

        if (targetNode != null) {
            Bundle arguments = new Bundle();
            arguments.putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    typedText
            );
            boolean setOk = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);

            if (submitAfterTyping) {
                // Press Enter / Search / Send action on the input node if supported
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    targetNode.performAction(
                            AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId()
                    );
                }
                // Also look for a nearby "Post", "Send", or "Search" button and click it
                clickSendOrSearchButton(root);
            }
            targetNode.recycle();
            root.recycle();
            return setOk;
        }

        root.recycle();
        return false;
    }

    private static AccessibilityNodeInfo findFirstEditableNode(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isEditable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo found = findFirstEditableNode(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void clickSendOrSearchButton(AccessibilityNodeInfo root) {
        String[] labels = new String[]{"Post", "Send", "Search", "Comment"};
        for (String label : labels) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(label);
            if (nodes != null) {
                for (AccessibilityNodeInfo node : nodes) {
                    if (node.isClickable()) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        return;
                    }
                }
            }
        }
    }
}
