package mate.academy.lib;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Injector {
    private static final Injector injector = new Injector();

    private Map<Class<?>, Object> instances = new HashMap<>();
    private Map<Class<?>, Class<?>> binds = new HashMap<>();

    public static Injector getInjector() {
        return injector;
    }

    public void bind(Class<?> interfaceForImpl, Class<?> implementation) {
        binds.put(interfaceForImpl, implementation);
    }

    public Object getInstance(Class<?> interfaceClazz) throws IllegalAccessException {
        if (instances.containsKey(interfaceClazz)) {
            return instances.get(interfaceClazz);
        }
        if (interfaceClazz.isInterface()) {
            Class<?> clazz = binds.get(interfaceClazz);
            if (clazz == null) {
                throw new RuntimeException("For interface: " + interfaceClazz
                        + " there is no implementation class");
            }
            interfaceClazz = clazz;
        }
        Object instance;
        try {
            instance = interfaceClazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (Field field : interfaceClazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Inject.class)) {
                field.setAccessible(true);
                Object dependency = getInstance(field.getType());
                field.set(instance, dependency);
            }
        }

        instances.put(interfaceClazz, instance);
        return instance;
    }

    public void scanPackage(String packageName) {
        String path = packageName.replace(".", "/");
        URL resource = Thread.currentThread()
                .getContextClassLoader()
                .getResource(path);
        if (resource == null) {
            throw new RuntimeException("Package not found - " + packageName);
        }
        File directory = new File(resource.getFile());
        if (!directory.exists()) {
            throw new RuntimeException("Directory doesn't exist " + directory);
        }

        for (File file : directory.listFiles()) {
            if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName()
                        .replace(".class", "");
                Class<?> clazz = null;
                try {
                    clazz = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                if(clazz.isAnnotationPresent(Component.class)) {
                    registerComponent(clazz);
                }
            }
        }
    }

    public void registerComponent(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Component.class)) {
                Class<?>[] interfaces = clazz.getInterfaces();
                if (interfaces.length > 1) {
                    throw new RuntimeException("Class " + clazz.getName()
                            + " implements more than 1 interface");
                } else if (interfaces.length == 1) {
                    binds.put(interfaces[0], clazz);
                }
            }
        }
    }
}
