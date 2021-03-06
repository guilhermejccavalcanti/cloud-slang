/*******************************************************************************
 * (c) Copyright 2014 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0 which accompany this distribution.
 *
 * The Apache License is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *******************************************************************************/
package io.cloudslang.lang.compiler.modeller.transformers;

import io.cloudslang.lang.compiler.modeller.result.BasicTransformModellingResult;
import io.cloudslang.lang.compiler.modeller.result.TransformModellingResult;
import io.cloudslang.lang.compiler.validator.ExecutableValidator;
import io.cloudslang.lang.entities.ListForLoopStatement;
import io.cloudslang.lang.entities.LoopStatement;
import io.cloudslang.lang.entities.MapForLoopStatement;
import io.cloudslang.lang.entities.ParallelLoopStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Date: 3/25/2015
 *
 * @author Bonczidai Levente
 */
public abstract class AbstractForTransformer extends AbstractInOutForTransformer {

    @Autowired
    private ExecutableValidator executableValidator;

    // case: value in variable_name
    private static final String FOR_REGEX = "^(\\s+)?(\\w+)\\s+(in)\\s+(\\w+)(\\s+)?$";

    // case: key, value
    private static final String KEY_VALUE_PAIR_REGEX = "^(\\s+)?(\\w+)(\\s+)?(,)(\\s+)?(\\w+)(\\s+)?$";

    private static final String FOR_IN_KEYWORD = " in ";

    public TransformModellingResult<LoopStatement> transformToLoopStatement(String rawData, boolean isParallelLoop) {
        List<RuntimeException> errors = new ArrayList<>();
        Accumulator dependencyAccumulator = extractFunctionData("${" + rawData + "}");
        if (StringUtils.isEmpty(rawData)) {
            return new BasicTransformModellingResult<>(null, errors);
        }
        LoopStatement loopStatement = null;
        String varName;
        String collectionExpression;
        Pattern regexSimpleFor = Pattern.compile(FOR_REGEX);
        Matcher matcherSimpleFor = regexSimpleFor.matcher(rawData);
        try {
            if (matcherSimpleFor.find()) {
                varName = matcherSimpleFor.group(2);
                collectionExpression = matcherSimpleFor.group(4);
                loopStatement = createLoopStatement(varName, collectionExpression, dependencyAccumulator, isParallelLoop);
            } else {
                String beforeInKeyword = StringUtils.substringBefore(rawData, FOR_IN_KEYWORD);
                collectionExpression = StringUtils.substringAfter(rawData, FOR_IN_KEYWORD).trim();
                Pattern regexKeyValueFor = Pattern.compile(KEY_VALUE_PAIR_REGEX);
                Matcher matcherKeyValueFor = regexKeyValueFor.matcher(beforeInKeyword);
                if (matcherKeyValueFor.find()) {
                    String keyName = matcherKeyValueFor.group(2);
                    String valueName = matcherKeyValueFor.group(6);
                    loopStatement = createMapForLoopStatement(keyName, valueName, collectionExpression, dependencyAccumulator);
                } else {
                    varName = beforeInKeyword.trim();
                    loopStatement = createLoopStatement(varName, collectionExpression, dependencyAccumulator, isParallelLoop);
                }
            }
        } catch (RuntimeException rex) {
            errors.add(rex);
        }
        return new BasicTransformModellingResult<>(loopStatement, errors);
    }

    private LoopStatement createMapForLoopStatement(String keyName, String valueName, String collectionExpression, Accumulator dependencyAccumulator) {
        validateChars(keyName);
        validateChars(valueName);
        return new MapForLoopStatement(keyName, valueName, collectionExpression, dependencyAccumulator.getFunctionDependencies(), dependencyAccumulator.getSystemPropertyDependencies());
    }

    private LoopStatement createLoopStatement(String varName, String collectionExpression, Accumulator dependencyAccumulator, boolean isParallelLoop) {
        validateChars(varName);
        LoopStatement loopStatement;
        if (isParallelLoop) {
            loopStatement = new ParallelLoopStatement(varName, collectionExpression, dependencyAccumulator.getFunctionDependencies(), dependencyAccumulator.getSystemPropertyDependencies());
        } else {
            loopStatement = new ListForLoopStatement(varName, collectionExpression, dependencyAccumulator.getFunctionDependencies(), dependencyAccumulator.getSystemPropertyDependencies());
        }
        return loopStatement;
    }

    private void validateChars(String name) {
        executableValidator.validateLoopStatementVariable(name);
    }
}
