package com.example.demo.distributedTransaction.saga;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.Callable;

/*
    生成动态代理
 */
public class SagaAnnotationPostProcessor
        implements
        EnvironmentAware,
        ResourceLoaderAware,
        BeanClassLoaderAware,
        ApplicationContextAware,
        InstantiationAwareBeanPostProcessor,
        MergedBeanDefinitionPostProcessor,
        BeanFactoryAware,
        DisposableBean,
        InitializingBean {

    private ClassLoader classLoader;
    private Environment environment;
    private ResourceLoader resourceLoader;
    private ApplicationContext applicationContext;
    private BeanFactory beanFactory;

    private Set<String> packages;
    public SagaAnnotationPostProcessor(Collection<String> packages) {
        this.packages  = new LinkedHashSet<>(packages);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("all the properties have been set");
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("destroy processor");
    }

    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {

    }


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return createProxy(bean);
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {


        MutablePropertyValues mutablePropertyValues = new MutablePropertyValues(pvs);
        for (PropertyValue pv : mutablePropertyValues) {
            Object value = pv.getValue();
            if(value!=null){
                Object newValue = createProxy(value);
                if(newValue!=value){
                    mutablePropertyValues.add(pv.getName(), newValue);
                }
            }
        }

        return mutablePropertyValues;
    }

    private Object createProxy(Object object){
        SagaAction sagaAction = object.getClass().getAnnotation(SagaAction.class);
        if(sagaAction != null){
            Method[] methods = sagaAction.methods();
            Class<?> clazz = object.getClass();
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(clazz);

            enhancer.setCallback(new MethodInterceptor() {
                @Override
                public Object intercept(Object obj, java.lang.reflect.Method method, Object[] args, MethodProxy proxy) throws Throwable {
                    String currentMethodName = method.getName();
                    Method annotateMethod = Arrays.stream(methods).filter(c->currentMethodName.equals(c.name())).findFirst().orElse(null);
                    if(annotateMethod != null){
                        try {
                            return retry(()->method.invoke(object,args),annotateMethod.retry());
                        }catch (SagaWrapperException e){
                            Exception ex = e.getWrappedException();
                            ex.printStackTrace();
                            //调用回滚方法
                            java.lang.reflect.Method method1 = ReflectionUtils.findMethod(clazz,annotateMethod.rollbackMethodFullName());
                            if(method1!=null){
                                ReflectionUtils.makeAccessible(method1);
                                Parameter[] parameters = method1.getParameters();
                                if(parameters.length>0){
                                    ReflectionUtils.invokeMethod(method1,object,args);
                                }else{
                                    ReflectionUtils.invokeMethod(method1,object);
                                }
                                //回滚后返回null
                                return null;
                            }else{
                                throw new SagaWrapperException("no rollback method found");
                            }
                        }
                    }
                    return method.invoke(object,args);
                }
            });
            return enhancer.create();
        }
        return object;
    }

    public Object retry(Callable<Object> callable,int times){
        try{
            return callable.call();
        }catch (Exception e){
            if(times>0){
                times--;
                return retry(callable,times);
            }else{
                throw new SagaWrapperException(e);
            }
        }
    }

}
