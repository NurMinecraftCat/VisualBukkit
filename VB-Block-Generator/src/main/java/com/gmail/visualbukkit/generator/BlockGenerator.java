package com.gmail.visualbukkit.generator;

import com.google.common.hash.Hashing;
import com.google.common.reflect.ClassPath;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class BlockGenerator {

    private Map<String, JSONObject> blockMap = new TreeMap<>();
    private Map<String, String> langMap = new TreeMap<>();

    private Path dir;
    private Path blocksFile;
    private Path langFile;

    private Set<String> blacklist = new HashSet<>();
    private String category;
    private String pluginModule;

    private static Class<?> eventClass;

    static {
        try {
            eventClass = Class.forName("org.bukkit.event.Event");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public BlockGenerator(Path dir, Path blocksFile, Path langFile) throws IOException {
        this.dir = dir;
        this.blocksFile = blocksFile;
        this.langFile = langFile;

        if (Files.exists(blocksFile)) {
            for (Object obj : new JSONArray(Files.readString(blocksFile))) {
                JSONObject json = (JSONObject) obj;
                blockMap.put(json.getString("id"), json);
            }
        }

        if (Files.exists(langFile)) {
            for (String line : Files.readAllLines(langFile)) {
                int i = line.indexOf("=");
                langMap.put(line.substring(0, i), line.substring(i + 1));
            }
        }
    }

    public void writeFiles() throws IOException {
        if (Files.notExists(dir)) {
            Files.createDirectories(dir);
        }

        JSONArray blockArray = new JSONArray();
        blockMap.values().forEach(blockArray::put);

        StringJoiner langString = new StringJoiner("\n");
        langMap.forEach((key, value) -> langString.add(key + "=" + value));

        Files.writeString(blocksFile, blockArray.toString(2));
        Files.writeString(langFile, langString.toString());
    }

    @SuppressWarnings("UnstableApiUsage")
    public void generate(String packageName) throws IOException {
        for (ClassPath.ClassInfo classInfo : ClassPath.from(ClassLoader.getSystemClassLoader()).getTopLevelClasses(packageName)) {
            Class<?> clazz = classInfo.load();
            generatePackageClass(clazz);
            for (Class<?> innerClass : clazz.getDeclaredClasses()) {
                generatePackageClass(innerClass);
            }
        }
    }

    private void generatePackageClass(Class<?> clazz) {
        if (!clazz.isAnonymousClass() && !clazz.isAnnotationPresent(Deprecated.class) && !blacklist.contains(clazz.toString())) {
            generate(clazz);
        }
    }

    public void generate(Class<?> clazz) {
        if (eventClass.isAssignableFrom(clazz)) {
            String id = hash(clazz.toString());
            if (!blockMap.containsKey(id)) {
                JSONObject json = new JSONObject();
                json.put("id", id);
                json.put("event", clazz.getName());
                json.putOpt("plugin-module", pluginModule);
                blockMap.put(id, json);
            }
        } else {
            for (Constructor<?> constructor : clazz.getConstructors()) {
                if (Modifier.isPublic(constructor.getModifiers()) && !constructor.isAnnotationPresent(Deprecated.class) && !blacklist.contains(constructor.toString())) {
                    String id = hash(constructor.toString());
                    if (!blockMap.containsKey(id)) {
                        blockMap.put(id, generate(constructor, id));
                    }
                    langMap.putIfAbsent(id + ".title", "New " + clazz.getSimpleName());
                    if (category != null) {
                        langMap.putIfAbsent(id + ".category", category);
                    }
                }
            }
        }

        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && !method.isAnnotationPresent(Deprecated.class) && !blacklist.contains(method.toString()) && !blacklist.contains(method.getName())) {
                String id = hash(method.toString());
                if (!blockMap.containsKey(id)) {
                    JSONObject json = generate(method, id);
                    json.put("method", method.getName());
                    if (method.getReturnType() != void.class) {
                        json.put("return", method.getReturnType().getName());
                    }
                    if (Modifier.isStatic(method.getModifiers())) {
                        json.put("static", true);
                    }
                    blockMap.put(id, json);
                }
                langMap.putIfAbsent(id + ".title", clazz.getSimpleName() + "_" + method.getName());
                if (category != null) {
                    langMap.putIfAbsent(id + ".category", category);
                }
            }
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers()) && !field.isAnnotationPresent(Deprecated.class) && !blacklist.contains(field.toString())) {
                String id = hash(field.toString());
                if (!blockMap.containsKey(id)) {
                    JSONObject json = new JSONObject();
                    json.put("id", id);
                    json.put("class", clazz.getName());
                    json.put("field", field.getName());
                    json.put("return", field.getType().getName());
                    json.putOpt("plugin-module", pluginModule);
                    if (Modifier.isStatic(field.getModifiers())) {
                        json.put("static", true);
                    } else {
                        langMap.computeIfAbsent(id + ".parameters", k -> clazz.getSimpleName());
                    }
                    blockMap.put(id, json);
                }
                langMap.putIfAbsent(id + ".title", clazz.getSimpleName() + "_" + field.getName());
                if (category != null) {
                    langMap.putIfAbsent(id + ".category", category);
                }
            }
        }
    }

    private JSONObject generate(Executable executable, String id) {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("class", executable.getDeclaringClass().getName());
        json.putOpt("plugin-module", pluginModule);
        for (Class<?> parameterClass : executable.getParameterTypes()) {
            json.append("parameters", parameterClass.getName());
        }
        if (!Modifier.isStatic(executable.getModifiers()) || executable.getParameterCount() > 0) {
            langMap.computeIfAbsent(id + ".parameters", k -> getParameterNames(executable));
        }
        return json;
    }

    public void reset() {
        category = null;
        pluginModule = null;
    }

    public void addToBlackList(String string) {
        blacklist.add(string);
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setPluginModule(String pluginModule) {
        this.pluginModule = pluginModule;
    }

    private static String getParameterNames(Executable executable) {
        StringJoiner joiner = new StringJoiner(",");
        if (!Modifier.isStatic(executable.getModifiers()) && !eventClass.isAssignableFrom(executable.getDeclaringClass())) {
            joiner.add(executable.getDeclaringClass().getSimpleName());
        }
        for (Parameter parameter : executable.getParameters()) {
            joiner.add(parameter.getName());
        }
        return joiner.toString();
    }

    @SuppressWarnings("UnstableApiUsage")
    private static String hash(String string) {
        return Hashing.murmur3_128().hashString(string, StandardCharsets.UTF_8).toString();
    }
}
