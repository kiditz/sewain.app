package app.sewain.integrations;

import java.util.Set;

/** Implemented by built-in plugins; new providers register as Spring components. */
public interface IntegrationPlugin {
    String code();
    String name();
    String description();
    Set<String> capabilities();
}
