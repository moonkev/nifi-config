package com.github.hermannpencole.nifi.config.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.model.GroupProcessorsEntity;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.ControllerApi;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Class that offer service for nifi processor
 * <p>
 * Created by SFRJ on 01/04/2017.
 */
@Singleton
public class ExtractProcessorService {


    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(ExtractProcessorService.class);

    @Inject
    private ProcessGroupService processGroupService;

    @Inject
    private ControllerApi controllerApi;

    @Inject
    private FlowApi flowapi;

    /**
     *
     * @param branch
     * @param fileConfiguration
     * @throws IOException
     * @throws ApiException
     */
    public void extractByBranch(List<String> branch, String fileConfiguration) throws IOException, ApiException {
        File file = new File(fileConfiguration);

        ProcessGroupFlowEntity componentSearch = processGroupService.changeDirectory(branch)
                .orElseThrow(() -> new ConfigException(("cannot find " + Arrays.toString(branch.toArray()))));

        //add group processors and processors
        GroupProcessorsEntity result = extractJsonFromComponent(componentSearch);

        if (fileConfiguration.endsWith(".json")) {
            //convert to json
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            LOG.debug("saving in file {}", fileConfiguration);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
                gson.toJson(result, writer);
            } finally {
                LOG.debug("extractByBranch end");
            }
        } else {
            //convert to yaml
            YAMLFactory yamlFactory = new YAMLFactory();
            yamlFactory.configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false);
            ObjectMapper mapper = new ObjectMapper(yamlFactory);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            try (SequenceWriter writer = mapper.writerWithDefaultPrettyPrinter().writeValues(file)) {
                writer.write(result);
            }
        }
    }

    /**
     * extract from component
     *
     * @param idComponent
     * @return
     * @throws ApiException
     */
    private GroupProcessorsEntity extractJsonFromComponent(ProcessGroupFlowEntity idComponent) throws ApiException {
        GroupProcessorsEntity result = new GroupProcessorsEntity();
        ProcessGroupFlowDTO processGroupFlow = idComponent.getProcessGroupFlow();
        result.setName(processGroupFlow.getBreadcrumb().getBreadcrumb().getName());
        processGroupFlow.getFlow().getProcessors()
                .forEach(processor -> result.getProcessors().add(extractProcessor(processor.getComponent())));
        for (ProcessGroupEntity processGroups : processGroupFlow.getFlow().getProcessGroups()) {
            result.getProcessGroups().add(extractJsonFromComponent(flowapi.getFlow(processGroups.getId())));
        }
        if (result.getProcessGroups().isEmpty()) {
            result.setProcessGroups(null);
        }
        if (result.getProcessors().isEmpty()) {
            result.setProcessors(null);
        }
        //add controllers
        ControllerServicesEntity controllerServicesEntity = flowapi.getControllerServicesFromGroup(idComponent.getProcessGroupFlow().getId());
        if (!controllerServicesEntity.getControllerServices().isEmpty() ) {
            result.setControllerServices(new ArrayList<>());
        }
        for (ControllerServiceEntity controllerServiceEntity : controllerServicesEntity.getControllerServices()) {
            if (controllerServiceEntity.getComponent().getParentGroupId().equals(idComponent.getProcessGroupFlow().getId())) {
                result.getControllerServices().add(extractController(controllerServiceEntity));
            }
        }
        return result;
    }

    /**
     * extract processor configuration
     *
     * @param processor
     * @return
     */
    private ProcessorDTO extractProcessor(ProcessorDTO processor) {
        ProcessorDTO result = new ProcessorDTO();
        result.setName(processor.getName());
        result.setConfig(processor.getConfig());
        result.setBundle(processor.getBundle());
        //remove controller link
        for ( Map.Entry<String, PropertyDescriptorDTO> entry : processor.getConfig().getDescriptors().entrySet()) {
            if (entry.getValue().getIdentifiesControllerService() != null) {
                result.getConfig().getProperties().remove(entry.getKey());
            }
        }
        result.getConfig().setAutoTerminatedRelationships(null);
        result.getConfig().setDescriptors(null);
        result.getConfig().setDefaultConcurrentTasks(null);
        result.getConfig().setDefaultSchedulingPeriod(null);
        result.setRelationships(null);
        result.setStyle(null);
        result.setSupportsBatching(null);
        result.setSupportsEventDriven(null);
        result.setSupportsParallelProcessing(null);
        result.setPersistsState(null);
        result.setRestricted(null);
        result.setValidationErrors(null);

        return result;
    }

    private ControllerServiceDTO extractController(ControllerServiceEntity controllerServiceEntity) {
        ControllerServiceDTO result = new ControllerServiceDTO();

        result.setBundle(controllerServiceEntity.getComponent().getBundle());
        result.setName(controllerServiceEntity.getComponent().getName());
        result.setProperties(controllerServiceEntity.getComponent().getProperties());
        result.setPersistsState(null);
        result.setRestricted(null);
        result.setDescriptors(null);
        result.setReferencingComponents(null);
        result.setValidationErrors(null);
        return result;
    }
}
