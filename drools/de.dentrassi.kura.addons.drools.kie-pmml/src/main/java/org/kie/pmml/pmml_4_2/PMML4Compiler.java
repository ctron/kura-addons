/*
 * Copyright 2011 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.pmml.pmml_4_2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.dmg.pmml.pmml_4_2.descr.ClusteringModel;
import org.dmg.pmml.pmml_4_2.descr.DataDictionary;
import org.dmg.pmml.pmml_4_2.descr.NaiveBayesModel;
import org.dmg.pmml.pmml_4_2.descr.NeuralNetwork;
import org.dmg.pmml.pmml_4_2.descr.PMML;
import org.dmg.pmml.pmml_4_2.descr.RegressionModel;
import org.dmg.pmml.pmml_4_2.descr.Scorecard;
import org.dmg.pmml.pmml_4_2.descr.SupportVectorMachineModel;
import org.dmg.pmml.pmml_4_2.descr.TreeModel;
import org.drools.compiler.compiler.PMMLCompiler;
import org.drools.compiler.compiler.PMMLResource;
import org.drools.core.io.impl.ByteArrayResource;
import org.drools.core.io.impl.ClassPathResource;
import org.drools.core.util.IoUtils;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.builder.model.KieSessionModel;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.builder.KnowledgeBuilderResult;
import org.kie.internal.io.ResourceFactory;
import org.kie.pmml.pmml_4_2.model.Miningmodel;
import org.kie.pmml.pmml_4_2.model.PMML4UnitImpl;
import org.kie.pmml.pmml_4_2.model.mining.MiningSegment;
import org.kie.pmml.pmml_4_2.model.mining.MiningSegmentation;
import org.mvel2.templates.SimpleTemplateRegistry;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRegistry;
import org.xml.sax.SAXException;

public class PMML4Compiler implements PMMLCompiler {

    static {
        // FIXME: jreimann - added warning
        System.err.println("WARNING: Running with patched version of PMML4Compiler");
    }

    public static final String PMML_NAMESPACE = "org.dmg.pmml.pmml_4_2"; // FIXME: jreimann - fixed namespace
    public static final String PMML_DROOLS = "org.kie.pmml.pmml_4_2";
    public static final String PMML = PMML_NAMESPACE + ".descr";
    public static final String SCHEMA_PATH = "xsd/org/dmg/pmml/pmml_4_2/pmml-4-2.xsd";
    public static final String BASE_PACK = PMML_DROOLS.replace('.', '/');

    protected static boolean globalLoaded = false;
    protected static final String[] GLOBAL_TEMPLATES = new String[] {
            "global/pmml_header.drlt",
            "global/pmml_import.drlt",
            "global/rule_meta.drlt",
            "global/modelMark.drlt",

            "global/dataDefinition/common.drlt",
            "global/dataDefinition/rootDataField.drlt",
            "global/dataDefinition/inputBinding.drlt",
            "global/dataDefinition/outputBinding.drlt",
            "global/dataDefinition/ioTypeDeclare.drlt",
            "global/dataDefinition/updateIOField.drlt",
            "global/dataDefinition/inputFromEP.drlt",
            "global/dataDefinition/inputBean.drlt",
            "global/dataDefinition/outputBean.drlt",

            "global/manipulation/confirm.drlt",
            "global/manipulation/mapMissingValues.drlt",
            "global/manipulation/propagateMissingValues.drlt",

            "global/validation/intervalsOnDomainRestriction.drlt",
            "global/validation/valuesNoRestriction.drlt",
            "global/validation/valuesOnDomainRestriction.drlt",
            "global/validation/valuesOnDomainRestrictionMissing.drlt",
            "global/validation/valuesOnDomainRestrictionInvalid.drlt",
    };

    protected static boolean transformationLoaded = false;
    protected static final String[] TRANSFORMATION_TEMPLATES = new String[] {
            "transformations/normContinuous/boundedLowerOutliers.drlt",
            "transformations/normContinuous/boundedUpperOutliers.drlt",
            "transformations/normContinuous/normContOutliersAsMissing.drlt",
            "transformations/normContinuous/linearTractNormalization.drlt",
            "transformations/normContinuous/lowerExtrapolateLinearTractNormalization.drlt",
            "transformations/normContinuous/upperExtrapolateLinearTractNormalization.drlt",

            "transformations/aggregate/aggregate.drlt",
            "transformations/aggregate/collect.drlt",

            "transformations/simple/constantField.drlt",
            "transformations/simple/aliasedField.drlt",

            "transformations/normDiscrete/indicatorFieldYes.drlt",
            "transformations/normDiscrete/indicatorFieldNo.drlt",
            "transformations/normDiscrete/predicateField.drlt",

            "transformations/discretize/intervalBinning.drlt",
            "transformations/discretize/outOfBinningDefault.drlt",
            "transformations/discretize/outOfBinningMissing.drlt",

            "transformations/mapping/mapping.drlt",

            "transformations/functions/apply.drlt",
            "transformations/functions/function.drlt"
    };

    protected static boolean miningLoaded = false;
    protected static final String[] MINING_TEMPLATES = new String[] {
            "models/common/mining/miningField.drlt",
            "models/common/mining/miningFieldInvalid.drlt",
            "models/common/mining/miningFieldMissing.drlt",
            "models/common/mining/miningFieldOutlierAsMissing.drlt",
            "models/common/mining/miningFieldOutlierAsExtremeLow.drlt",
            "models/common/mining/miningFieldOutlierAsExtremeUpp.drlt",

            "models/common/targets/targetReshape.drlt",
            "models/common/targets/aliasedOutput.drlt",
            "models/common/targets/addOutputFeature.drlt",
            "models/common/targets/addRelOutputFeature.drlt",
            "models/common/targets/outputQuery.drlt",
            "models/common/targets/outputQueryPredicate.drlt"
    };

    protected static boolean neuralLoaded = false;
    protected static final String[] NEURAL_TEMPLATES = new String[] {
            "models/neural/neuralBeans.drlt",
            "models/neural/neuralWireInput.drlt",
            "models/neural/neuralBuildSynapses.drlt",
            "models/neural/neuralBuildNeurons.drlt",
            "models/neural/neuralLinkSynapses.drlt",
            "models/neural/neuralFire.drlt",
            "models/neural/neuralLayerMaxNormalization.drlt",
            "models/neural/neuralLayerSoftMaxNormalization.drlt",
            "models/neural/neuralOutputField.drlt",
            "models/neural/neuralClean.drlt"
    };

    protected static boolean svmLoaded = false;
    protected static final String[] SVM_TEMPLATES = new String[] {
            "models/svm/svmParams.drlt",
            "models/svm/svmDeclare.drlt",
            "models/svm/svmFunctions.drlt",
            "models/svm/svmBuild.drlt",
            "models/svm/svmInitSupportVector.drlt",
            "models/svm/svmInitInputVector.drlt",
            "models/svm/svmKernelEval.drlt",
            "models/svm/svmOutputGeneration.drlt",
            "models/svm/svmOutputVoteDeclare.drlt",
            "models/svm/svmOutputVote1vN.drlt",
            "models/svm/svmOutputVote1v1.drlt",
    };

    protected static boolean naiveBayesLoaded = false;
    protected static final String[] NAIVE_BAYES_TEMPLATES = new String[] {
            "models/bayes/naiveBayesDeclare.drlt",
            "models/bayes/naiveBayesEvalDiscrete.drlt",
            "models/bayes/naiveBayesEvalContinuous.drlt",
            "models/bayes/naiveBayesBuildCounts.drlt",
            "models/bayes/naiveBayesBuildDistrs.drlt",
            "models/bayes/naiveBayesBuildOuts.drlt",
    };

    protected static boolean simpleRegLoaded = false;
    protected static final String[] SIMPLEREG_TEMPLATES = new String[] {
            "models/regression/regDeclare.drlt",
            "models/regression/regCommon.drlt",
            "models/regression/regParams.drlt",
            "models/regression/regEval.drlt",
            "models/regression/regClaxOutput.drlt",
            "models/regression/regNormalization.drlt",
            "models/regression/regDecumulation.drlt",

    };

    protected static boolean clusteringLoaded = false;
    protected static final String[] CLUSTERING_TEMPLATES = new String[] {
            "models/clustering/clusteringDeclare.drlt",
            "models/clustering/clusteringInit.drlt",
            "models/clustering/clusteringEvalDistance.drlt",
            "models/clustering/clusteringEvalSimilarity.drlt",
            "models/clustering/clusteringMatrixCompare.drlt"
    };

    protected static boolean treeLoaded = false;
    protected static final String[] TREE_TEMPLATES = new String[] {
            "models/tree/treeDeclare.drlt",
            "models/tree/treeCommon.drlt",
            "models/tree/treeInputDeclare.drlt",
            "models/tree/treeInit.drlt",
            "models/tree/treeAggregateEval.drlt",
            "models/tree/treeDefaultEval.drlt",
            "models/tree/treeEval.drlt",
            "models/tree/treeIOBinding.drlt",
            "models/tree/treeMissHandleAggregate.drlt",
            "models/tree/treeMissHandleWeighted.drlt",
            "models/tree/treeMissHandleLast.drlt",
            "models/tree/treeMissHandleNull.drlt",
            "models/tree/treeMissHandleNone.drlt"
    };

    protected static boolean scorecardLoaded = false;
    protected static final String[] SCORECARD_TEMPLATES = new String[] {
            "models/scorecard/scorecardInit.drlt",
            "models/scorecard/scorecardParamsInit.drlt",
            "models/scorecard/scorecardDeclare.drlt",
            "models/scorecard/scorecardDataDeclare.drlt",
            "models/scorecard/scorecardPartialScore.drlt",
            "models/scorecard/scorecardScoring.drlt",
            "models/scorecard/scorecardOutputGeneration.drlt",
            "models/scorecard/scorecardOutputRankCode.drlt",
            "mvel/scorecard/complexPartialScore.mvel"
    };

    protected static final String RESOURCE_PATH = BASE_PACK;
    protected static final String TEMPLATE_PATH = "/" + RESOURCE_PATH + "/templates/";

    private static TemplateRegistry registry;

    private static List<KnowledgeBuilderResult> visitorBuildResults = new ArrayList<>();
    private List<KnowledgeBuilderResult> results;
    private Schema schema;

    private final PMML4Helper helper;

    public PMML4Compiler() {
        super();
        this.results = new ArrayList<>();
        this.helper = new PMML4Helper();
        this.helper.setPack("org.kie.pmml.pmml_4_2.test");

        final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        try {
            this.schema = sf.newSchema(Thread.currentThread().getContextClassLoader().getResource(SCHEMA_PATH));
        } catch (final SAXException e) {
            e.printStackTrace();
        }

    }

    public PMML4Helper getHelper() {
        return this.helper;
    }

    private String getRuleUnitClass(final PMML4Unit unit) {
        final PMML4Model root = unit.getRootModel();
        return root.getRuleUnitClassName();
    }

    public String generateTheory(final PMML pmml) {
        final StringBuilder sb = new StringBuilder();
        final PMML4Unit unit = new PMML4UnitImpl(pmml);

        KieBase visitor;
        try {
            visitor = checkBuildingResources(pmml);
        } catch (final IOException e) {
            this.results.add(new PMMLError(e.getMessage()));
            return null;
        }

        final KieSession visitorSession = visitor.newKieSession();

        this.helper.reset();
        visitorSession.setGlobal("registry", registry);
        visitorSession.setGlobal("fld2var", new HashMap());
        visitorSession.setGlobal("utils", this.helper);
        visitorSession.setGlobal("unitPackageName", this.helper.getPack());
        visitorSession.setGlobal("ruleUnitClassName", getRuleUnitClass(unit));

        visitorSession.setGlobal("theory", sb);

        visitorSession.insert(pmml);

        visitorSession.fireAllRules();

        final String modelEvaluatingRules = sb.toString();

        visitorSession.dispose();

        return modelEvaluatingRules;
    }

    private static void initRegistry() {
        if (registry == null) {
            registry = new SimpleTemplateRegistry();
        }

        if (!globalLoaded) {
            for (final String ntempl : GLOBAL_TEMPLATES) {
                prepareTemplate(ntempl);
            }
            globalLoaded = true;
        }

        if (!transformationLoaded) {
            for (final String ntempl : TRANSFORMATION_TEMPLATES) {
                prepareTemplate(ntempl);
            }
            transformationLoaded = true;
        }

        if (!miningLoaded) {
            for (final String ntempl : MINING_TEMPLATES) {
                prepareTemplate(ntempl);
            }
            miningLoaded = true;
        }
    }

    private static KieBase checkBuildingResources(final PMML pmml) throws IOException {

        final KieServices ks = KieServices.Factory.get();
        // FIXME: jreimann - add ClassLoader
        final KieContainer kieContainer = ks.getKieClasspathContainer(PMML4Compiler.class.getClassLoader());

        if (registry == null) {
            initRegistry();
        }

        String chosenKieBase = null;

        for (final Object o : pmml.getAssociationModelsAndBaselineModelsAndClusteringModels()) {

            if (o instanceof NaiveBayesModel) {
                if (!naiveBayesLoaded) {
                    for (final String ntempl : NAIVE_BAYES_TEMPLATES) {
                        prepareTemplate(ntempl);
                    }
                    naiveBayesLoaded = true;
                }
                chosenKieBase = chosenKieBase == null ? "KiePMML-Bayes" : "KiePMML";
            }

            if (o instanceof NeuralNetwork) {
                if (!neuralLoaded) {
                    for (final String ntempl : NEURAL_TEMPLATES) {
                        prepareTemplate(ntempl);
                    }
                    neuralLoaded = true;
                }
                chosenKieBase = chosenKieBase == null ? "KiePMML-Neural" : "KiePMML";
            }

            if (o instanceof ClusteringModel) {
                if (!clusteringLoaded) {
                    for (final String ntempl : CLUSTERING_TEMPLATES) {
                        prepareTemplate(ntempl);
                    }
                    clusteringLoaded = true;
                }
                chosenKieBase = chosenKieBase == null ? "KiePMML-Cluster" : "KiePMML";
            }

            if (o instanceof SupportVectorMachineModel) {
                if (!svmLoaded) {
                    for (final String ntempl : SVM_TEMPLATES) {
                        prepareTemplate(ntempl);
                    }
                    svmLoaded = true;
                }
                chosenKieBase = chosenKieBase == null ? "KiePMML-SVM" : "KiePMML";
            }

            if (o instanceof TreeModel) {
                if (!treeLoaded) {
                    for (final String ntempl : TREE_TEMPLATES) {
                        prepareTemplate(ntempl);
                    }
                    treeLoaded = true;
                }
                chosenKieBase = chosenKieBase == null ? "KiePMML-Tree" : "KiePMML";
            }

            if (o instanceof RegressionModel) {
                if (!simpleRegLoaded) {
                    for (final String ntempl : SIMPLEREG_TEMPLATES) {
                        prepareTemplate(ntempl);
                    }
                    simpleRegLoaded = true;
                }
                chosenKieBase = chosenKieBase == null ? "KiePMML-Regression" : "KiePMML";
            }

            if (o instanceof Scorecard) {
                if (!scorecardLoaded) {
                    for (final String ntempl : SCORECARD_TEMPLATES) {
                        prepareTemplate(ntempl);
                    }
                    scorecardLoaded = true;
                }
                chosenKieBase = chosenKieBase == null ? "KiePMML-Scorecard" : "KiePMML";
            }
        }

        if (chosenKieBase == null) {
            chosenKieBase = "KiePMML-Base";
        }
        return kieContainer.getKieBase(chosenKieBase);
    }

    private static void prepareTemplate(final String ntempl) {
        try {
            final String path = TEMPLATE_PATH + ntempl;
            final Resource res = ResourceFactory.newClassPathResource(path, org.kie.pmml.pmml_4_2.PMML4Compiler.class);
            if (res != null) {
                final InputStream stream = res.getInputStream();
                if (stream != null) {
                    registry.addNamedTemplate(path.substring(path.lastIndexOf('/') + 1),
                            TemplateCompiler.compileTemplate(stream));
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public String compile(final String resource, final ClassLoader classLoader) {
        String theory = null;
        final Resource cpr = new ClassPathResource(resource);
        try {
            theory = compile(cpr.getInputStream(), classLoader);
        } catch (final IOException e) {
            this.results.add(new PMMLError(e.toString()));
            e.printStackTrace();
        }
        return theory;
    }

    public Resource[] transform(final Resource resource, final ClassLoader classLoader) {
        String theory = null;
        try {
            theory = compile(resource.getInputStream(), classLoader);
        } catch (final IOException e) {
            this.results.add(new PMMLError(e.toString()));
            e.printStackTrace();
            return new Resource[0];
        }
        return new Resource[] { buildOutputResource(resource, theory) };
    }

    private Resource buildOutputResource(final Resource resource, final String theory) {
        final ByteArrayResource byteArrayResource = new ByteArrayResource(theory.getBytes(IoUtils.UTF8_CHARSET));
        byteArrayResource.setResourceType(ResourceType.PMML);

        if (resource.getSourcePath() != null) {
            final String originalPath = resource.getSourcePath();
            final int start = originalPath.lastIndexOf(File.separator);
            byteArrayResource.setSourcePath("generated-sources/" + originalPath.substring(start) + ".pmml");
        } else {
            byteArrayResource.setSourcePath("generated-sources/" + this.helper.getContext() + ".pmml");
        }
        return byteArrayResource;
    }

    private InputStream getInputStreamByFileName(final String fileName) {
        InputStream is = null;
        Resource res = ResourceFactory.newClassPathResource(fileName);
        try {
            is = res.getInputStream();
        } catch (final Exception e) {
        }
        if (is == null) {
            res = ResourceFactory.newFileResource(fileName);
        }
        try {
            is = res.getInputStream();
        } catch (final Exception e) {
            this.results.add(new PMMLError("Unable to retrieve file based resource: " + fileName));
        }
        return is;
    }

    @Override
    public Map<String, String> getJavaClasses(final String fileName) {
        final InputStream is = getInputStreamByFileName(fileName);
        if (is != null) {
            return getJavaClasses(is);
        }
        return new HashMap<>();
    }

    @Override
    public Map<String, String> getJavaClasses(final InputStream stream) {
        final Map<String, String> javaClasses = new HashMap<>();
        final PMML pmml = loadModel(PMML, stream);
        if (pmml != null) {
            final PMML4Unit unit = new PMML4UnitImpl(pmml);
            if (unit != null) {
                final List<PMML4Model> models = unit.getModels();
                models.forEach(model -> {
                    final Map.Entry<String, String> inputPojo = model.getMappedMiningPojo();
                    final Map.Entry<String, String> ruleUnit = model.getMappedRuleUnit();
                    if (inputPojo != null) {
                        javaClasses.put(inputPojo.getKey(), inputPojo.getValue());
                    }
                    if (ruleUnit != null) {
                        javaClasses.put(ruleUnit.getKey(), ruleUnit.getValue());
                    }
                });
            }
        }
        return javaClasses;
    }

    @Override
    public List<PMMLResource> precompile(final String fileName, final ClassLoader classLoader,
            final KieBaseModel rootKieBaseModel) {
        final InputStream is = getInputStreamByFileName(fileName);
        List<PMMLResource> resources = null;
        if (is != null) {
            try {
                resources = precompile(is, classLoader, rootKieBaseModel);
            } catch (final Exception e) {
                final PMMLError err = new PMMLError(
                        "Unable to retrieve pre-compiled resources for PMML: " + e.getMessage());
                this.results.add(err);
            }
        }
        return resources != null ? resources : Collections.emptyList();
    }

    @Override
    public List<PMMLResource> precompile(final InputStream stream, final ClassLoader classLoader,
            final KieBaseModel rootKieBaseModel) {
        List<PMMLResource> resources = new ArrayList<>();
        final KieServices services = KieServices.Factory.get();
        final KieModuleModel module = services.newKieModuleModel();
        this.results = new ArrayList<>();
        final PMML pmml = loadModel(PMML, stream);
        this.helper.setResolver(classLoader);
        final PMML4Unit unit = new PMML4UnitImpl(pmml);
        if (unit.containsMiningModel()) {
            final Miningmodel rootModel = unit.getRootMiningModel();
            resources = buildResourcesFromModel(pmml, rootModel, null, classLoader, module);
        } else {
            final PMML4Model rootModel = unit.getRootModel();
            if (rootModel != null) {
                this.helper.setPack(rootModel.getModelPackageName());
                final KieBaseModel kbm = module.newKieBaseModel(rootModel.getModelId());
                kbm.addPackage(this.helper.getPack())
                        .setDefault(true)
                        .setEventProcessingMode(EventProcessingOption.CLOUD);
                final PMMLResource resource = new PMMLResource(this.helper.getPack());
                resource.setKieBaseModel(kbm);
                resource.addRules(rootModel.getModelId(), this.compile(pmml, classLoader));
                resources.add(resource);
            }
        }
        return resources;
    }

    protected PMMLResource buildResourceFromSegment(final PMML pmml_origin, final MiningSegment segment,
            final ClassLoader classLoader, final KieModuleModel module) {
        final PMML pmml = new PMML();
        final DataDictionary dd = pmml_origin.getDataDictionary();
        pmml.setDataDictionary(dd);
        pmml.setHeader(pmml_origin.getHeader());
        pmml.getAssociationModelsAndBaselineModelsAndClusteringModels().add(segment.getModel().getRawModel());
        this.helper.setPack(segment.getModel().getModelPackageName());// PMML4Helper.pmmlDefaultPackageName()+".mining.segment_"+segment.getSegmentId());

        final String rules = this.compile(pmml, classLoader);
        final KieBaseModel kbModel = module.newKieBaseModel(segment.getOwner().getOwner().getModelId() + "_"
                + segment.getOwner().getSegmentationId() + "_SEGMENT_" + segment.getSegmentId());
        kbModel.addPackage(this.helper.getPack())
                .setDefault(false)
                .setEventProcessingMode(EventProcessingOption.CLOUD);
        final KieSessionModel ksm = kbModel.newKieSessionModel("SEGMENT_" + segment.getSegmentId());
        ksm.setDefault(true);
        final PMMLResource resource = new PMMLResource(this.helper.getPack());
        resource.setKieBaseModel(kbModel);
        resource.addRules(segment.getModel().getModelId(), rules);
        return resource;
    }

    protected List<PMMLResource> buildResourcesFromModel(final PMML pmml, final Miningmodel miningModel,
            List<PMMLResource> resourcesList, final ClassLoader classLoader, final KieModuleModel module) {
        if (resourcesList == null) {
            resourcesList = new ArrayList<>();
        }
        final PMMLResource resource = new PMMLResource(miningModel.getModelPackageName());// new
                                                                                          // PMMLResource(PMML_DROOLS+".mining.model_"+miningModel.getModelId());
        final KieBaseModel rootKieBaseModel = module.newKieBaseModel(resource.getPackageName());
        rootKieBaseModel.addPackage(resource.getPackageName());
        rootKieBaseModel.setDefault(true);
        resource.setKieBaseModel(rootKieBaseModel);
        resource.addRules(miningModel.getModelId(), miningModel.generateRules());
        resourcesList.add(resource);
        getChildResources(pmml, miningModel, resourcesList, classLoader, module);
        return resourcesList;
    }

    protected List<PMMLResource> getChildResources(final PMML pmml_origin, final Miningmodel parent,
            final List<PMMLResource> resourceList, final ClassLoader classLoader, final KieModuleModel module) {
        if (parent != null && parent.getSegmentation() != null) {
            final MiningSegmentation segmentation = parent.getSegmentation();
            if (segmentation.getMiningSegments() != null) {
                final List<MiningSegment> segments = segmentation.getMiningSegments();
                for (final MiningSegment segment : segments) {
                    if (segment.getModel() instanceof Miningmodel) {
                        buildResourcesFromModel(pmml_origin, (Miningmodel) segment.getModel(), resourceList,
                                classLoader, module);
                    } else {
                        resourceList.add(buildResourceFromSegment(pmml_origin, segment, classLoader, module));
                    }
                }
            }
        }
        return resourceList;
    }

    public String compile(final PMML pmml, final ClassLoader classLoader) {
        this.helper.setResolver(classLoader);

        if (getResults().isEmpty()) {
            return generateTheory(pmml);
        } else {
            return null;
        }

    }

    @Override
    public String compile(final InputStream source, final ClassLoader classLoader) {
        this.results = new ArrayList<>();
        final PMML pmml = loadModel(PMML, source);
        return compile(pmml, classLoader);
    }

    @Override
    public List<KnowledgeBuilderResult> getResults() {
        final List<KnowledgeBuilderResult> combinedResults = new ArrayList<>(this.results);
        combinedResults.addAll(visitorBuildResults);
        return combinedResults;
    }

    @Override
    public void clearResults() {
        this.results.clear();
    }

    public void dump(final String s, final OutputStream ostream) {
        // write to outstream
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(ostream, "UTF-8");
            writer.write(s);
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.flush();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Imports a PMML source file, returning a Java descriptor
     * 
     * @param model
     *            the PMML package name (classes derived from a specific schema)
     * @param source
     *            the name of the PMML resource storing the predictive model
     * @return the Java Descriptor of the PMML resource
     */
    public PMML loadModel(final String model, final InputStream source) {
        try {
            if (this.schema == null) {
                visitorBuildResults.add(new PMMLWarning(ResourceFactory.newInputStreamResource(source),
                        "Could not validate PMML document, schema not available"));
            }
            // FIXME: jreimann - BEGIN: set context class loader for JAXB
            final JAXBContext jc;
            final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(PMML4Compiler.class.getClassLoader());
                jc = JAXBContext.newInstance(model, PMML4Compiler.class.getClassLoader());
            } finally {
                Thread.currentThread().setContextClassLoader(ccl);
            }
            // FIXME: jreimann - END: set context class loader for JAXB

            final Unmarshaller unmarshaller = jc.createUnmarshaller();
            if (this.schema != null) {
                unmarshaller.setSchema(this.schema);
            }

            return (PMML) unmarshaller.unmarshal(source);
        } catch (final JAXBException e) {
            this.results.add(new PMMLError(e.toString()));
            return null;
        }

    }

    public static void dumpModel(final PMML model, final OutputStream target) {
        try {
            final JAXBContext jc = JAXBContext.newInstance(PMML.class.getPackage().getName());
            final Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            marshaller.marshal(model, target);
        } catch (final JAXBException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String getCompilerVersion() {
        return "KIE PMML v2";
    }

}
