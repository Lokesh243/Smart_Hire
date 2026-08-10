package com.smarthire.aspect;

import com.smarthire.entity.AuditLog;
import com.smarthire.entity.User;
import com.smarthire.repository.AuditLogRepository;
import com.smarthire.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint,
                              Auditable auditable) throws Throwable {

        Object result = joinPoint.proceed();

        try {
            Authentication auth = SecurityContextHolder
                    .getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {

                String email = auth.getName();
                User actor = userRepository.findByEmail(email).orElse(null);

                String action = auditable.action().isEmpty()
                        ? joinPoint.getSignature().getName()
                        : auditable.action();

                AuditLog log = AuditLog.builder()
                        .actor(actor)
                        .action(action)
                        .entityType(joinPoint.getTarget()
                                .getClass().getSimpleName())
                        .details("Method: " + joinPoint.getSignature().getName())
                        .build();

                auditLogRepository.save(log);
            }
        } catch (Exception e) {
            log.warn("Audit logging failed: {}", e.getMessage());
        }

        return result;
    }
}