package com.momentum.releaser.global.config;

import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.dialect.MySQL8Dialect;

public class MySQL8DialectCustom extends MySQL8Dialect {

    public MySQL8DialectCustom() {
        super();

        // Add the custom match function template
        registerFunction(
                "match",
                new SQLFunctionTemplate(StandardBasicTypes.DOUBLE, "match(?1) against (?2 in boolean mode)")
        );
    }

}
