package com.example.demo.distributedTransaction.saga;

import com.example.demo.distributedTransaction.rpc.external.TestExternal;
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
import org.springframework.util.NumberUtils;
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
        InstantiationAwareBeanPostProcessor,
        BeanFactoryAware,
        DisposableBean,
        InitializingBean {

    private BeanFactory beanFactory;

    private Set<String> packages;

    private static final ThreadLocal<SagaTransaction> actions = new ThreadLocal<>();

    public SagaAnnotationPostProcessor(Collection<String> packages) {
        this.packages  = new LinkedHashSet<>(packages);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("解析器初始化完成");
    }



    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void destroy() throws Exception {
        System.out.println("解析器销毁");
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
            throw new SagaWrapperException("creation or injection of proxy object failed");
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
                        value = doCreateProxy(field.getAnnotation(SagaAction.class),null,beanFactory.getBean(field.getType()));
                    }else{
                        value = doCreateProxy(field.getAnnotation(SagaAction.class),null,obj);
                    }
                    SagaAnnotationInjectedElement element = new SagaAnnotationInjectedElement(field, value);
                    elements.add(element);
                }
            });
            targetClazz = targetClazz.getSuperclass();
        }while(targetClazz!=null);

        return injectionMetadata;
    }

    private Object doCreateProxy(SagaAction sagaAction, java.lang.reflect.Method[] methods, Object object){
        //todo do validate rollback
        Method[] methods_ = sagaAction!=null?sagaAction.methods():null;
        Class<?> clazz = object.getClass();
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);

        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, java.lang.reflect.Method method, Object[] args, MethodProxy proxy) throws Throwable {

                SagaAnnotationMetaData data = ifAnnotationPresent(method,methods_,methods);
                if(data != null){
                    try {
                        Object result = retry(()->method.invoke(object,args),data.getRetry());
                        addRollback(clazz,data.getRollbackMethodFullName(),args,object);
                        return result;
                    }catch (SagaWrapperException e){
                        doRollback(e);
                        return null;
                    }
                }
                return method.invoke(object,args);
            }
        });
        return enhancer.create();
    }


    private SagaAnnotationMetaData ifAnnotationPresent(java.lang.reflect.Method method, Method[] method1, java.lang.reflect.Method[] method2){
        if(method1!=null && method1.length>0){
            String currentMethodName = method.getName();
            Method matchMethod = Arrays.stream(method1).filter(c->currentMethodName.equals(c.name())).findFirst().orElse(null);
            if(matchMethod!=null){
                return new SagaAnnotationMetaData(matchMethod.retry(),matchMethod.rollbackMethodFullName());
            }
        }
        if(method2!=null && method2.length>0){
            java.lang.reflect.Method matchMethod = Arrays.stream(method2).takeWhile(c->c.equals(method)).findFirst().orElse(null);
            if(matchMethod!=null){
                SagaAction action = matchMethod.getAnnotation(SagaAction.class);
                return new SagaAnnotationMetaData(action.retry(),action.rollbackMethodFullName());
            }
        }
        return null;
    }

    public void doRollback(SagaWrapperException e){
        Exception ex = e.getWrappedException();
        ex.printStackTrace();
        //调用回滚方法
        SagaTransaction sagaTransaction = actions.get();
        if(sagaTransaction!=null){
            ArrayDeque<SagaTransaction.Rollback> rollbacks = sagaTransaction.getRollBackChain();
            while(!rollbacks.isEmpty()){
                SagaTransaction.Rollback rollback = rollbacks.pop();
                java.lang.reflect.Method method1 = ReflectionUtils.findMethod(rollback.getClazz(),rollback.getMethodName());
                if(method1!=null){
                    if(!method1.getReturnType().equals(int.class)){
                        rollback.setState(SagaTransaction.Rollback.FAIL);
                        throw new SagaWrapperException("the returnType of a rollback method should be int");
                    }
                    ReflectionUtils.makeAccessible(method1);
                    Parameter[] parameters = method1.getParameters();
                    int response;
                    rollback.setState(SagaTransaction.Rollback.PROCESSING);
                    if(parameters.length>0){
                        response = (int)Optional.ofNullable(ReflectionUtils.invokeMethod(method1,rollback.getTarget(),rollback.getArgs())).orElse(0);
                    }else{
                        response = (int)Optional.ofNullable(ReflectionUtils.invokeMethod(method1,rollback.getTarget())).orElse(0);
                    }
                    if(response==0){
                        rollback.setState(SagaTransaction.Rollback.FAIL);
                        //todo here need change to retry rollback process for 3 times instead of breaking the circle
                        break;
                    }
                }else{
                    rollback.setState(SagaTransaction.Rollback.FAIL);
                    throw new SagaWrapperException("no rollback method found");
                }
            }
        }
    }


    private Object createProxy(Object object){
        SagaAction sagaAction = object.getClass().getAnnotation(SagaAction.class);
        if(sagaAction != null){
            return doCreateProxy(sagaAction,null,object);
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
                return doCreateProxy(null,methods.toArray(new java.lang.reflect.Method[0]), object);
            }
        }
        return object;
    }

    public Object retry(Callable<Object> callable,int times){
        try{
            System.out.println("调用");
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

    private void addRollback(Class<?> beanType, String methodName, Object[] args, Object obj){

        SagaTransaction.Rollback rollback = new SagaTransaction.Rollback();
        rollback.setArgs(args);
        rollback.setClazz(beanType);
        rollback.setMethodName(methodName);
        rollback.setTarget(obj);
        SagaTransaction sagaTransaction = actions.get();
        if(sagaTransaction==null){
            sagaTransaction = new SagaTransaction();
            actions.set(sagaTransaction);
        }
        //生成一组数字作为事务的唯一标识
        Long id = System.currentTimeMillis();
        sagaTransaction.setXid(id);
        ArrayDeque<SagaTransaction.Rollback> rollbacks = sagaTransaction.getRollBackChain();
        rollbacks.push(rollback);
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

    private static class SagaAnnotationMetaData{
        private int retry;
        private String rollbackMethodFullName;

        SagaAnnotationMetaData(int retry, String rollbackMethodFullName){
            this.retry = retry;
            this.rollbackMethodFullName = rollbackMethodFullName;
        }

        public int getRetry() {
            return retry;
        }

        public void setRetry(int retry) {
            this.retry = retry;
        }

        public String getRollbackMethodFullName() {
            return rollbackMethodFullName;
        }

        public void setRollbackMethodFullName(String rollbackMethodFullName) {
            this.rollbackMethodFullName = rollbackMethodFullName;
        }
    }

}
