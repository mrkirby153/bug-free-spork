package com.mrkirby153.bfs.model.enhancers;

import com.mrkirby153.bfs.model.Enhancer;
import com.mrkirby153.bfs.model.Model;
import com.mrkirby153.bfs.model.annotations.Enhancers;
import com.mrkirby153.bfs.model.annotations.Timestamps;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class EnhancerUtils {

    private static Map<Class<? extends Model>, List<Class<? extends Enhancer>>> modelEnhancerCache = new HashMap<>();
    private static Map<Class<? extends Enhancer>, Enhancer> cachedEnhancers = new HashMap<>();

    /**
     * Gets a list of enhancers for the model.
     * <br/>
     * Due to the potentially frequent accessing of this data, the enhancers and their instances
     * are cached on first use and the cached values are returned when present
     *
     * @param model The model to get enhancers for
     *
     * @return A list of enhancers
     */
    public static List<Enhancer> getEnhancers(Class<? extends Model> model) {
        List<Class<? extends Enhancer>> modelEnhancers = modelEnhancerCache.get(model);
        if (modelEnhancers == null) {
            modelEnhancers = new ArrayList<>();
            Enhancers annotation = model.getAnnotation(Enhancers.class);
            if (annotation != null) {Arrays.stream(annotation.value()).map(
                    com.mrkirby153.bfs.model.annotations.Enhancer::value).forEach(modelEnhancers::add);
            } else {
                com.mrkirby153.bfs.model.annotations.Enhancer enhancerAnnotation = model.getAnnotation(
                    com.mrkirby153.bfs.model.annotations.Enhancer.class);
                if(enhancerAnnotation != null) {
                    modelEnhancers.add(enhancerAnnotation.value());
                }
            }
            if(model.isAnnotationPresent(Timestamps.class)) {
                modelEnhancers.add(TimestampEnhancer.class);
            }
            log.trace("Caching enhancers for {}", model);
            modelEnhancerCache.put(model, modelEnhancers);
        }
        List<Enhancer> enhancers = new ArrayList<>();
        for (Class<? extends Enhancer> clazz : modelEnhancers) {
            if (cachedEnhancers.containsKey(clazz)) {
                enhancers.add(cachedEnhancers.get(clazz));
            } else {
                try {
                    Enhancer e = clazz.getConstructor().newInstance();
                    log.trace("Caching instance of enhancer {}", clazz);
                    cachedEnhancers.put(clazz, e);
                    enhancers.add(e);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                    log.error("Could not instantiate enhancer {}", clazz, ex);
                }
            }
        }
        return enhancers;
    }

    /**
     * Gets a list of enhancers for the model without the given enhancers
     *
     * @param model     The model to get enhancers for
     * @param enhancers The enhancers to exclude
     *
     * @return The enhancers
     */
    public static List<Enhancer> withoutEnhancers(Class<? extends Model> model, String... enhancers) {
        List<Enhancer> e = getEnhancers(model);
        List<String> toRemove = Arrays.asList(enhancers);
        e.removeIf(enhancer -> toRemove.contains(enhancer.name()));
        return e;
    }
}
