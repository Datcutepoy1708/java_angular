package com.store.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    String module(); // ROLE, STAFF, CUSTOMER, ORDER, INVENTORY, PRODUCT, DISCOUNT, SETTING, RETURN_REFUND

    String actionType(); // CREATE, UPDATE, DELETE, LOGIN, STATUS_CHANGE, REFUND, EXPORT

    String description() default "";
}
