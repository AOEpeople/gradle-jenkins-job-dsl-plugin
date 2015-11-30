package com.aoe.gradle.jenkinsjobdsl

import hudson.Extension
import hudson.model.Items
import javaposse.jobdsl.dsl.DslException
import javaposse.jobdsl.dsl.Item
import javaposse.jobdsl.dsl.helpers.ExtensibleContext
import javaposse.jobdsl.plugin.ContextExtensionPoint
import javaposse.jobdsl.plugin.DslEnvironment
import javaposse.jobdsl.plugin.DslEnvironmentImpl
import javaposse.jobdsl.plugin.DslExtensionMethod
import javaposse.jobdsl.plugin.Messages
import net.java.sezpoz.Index
import org.apache.commons.lang.ClassUtils

import java.lang.reflect.AnnotatedElement
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.logging.Logger

import static org.apache.commons.lang.ClassUtils.convertClassesToClassNames
import static org.apache.commons.lang.StringUtils.join

/**
 * This trait can be mixed into any JobManagement class to allow the usage of
 * extensions.
 *
 * @author Carsten Lenz, AOE
 */
trait WithExtensionAwareness {
    private static final Logger LOGGER = Logger.getLogger(WithExtensionAwareness.class.getName());

    private final Map<Item, DslEnvironment> environments =
            new HashMap<Item, DslEnvironment>();

    private DslEnvironment getSession(Item item) {
        DslEnvironment session = environments.get(item);
        if (session == null) {
            session = new DslEnvironmentImpl();
            environments.put(item, session);
        }
        return session;
    }

    public Node callExtension(String name, Item item,
                              Class<? extends ExtensibleContext> contextType, Object... args) {
        Set<Helper.ExtensionPointMethod> candidates = Helper.findExtensionPoints(name, contextType, args);
        if (candidates.isEmpty()) {
            LOGGER.fine(
                    "Found no extension which provides method " + name + " with arguments " + Arrays.toString(args)
            );
            return null;
        } else if (candidates.size() > 1) {
            throw new DslException(String.format(
                    Messages.CallExtension_MultipleCandidates(),
                    name,
                    Arrays.toString(args),
                    Arrays.toString(candidates.toArray())
            ));
        }

        try {
            assert candidates.size() == 1
            Object result = candidates.iterator().next().call(getSession(item), args);
            return new XmlParser().parseText(Items.XSTREAM2.toXML(result));
        } catch (Exception e) {
            throw new RuntimeException("Error calling extension", e);
        }
    }
}

class Helper {

    /**
     * All DSL extension points (that means all classes with @Extension annotation and that are
     * subclasses of ContextExtensionPoint) get autodetected at runtime.
     */
    static @Lazy
    List<ContextExtensionPoint> DSL_EXTENSION_POINTS = {
        println "Autodetecting DSL extension points..."

        Index<Extension, ContextExtensionPoint> index = Index.load(Extension, ContextExtensionPoint)

        List<ContextExtensionPoint> result = []
        for (def indexItem : index) {
            try {
                AnnotatedElement element = indexItem.element()
                if (element instanceof Class<?>
                        && ContextExtensionPoint.isAssignableFrom(element as Class<?>)) {
                    result << element.newInstance().asType(ContextExtensionPoint)
                }
            }
            catch (ignored) {
                println "Extension Point ${indexItem.className()} could not load - if needed check dependencies"
            }
        }

        println "Finished autodetecting DSL extension points."
        println "Found the following DSL extensions:"
        println result.collect { "  ${it.getClass().name}" }.join("\n")

        Collections.unmodifiableList(result)
    }()

    static Set<ExtensionPointMethod> findExtensionPoints(String name, Class<? extends ExtensibleContext> contextType,
                                                         Object... args) {
        Class[] parameterTypes = ClassUtils.toClass(args);
        Set<ExtensionPointMethod> candidates = new HashSet<ExtensionPointMethod>();

        // Find extensions that match any @DslExtensionMethod annotated method with the given name and parameters
        for (ExtensionPointMethod candidate : findCandidateMethods(name, contextType)) {
            if (ClassUtils.isAssignable(parameterTypes, candidate.getFilteredParameterTypes(), true)) {
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    private
    static List<ExtensionPointMethod> findCandidateMethods(String name, Class<? extends ExtensibleContext> contextType) {
        List<ExtensionPointMethod> result = new ArrayList<ExtensionPointMethod>();
        for (ContextExtensionPoint extensionPoint : DSL_EXTENSION_POINTS) {
            for (Method method : extensionPoint.getClass().getMethods()) {
                if (method.getName().equals(name)) {
                    DslExtensionMethod annotation = method.getAnnotation(DslExtensionMethod.class);
                    if (annotation != null && annotation.context().isAssignableFrom(contextType)) {
                        result.add(new ExtensionPointMethod(extensionPoint, method));
                    }
                }
            }
        }
        return result;
    }

    static class ExtensionPointMethod {
        private final ContextExtensionPoint extensionPoint;
        private final Method method;

        ExtensionPointMethod(ContextExtensionPoint extensionPoint, Method method) {
            this.extensionPoint = extensionPoint;
            this.method = method;
        }

        public Class<?>[] getFilteredParameterTypes() {
            List<Class<?>> result = new ArrayList<Class<?>>();
            for (Class<?> parameterType : method.getParameterTypes()) {
                if (!DslEnvironment.class.isAssignableFrom(parameterType)) {
                    result.add(parameterType);
                }
            }
            return result.toArray(new Class<?>[result.size()]);
        }

        @Override
        public String toString() {
            return extensionPoint.getClass() +
                    "." +
                    method.getName() +
                    "(" +
                    join(convertClassesToClassNames(Arrays.asList(method.getParameterTypes())), ", ") +
                    ")";
        }

        public Object call(DslEnvironment environment, Object[] args)
                throws InvocationTargetException, IllegalAccessException {
            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] processedArgs = new Object[parameterTypes.length];
            int j = 0;
            for (int i = 0; i < parameterTypes.length; i++) {
                processedArgs[i] = DslEnvironment.class.isAssignableFrom(parameterTypes[i]) ? environment : args[j++];
            }
            return method.invoke(extensionPoint, processedArgs);
        }
    }
}
