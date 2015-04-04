package io.github.hidroh.materialistic;

/**
 * Interface for context that can be injected with dependencies
 */
public interface Injectable {
    /**
     * Injects the members of given object, including injectable members
     * inherited from its supertypes.
     * @param object object with members to be injected
     */
    void inject(Object object);
}
