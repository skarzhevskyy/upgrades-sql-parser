package com.example.ddl;

import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.StringProvider;
import net.sf.jsqlparser.parser.feature.FeatureConfiguration;
import net.sf.jsqlparser.parser.feature.Feature;
import net.sf.jsqlparser.parser.ParserKeywordsUtils;
import org.junit.jupiter.api.Test;

public class ExploreParserTest {
    
    @Test
    void exploreParserOptions() {
        try {
            System.out.println("== Exploring parser configuration options ==");
            
            FeatureConfiguration config = new FeatureConfiguration();
            System.out.println("FeatureConfiguration class: " + config.getClass().getName());
            
            // Let's see what methods are available on FeatureConfiguration
            java.lang.reflect.Method[] methods = config.getClass().getMethods();
            System.out.println("FeatureConfiguration methods:");
            for (java.lang.reflect.Method method : methods) {
                if (!method.getDeclaringClass().equals(Object.class)) {
                    System.out.println("  " + method.getName() + "(" + 
                        java.util.Arrays.stream(method.getParameterTypes())
                            .map(Class::getSimpleName)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("") + 
                        ") -> " + method.getReturnType().getSimpleName());
                }
            }
            
            // Now let's check CCJSqlParser methods
            CCJSqlParser parser = new CCJSqlParser(new StringProvider("test"));
            methods = parser.getClass().getMethods();
            System.out.println("\nCCJSqlParser methods (with 'with' prefix):");
            for (java.lang.reflect.Method method : methods) {
                if (method.getName().startsWith("with")) {
                    System.out.println("  " + method.getName() + "() -> " + method.getReturnType().getSimpleName());
                }
            }
            System.out.println("\nFeature enum values:");
            for (Feature feature : Feature.values()) {
                System.out.println("  " + feature.name() + " -> " + feature.toString());
            }
            
            System.out.println("\nTesting specific features for keywords:");
            
            // Check ParserKeywordsUtils
            System.out.println("\nParserKeywordsUtils methods:");
            java.lang.reflect.Method[] keywordMethods = ParserKeywordsUtils.class.getMethods();
            for (java.lang.reflect.Method method : keywordMethods) {
                if (!method.getDeclaringClass().equals(Object.class)) {
                    System.out.println("  " + method.getName() + "()");
                }
            }
            
            System.out.println("== End exploration ==");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}