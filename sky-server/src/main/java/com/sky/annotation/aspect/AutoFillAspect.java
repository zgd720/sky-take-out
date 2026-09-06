package com.sky.annotation.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面类：实现公共字段的自动填充
 */

@Aspect
@Component //交给spring容器管理
@Slf4j //日志
public class AutoFillAspect {

    /**
     * 切入点
     */
    //切入点表达式，匹配com.sky.mapper包下的所有方法,并且这些方法上带有@AutoFill注解
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {}

    /**
     * 前置通知
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始执行公共字段自动填充...");

        //获取当前被拦截到的方法上的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();//方法签名对象
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);//获得方法上的注解对象
        OperationType operationType = autoFill.value();//获得数据库的操作类型

        //获取到前被拦截的实体对象
        Object[] args = joinPoint.getArgs();//获得所有参数
        if(args == null || args.length == 0) {
            log.error("参数为空");
            return;
        }

        Object entity = args[0];//约定：实体参数为第一个参数

        //准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        //根据当前操作类型赋值
        if (operationType == OperationType.INSERT){
            //插入操作,为四个公共字段赋值
            try {
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);//获得setCreateTime方法
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);//获得setCreateUser方法
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);//获得setUpdateTime方法
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);//获得setUpdateUser方法

                //通过反射为四个属性对象赋值
                setCreateTime.invoke(entity, now);
                setCreateUser.invoke(entity, currentId);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }else if (operationType == OperationType.UPDATE){
            //更新操作,为两个公共字段赋值
            try {
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);//获得setUpdateUser方法
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);//获得setUpdateTime方法

                setUpdateUser.invoke(entity, currentId);
                setUpdateTime.invoke(entity, now);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }
}
