package io.cloudslang.lang.compiler.modeller.transformers;

import io.cloudslang.lang.compiler.modeller.result.BasicTransformModellingResult;
import io.cloudslang.lang.compiler.modeller.result.TransformModellingResult;
import io.cloudslang.lang.compiler.validator.ExecutableValidator;
import io.cloudslang.lang.compiler.validator.PreCompileValidator;
import io.cloudslang.lang.entities.ScoreLangConstants;
import io.cloudslang.lang.entities.bindings.InOutParam;
import io.cloudslang.lang.entities.bindings.Output;
import io.cloudslang.lang.entities.bindings.values.ValueFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * User: stoneo
 * Date: 12/11/2014
 * Time: 11:33
 */
public abstract class AbstractOutputsTransformer extends InOutTransformer {

    @Autowired
    private PreCompileValidator preCompileValidator;

    @Autowired
    private ExecutableValidator executableValidator;

    public TransformModellingResult<List<Output>> transform(List<Object> rawData) {
        List<Output> transformedData = new ArrayList<>();
        List<RuntimeException> errors = new ArrayList<>();
        if (CollectionUtils.isEmpty(rawData)) {
            return new BasicTransformModellingResult<>(transformedData, errors);
        }
        for (Object rawOutput : rawData) {
            try {
                if (rawOutput instanceof Map) {
                    @SuppressWarnings(value = { "unchecked" }) Map.Entry<String, ?> entry = ((Map<String, ?>) rawOutput).entrySet().iterator().next();
                    Serializable entryValue = (Serializable) entry.getValue();
                    if (entryValue == null) {
                        throw new RuntimeException("Could not transform Output : " + rawOutput + " since it has a null value.\n\nMake sure a value is specified or that indentation is properly done.");
                    }
                    if (entryValue instanceof Map) {
                        handleOutputProperties(transformedData, entry, errors);
                    } else {
                        addOutput(transformedData, createOutput(entry.getKey(), entryValue, false), errors);
                    }
                } else {
                    addOutput(transformedData, createRefOutput((String) rawOutput, false), errors);
                }
            } catch (RuntimeException rex) {
                errors.add(rex);
            }
        }
        return new BasicTransformModellingResult<>(transformedData, errors);
    }

    @Override
    public Class<? extends InOutParam> getTransformedObjectsClass() {
        return Output.class;
    }

    abstract void handleOutputProperties(List<Output> transformedData, Map.Entry<String, ?> entry, List<RuntimeException> errors);

    void addOutput(List<Output> outputs, Output element, List<RuntimeException> errors) {
        List<RuntimeException> validationErrors = preCompileValidator.validateNoDuplicateInOutParams(outputs, element);
        if (CollectionUtils.isEmpty(validationErrors)) {
            outputs.add(element);
        } else {
            errors.addAll(validationErrors);
        }
    }

    Output createOutput(String outputName, Serializable outputExpression, boolean sensitive) {
        executableValidator.validateOutputName(outputName);
        preCompileValidator.validateStringValue(outputName, outputExpression, this);
        Accumulator accumulator = extractFunctionData(outputExpression);
        return new Output(outputName, ValueFactory.create(outputExpression, sensitive), accumulator.getFunctionDependencies(), accumulator.getSystemPropertyDependencies());
    }

    Output createRefOutput(String rawOutput, boolean sensitive) {
        return createOutput(rawOutput, transformNameToExpression(rawOutput), sensitive);
    }

    private String transformNameToExpression(String name) {
        return ScoreLangConstants.EXPRESSION_START_DELIMITER + name + ScoreLangConstants.EXPRESSION_END_DELIMITER;
    }
}
