package com.aoe.gradle.jenkinsjobdsl

import javaposse.jobdsl.dsl.MemoryJobManagement

/**
 * Extend MemoryJobManagement with the ability to find ExtensionPoints
 */
@Mixin(WithExtensionAwareness)
class ExtensionAwareJobManagement extends MemoryJobManagement { }
