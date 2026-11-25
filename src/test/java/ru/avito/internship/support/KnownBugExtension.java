package ru.avito.internship.support;

import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.commons.JUnitException;
import org.opentest4j.TestAbortedException;

/** Считает падение теста с @KnownBug ожидаемым, а проход валит сборку. */
public class KnownBugExtension implements InvocationInterceptor {

    @Override
    public void interceptTestMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext)
            throws Throwable {
        KnownBug bug = invocationContext.getExecutable().getAnnotation(KnownBug.class);
        if (bug == null) {
            invocation.proceed();
            return;
        }

        try {
            invocation.proceed();
        } catch (TestAbortedException aborted) {
            throw aborted;
        } catch (AssertionError failure) {
            String message = "Ожидаемое падение по багу " + bug.value() + ": " + bug.summary();
            throw new TestAbortedException(message, failure);
        }

        throw new JUnitException("Тест прошёл, но помечен как известный баг "
                + bug.value()
                + ". Похоже, дефект исправлен: уберите @KnownBug и обновите BUGS.md.");
    }
}
