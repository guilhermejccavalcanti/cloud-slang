package org.openscore.cli.utils;

/*
 * Licensed to Hewlett-Packard Development Company, L.P. under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
*/


import com.google.common.collect.Lists;
import org.openscore.lang.api.Slang;
import com.hp.score.lang.entities.CompilationArtifact;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Date: 11/13/2014
 *
 * @author lesant
 */

@Component
public class CompilerHelperImpl implements CompilerHelper{

    @Autowired
    private Slang slang;

    private final static Logger logger = Logger.getLogger(CompilerHelperImpl.class);

    private String[] SLANG_FILE_EXTENSIONS = {"yml", "yaml", "py","sl"};

    /**
     * @param filePath
     * @param opName
     * @param dependencies
     * @return
     * @throws IOException
     */
    public CompilationArtifact compile(String filePath, String opName, List<String> dependencies) throws IOException {
        Validate.notNull(filePath, "filePath can not be null");

        Set<File> dependenciesFilesSet = new HashSet<>();
        File file = new File(filePath);
        Validate.isTrue(file.isFile(), "filePath must lead to a file");

        if (dependencies == null || dependencies.isEmpty()) {
            dependencies = Lists.newArrayList(file.getParent()); //default behavior is taking the parent dir
        }

        for (String dependency:dependencies) {
            Collection<File> dependenciesFiles = FileUtils.listFiles(new File(dependency), SLANG_FILE_EXTENSIONS, false);
            dependenciesFilesSet.addAll(dependenciesFiles);
        }

        //todo - support compile of op too?
        CompilationArtifact compilationArtifact = null;
        try {
            compilationArtifact = slang.compile(file, dependenciesFilesSet);
        } catch (Exception e) {
            logger.error("Failed compilation for file : "+file.getName() + " ,Exception is : " + e.getMessage());
            throw e;
        }

        return compilationArtifact;
    }

}