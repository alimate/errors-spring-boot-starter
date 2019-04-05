package me.alidg.errors;

import org.springframework.lang.Nullable;

/**
 * Defines a contract to refine exceptions. Sometimes we need to dig deeper to find the
 * actual cause of the problem. This interface can help us to transform the given exception
 * before handling it.
 *
 * @author Ali Dehghani
 * @implNote Do not throw exceptions in method implementations.
 */
public interface ExceptionRefiner {

    /**
     * Performs the actual mechanics of exception transformation.
     *
     * <p>Please note that both the method parameter and return type are nullable.</p>
     *
     * @param exception The exception to refine.
     * @return The refined exception.
     */
    @Nullable
    Throwable refine(@Nullable Throwable exception);

    /**
     * A NoOp implementation for the {@link ExceptionRefiner}.
     *
     * @author zarebski-m
     */
    enum NoOp implements ExceptionRefiner {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * Does nothing special.
         *
         * @param exception The exception to refine.
         * @return null, always.
         */
        @Override
        public Throwable refine(Throwable exception) {
            return null;
        }
    }
}
