/*
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0 which accompany this distribution.
 *
 * The Apache License is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package io.cloudslang.lang.compiler.modeller.transformers;

import io.cloudslang.lang.compiler.validator.ExecutableValidator;
import io.cloudslang.lang.compiler.validator.PreCompileValidator;
import io.cloudslang.lang.entities.bindings.InOutParam;
import io.cloudslang.lang.entities.bindings.Input;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import static io.cloudslang.lang.compiler.SlangTextualKeys.DEFAULT_KEY;
import static io.cloudslang.lang.compiler.SlangTextualKeys.PRIVATE_INPUT_KEY;
import static io.cloudslang.lang.compiler.SlangTextualKeys.REQUIRED_KEY;
import static io.cloudslang.lang.compiler.SlangTextualKeys.SENSITIVE_KEY;

public abstract class AbstractInputsTransformer extends InOutTransformer {

    @Autowired
    protected PreCompileValidator preCompileValidator;

    @Autowired
    private ExecutableValidator executableValidator;

    @Override
    public Class<? extends InOutParam> getTransformedObjectsClass() {
        return Input.class;
    }

    protected Input transformSingleInput(Object rawInput) {
        if (rawInput instanceof String) {
            String inputName = (String) rawInput;
            return createInput(inputName, null);
        } else {
            if (rawInput instanceof Map) {
                @SuppressWarnings(value = { "unchecked" }) Map<String, ?> map = (Map<String, ?>) rawInput;
                Iterator<? extends Map.Entry<String, ?>> iterator = map.entrySet().iterator();
                Map.Entry<String, ?> entry = iterator.next();
                Serializable entryValue = (Serializable) entry.getValue();
                if (map.size() > 1) {
                    throw new RuntimeException("Invalid syntax after input \"" + entry.getKey() + "\". " + "Please check all inputs are provided as a list and each input is preceded by a hyphen. " + "Input \"" + iterator.next().getKey() + "\" is missing the hyphen.");
                }
                if (entryValue == null) {
                    throw new RuntimeException("Could not transform Input : " + rawInput + " since it has a null value.\n\nMake sure a value is specified or that indentation is properly done.");
                }
                if (entryValue instanceof Map) {
                    return createPropInput((Map.Entry<String, Map<String, Serializable>>) entry);
                }
                return createInput(entry.getKey(), entryValue);
            }
        }
        throw new RuntimeException("Could not transform Input : " + rawInput);
    }

    private Input createPropInput(Map.Entry<String, Map<String, Serializable>> entry) {
        Map<String, Serializable> props = entry.getValue();
        List<String> knownKeys = Arrays.asList(REQUIRED_KEY, SENSITIVE_KEY, PRIVATE_INPUT_KEY, DEFAULT_KEY);
        for (String key : props.keySet()) {
            if (!knownKeys.contains(key)) {
                throw new RuntimeException("key: " + key + " in input: " + entry.getKey() + " is not a known property");
            }
        }
        boolean required = !props.containsKey(REQUIRED_KEY) || (boolean) props.get(REQUIRED_KEY);
        boolean sensitive = props.containsKey(SENSITIVE_KEY) && (boolean) props.get(SENSITIVE_KEY);
        boolean privateInput = props.containsKey(PRIVATE_INPUT_KEY) && (boolean) props.get(PRIVATE_INPUT_KEY);
        boolean defaultSpecified = props.containsKey(DEFAULT_KEY);
        String inputName = entry.getKey();
        Serializable value = defaultSpecified ? props.get(DEFAULT_KEY) : null;
        if (privateInput && !defaultSpecified) {
            throw new RuntimeException("Input: " + inputName + " is private but no default value was specified");
        }
        return createInput(inputName, value, sensitive, required, privateInput);
    }

    private Input createInput(String name, Serializable value) {
        return createInput(name, value, false, true, false);
    }

    private Input createInput(String name, Serializable value, boolean sensitive, boolean required, boolean privateInput) {
        executableValidator.validateInputName(name);
        preCompileValidator.validateStringValue(name, value, this);
        Accumulator dependencyAccumulator = extractFunctionData(value);
        return new Input.InputBuilder(name, value, sensitive).withRequired(required).withPrivateInput(privateInput).withFunctionDependencies(dependencyAccumulator.getFunctionDependencies()).withSystemPropertyDependencies(dependencyAccumulator.getSystemPropertyDependencies()).build();
    }
}
