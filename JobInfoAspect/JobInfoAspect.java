import org.apache.commons.lang.exception.ExceptionUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @CreateDate: 2018/9/25 16:44
 * @UpdateUser: jiangzihan
 * @UpdateDate: 2018/9/25 16:44
 * @UpdateRemark: The modified content
 * @Version: 1.0
 * Copyright: Copyright (c) 2018
 */
@Component
@Aspect
public class JobInfoAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobInfoAspect.class);

    //使用ThreadLocal存储
    public final static ThreadLocal<Long> THREADLOCAL_JOBID = new ThreadLocal<Long>();
    public final static ThreadLocal<Long> THREADLOCAL_STARTTIME = new ThreadLocal<Long>();

    @Autowired
    private JobInfoService JobInfoService;//自定义的持久化任务状态和过程到本地
    //这个里面指定包名(com.jiangzihan.jobhandler),类名样式(*Process),还有方法(execute)
    @Pointcut("execution(* com.jiangzihan.jobhandler..*Process.execute(..))")
    public void pointcutName() {
    }


    @Before("pointcutName()")
    public void before(JoinPoint joinPoint) {
        String targetName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        LOGGER.info("Before开始==========执行类[方法]{}，数据入参：{}", targetName, methodName);
        Integer count = JobInfoService.selectRunTimerByJobName(targetName);
        JobInfo jobInfo = new JobInfo();
        Date startTime = new Date();
        jobInfo.setJobName(targetName);
        jobInfo.setRunTimes(count == null ? 0 : ++count);
        jobInfo.setStartTime(startTime);
        jobInfo.setStatus(JobInfoStatusEnum.JOB_PROCESS.getCode());//自定义一个维护状态的枚举
        JobInfoService.insert(jobInfo);
        THREADLOCAL_JOBID.set(jobInfo.getId());
        THREADLOCAL_STARTTIME.set(startTime.getTime());
        LOGGER.info("Before结束==========执行类[方法]{}，数据入参：{}", targetName, methodName);
    }


    @AfterThrowing(pointcut = "pointcutName()", throwing = "e")
    public void afterThrowing(JoinPoint joinPoint, Throwable e) {
        String targetName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        LOGGER.info("AfterThrowing开始==========执行类[方法]{}，数据入参：{}", targetName, methodName);
        JobInfo updateInfo = new JobInfo();

        updateInfo.setEndTime(new Date());
        Long processTime = updateInfo.getEndTime().getTime() - THREADLOCAL_STARTTIME.get();
        BigDecimal second = new BigDecimal(
                (processTime) / 1000).add(new BigDecimal(processTime % 1000).divide(new BigDecimal("1000"))
        );
        updateInfo.setExecuteSecond(second);
        updateInfo.setId(THREADLOCAL_JOBID.get());
        updateInfo.setStatus(JobInfoStatusEnum.JOB_FAILED.getCode());//自定义一个维护状态的枚举
        updateInfo.setErrorInfo(ExceptionUtils.getStackTrace(e));
        JobInfoService.updateById(updateInfo);

        THREADLOCAL_JOBID.remove();
        THREADLOCAL_STARTTIME.remove();
        LOGGER.info("AfterThrowing结束==========执行类[方法]{}，数据入参：{}", targetName, methodName);
    }

    @AfterReturning(pointcut = "pointcutName()", returning = "rvt")
    public void afterReturning(JoinPoint joinPoint, Object rvt) {
        JobInfo updateInfo = new JobInfo();
        String targetName = joinPoint.getTarget().getClass().getSimpleName();//自定义一个维护状态的枚举
        String methodName = joinPoint.getSignature().getName();
        LOGGER.info("AfterReturning开始==========执行类[方法]{}，数据入参：{}", targetName, methodName);

        updateInfo.setEndTime(new Date());
        Long processTime = updateInfo.getEndTime().getTime() - THREADLOCAL_STARTTIME.get();
        BigDecimal second = new BigDecimal(
                (processTime) / 1000).add(new BigDecimal(processTime % 1000).divide(new BigDecimal("1000"))
        );
        updateInfo.setExecuteSecond(second);
        updateInfo.setId(THREADLOCAL_JOBID.get());
        updateInfo.setStatus(JobInfoStatusEnum.JOB_SUCCESS.getCode());
        JobInfoService.updateById(updateInfo);

        THREADLOCAL_JOBID.remove();
        THREADLOCAL_STARTTIME.remove();
        LOGGER.info("AfterReturning结束==========执行类[方法]{}，数据入参：{}", targetName, methodName);
    }

}