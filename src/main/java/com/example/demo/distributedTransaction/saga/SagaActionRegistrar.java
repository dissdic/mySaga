package com.example.demo.distributedTransaction.saga;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SagaActionRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    private ResourceLoader resourceLoader;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        //注册beanPostProcessor
        Map<String,Object> attributes = importingClassMetadata.getAnnotationAttributes(EnableSaga.class.getName());
        AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(attributes);
        Set<String> packages = new LinkedHashSet<>();
        if(annotationAttributes!=null){
            String[] basePackages = annotationAttributes.getStringArray("basePackages");
            if(basePackages.length>0){
                packages.addAll(Arrays.asList(basePackages));
            }
        }

        if(packages.isEmpty()){
            packages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        }

        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry);
        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(SagaAction.class));
        scanner.scan(packages.toArray(new String[0]));

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(SagaAnnotationPostProcessor.class);
        builder.addConstructorArgValue(packages);
        builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), registry);
        System.out.println("successfully registry saga beanPostProcessor");
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
