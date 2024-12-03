package com.example.demo.distributedTransaction.fuse;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FuseActionRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    private ResourceLoader resourceLoader;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        Map<String,Object> map = importingClassMetadata.getAnnotationAttributes(EnableFuse.class.getName());
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(map);
        // 获取EnableFuse注解的属性值
        List<String> packages = new ArrayList<>();
        if(attributes != null){
            String[] ps = attributes.getStringArray("packages");
            if(ps.length>0){
                packages.addAll(Arrays.asList(ps));
            }
        }
        if(packages.isEmpty()){
            packages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        }

        //scan
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry);
        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(EnableFuse.class));
        scanner.scan(packages.toArray(new String[0]));



    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
