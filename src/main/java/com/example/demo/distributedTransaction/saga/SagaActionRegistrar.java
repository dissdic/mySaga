package com.example.demo.distributedTransaction.saga;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SagaActionRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        ExecutorService service = Executors.newFixedThreadPool(1);

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

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(SagaAnnotationPostProcessor.class);
        builder.addConstructorArgValue(packages);
        builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), registry);
        System.out.println("successfully registry saga beanPostProcessor");
    }
}
