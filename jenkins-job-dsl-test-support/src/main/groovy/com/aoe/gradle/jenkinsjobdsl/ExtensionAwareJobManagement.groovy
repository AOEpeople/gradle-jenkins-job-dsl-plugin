package com.aoe.gradle.jenkinsjobdsl

import javaposse.jobdsl.dsl.Item
import javaposse.jobdsl.dsl.MemoryJobManagement
import javaposse.jobdsl.dsl.helpers.ExtensibleContext

/**
 * Extend MemoryJobManagement with the ability to find ExtensionPoints
 */
class ExtensionAwareJobManagement extends MemoryJobManagement {

    ExtensionSupport extensionSupport = new ExtensionSupport()

    public Node callExtension(String name,
                              Item item,
                              Class<? extends ExtensibleContext> contextType,
                              Object... args) {
        extensionSupport.callExtension(name, item, contextType, args)
    }
}
