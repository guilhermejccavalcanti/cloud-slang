package io.cloudslang.lang.compiler.parser;
/*******************************************************************************
* (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Apache License v2.0 which accompany this distribution.
*
* The Apache License is available at
* http://www.apache.org/licenses/LICENSE-2.0
*
*******************************************************************************/


/*
 * Created by orius123 on 05/11/14.
 */

import org.apache.commons.lang3.Validate;
import io.cloudslang.lang.compiler.SlangSource;
import io.cloudslang.lang.compiler.parser.model.ParsedSlang;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.scanner.ScannerException;

@Component
public class YamlParser {

    public static final String KEY_VALUE_PAIR_MISSING_MSG = "Probably did not provide (key: value) pair or missing space after colon(:). Also check that everything is indented properly";
    @Autowired
    private Yaml yaml;

    public ParsedSlang parse(SlangSource source) {

        Validate.notEmpty(source.getSource(), "Source " + source.getName() + " cannot be empty");

        try {
            ParsedSlang parsedSlang = yaml.loadAs(source.getSource(), ParsedSlang.class);
            if(parsedSlang == null) {
                throw new RuntimeException("Source " + source.getName() + " does not contain YAML content");
            }
            parsedSlang.setName(source.getName());
            return parsedSlang;
        } catch (Throwable e) {
            String errorMessage = e.getMessage();
            if (e instanceof ScannerException && (errorMessage.startsWith("mapping values") || errorMessage.startsWith("while scanning a simple key"))){
                errorMessage += KEY_VALUE_PAIR_MISSING_MSG;
            }
            else if (e instanceof ConstructorException && errorMessage.startsWith("Cannot create property")){
                if (errorMessage.contains("Unable to find property")){
                    String truncatedErrorMessage = errorMessage.substring(errorMessage.indexOf("Unable"), errorMessage.indexOf("on class"));
                    String undefinedProperty = truncatedErrorMessage.substring(truncatedErrorMessage.indexOf("\'")+1, truncatedErrorMessage.lastIndexOf("\'"));
                    errorMessage += "Property \'" + undefinedProperty + "\' is not supported by CloudSlang. Check that \'" + undefinedProperty + "\' is indented properly.";
                }
                else if (errorMessage.contains("No single argument constructor found for interface java.util.Map")){
                    errorMessage += KEY_VALUE_PAIR_MISSING_MSG;
                }
            }
            throw new RuntimeException("There was a problem parsing the YAML source: " + source.getName() + ".\n" + errorMessage, e);
        }
    }

}
