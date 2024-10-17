package com.example.demo.distributedTransaction.saga;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.InjectionMetadata;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
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


        InjectionMetadata metadata = findAnnotatedFields(bean);
        try {
            metadata.inject(bean,beanName,pvs);
        } catch (Throwable throwable) {
            throw new SagaWrapperException("creation or injection of proxy object fail");
        }
        return pvs;
    }

    private InjectionMetadata findAnnotatedFields(Object bean){
        Class<?> clazz = bean.getClass();
        List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
        InjectionMetadata injectionMetadata = new InjectionMetadata(clazz,elements);
        Class<?> targetClazz = clazz;
        do {
            ReflectionUtils.doWithLocalFields(targetClazz,field -> {
                if(field.isAnnotationPresent(SagaAction.class)){
                    //可能需要考虑private protect
                    Object obj = ReflectionUtils.getField(field,bean);
                    Object value = null;
                    if(obj==null){
                        value = doCreateProxy(field.getAnnotation(SagaAction.class),beanFactory.getBean(field.getType()));
                    }else{
                        value = doCreateProxy(field.getAnnotation(SagaAction.class),obj);
                    }
                    SagaAnnotationInjectedElement element = new SagaAnnotationInjectedElement(field, value);
                    elements.add(element);
                }
            });
            targetClazz = targetClazz.getSuperclass();
        }while(targetClazz!=null);

        return injectionMetadata;
    }

    private Object doCreateProxy(SagaAction sagaAction, Object object){
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
                        doRollback(e,object.getClass(),annotateMethod.rollbackMethodFullName(),object,args);
                        return null;
                    }
                }
                return method.invoke(object,args);
            }
        });
        return enhancer.create();
    }

    public void doRollback(SagaWrapperException e, Class<?> clazz, String rollbackMethodName,Object object, Object[] args){
        Exception ex = e.getWrappedException();
        ex.printStackTrace();
        //调用回滚方法
        java.lang.reflect.Method method1 = ReflectionUtils.findMethod(clazz,rollbackMethodName);
        if(method1!=null){
            ReflectionUtils.makeAccessible(method1);
            Parameter[] parameters = method1.getParameters();
            if(parameters.length>0){
                ReflectionUtils.invokeMethod(method1,object,args);
            }else{
                ReflectionUtils.invokeMethod(method1,object);
            }
        }else{
            throw new SagaWrapperException("no rollback method found");
        }
    }

    private Object createProxy(Object object){
        SagaAction sagaAction = object.getClass().getAnnotation(SagaAction.class);
        if(sagaAction != null){
            return doCreateProxy(sagaAction,object);
        }else{

            List<java.lang.reflect.Method> methods = new ArrayList<>();
            Class<?> targetClass = object.getClass();
            do{
                ReflectionUtils.doWithLocalMethods(targetClass,method -> {
                    Annotation annotation = method.getAnnotation(SagaAction.class);
                    if(annotation!=null){
                        methods.add(method);
                    }
                });
                targetClass = targetClass.getSuperclass();
            }while (targetClass!=null);
            if(!CollectionUtils.isEmpty(methods)){
                //生成代理
                Enhancer enhancer = new Enhancer();
                enhancer.setSuperclass(object.getClass());
                enhancer.setCallback(new MethodInterceptor() {
                    @Override
                    public Object intercept(Object o, java.lang.reflect.Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                        java.lang.reflect.Method matchMethod = methods.stream().takeWhile(c->c.equals(method)).findFirst().orElse(null);
                        if(matchMethod!=null){
                            SagaAction action = matchMethod.getAnnotation(SagaAction.class);
                            try {
                                return retry(()->method.invoke(object,objects),action.retry());
                            }catch (SagaWrapperException e){
                                doRollback(e,object.getClass(),action.rollbackMethodFullName(),object,objects);
                                return null;
                            }
                        }
                        return method.invoke(object,objects);
                    }
                });
                return enhancer.create();
            }
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

    private static class SagaAnnotationInjectedElement extends InjectionMetadata.InjectedElement {

        private final Object proxy;

        protected  SagaAnnotationInjectedElement(Field field,Object proxy){
            super(field, null);
            this.proxy = proxy;
        }


        @Override
        protected void inject(Object target, String requestingBeanName, PropertyValues pvs) throws Throwable {
            ReflectionUtils.makeAccessible((Field) member);
            ReflectionUtils.setField((Field) member,target,proxy);
        }
    }

}
