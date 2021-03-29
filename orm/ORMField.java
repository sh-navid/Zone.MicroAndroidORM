package partial_framework.orm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ORMField {
    String name() default "";

    boolean isPrimaryKey() default false;

    boolean canBeNull() default false;

    boolean isUnique() default false;
}
