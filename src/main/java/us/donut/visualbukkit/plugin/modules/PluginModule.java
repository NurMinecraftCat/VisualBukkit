package us.donut.visualbukkit.plugin.modules;

import com.google.common.collect.ObjectArrays;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import org.bstats.bukkit.Metrics;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import us.donut.visualbukkit.plugin.modules.classes.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum PluginModule {

    BSTATS(Metrics.class),
    DATABASE(ObjectArrays.concat(DatabaseManager.class, getClasses("com.zaxxer.hikari"))),
    DURATION(Duration.class),
    REFLECTION_UTIL(ReflectionUtil.class),
    WORLDGUARD(WorldGuardHook.class),
    VAULT(VaultHook.class),

    GUI(GuiManager.class, GuiIdentifier.class) {
        @Override
        public void insertInto(JavaClassSource mainClass) {
            MethodSource<JavaClassSource> enableMethod = mainClass.getMethod("onEnable");
            enableMethod.setBody(enableMethod.getBody() + "getServer().getPluginManager().registerEvents(GuiManager.getInstance(), this);");
        }
    },

    PlACEHOLDERAPI(ExpansionHandler.class, PapiExpansion.class, PlaceholderEvent.class) {
        @Override
        public void insertInto(JavaClassSource mainClass) {
            MethodSource<JavaClassSource> enableMethod = mainClass.getMethod("onEnable");
            enableMethod.setBody(enableMethod.getBody() +
                    "if (Bukkit.getPluginManager().getPlugin(\"PlaceholderAPI\") != null) {" +
                    "ExpansionHandler.register(this);" +
                    "}");
        }
    },

    VARIABLES(VariableManager.class) {
        @Override
        public void insertInto(JavaClassSource mainClass) {
            MethodSource<JavaClassSource> enableMethod = mainClass.getMethod("onEnable");
            enableMethod.setBody("VariableManager.loadVariables(this);" + enableMethod.getBody());
            MethodSource<JavaClassSource> disableMethod = mainClass.getMethod("onDisable");
            disableMethod.setBody(disableMethod.getBody() + "VariableManager.saveVariables();");
        }
    };

    private static Class<?>[] getClasses(String packageName) {
        Set<Class<?>> classes = new HashSet<>();
        for (Class<?> clazz : new Reflections(packageName, new SubTypesScanner(false)).getSubTypesOf(Object.class)) {
            classes.add(clazz);
            try {
                classes.addAll(Arrays.asList(clazz.getDeclaredClasses()));
            } catch (NoClassDefFoundError ignored) {}
        }
        return classes.toArray(new Class<?>[0]);
    }

    private Set<Class<?>> classes = new HashSet<>();

    PluginModule(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            this.classes.add(clazz);
            try {
                for (CtClass ctClass : ClassPool.getDefault().getCtClass(clazz.getName()).getNestedClasses()) {
                    this.classes.add(Class.forName(ctClass.getName()));
                }
            } catch (NoClassDefFoundError | NotFoundException | ClassNotFoundException ignored) {}
        }
    }

    public void insertInto(JavaClassSource mainClass) {}

    public Set<Class<?>> getClasses() {
        return classes;
    }
}
