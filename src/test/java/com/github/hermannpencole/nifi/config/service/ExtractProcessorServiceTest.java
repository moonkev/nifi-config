package com.github.hermannpencole.nifi.config.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.model.GroupProcessorsEntity;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.FlowApi;
import com.github.hermannpencole.nifi.swagger.client.model.ControllerServicesEntity;
import com.github.hermannpencole.nifi.swagger.client.model.ProcessGroupFlowEntity;
import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
/**
 * API tests for AccessApi
 */
@RunWith(MockitoJUnitRunner.class)
public class ExtractProcessorServiceTest {
    @Mock
    private ProcessGroupService processGroupServiceMock;

    @Mock
    private FlowApi flowapiMock;

    @InjectMocks
    private ExtractProcessorService extractService;

    @Test(expected = ConfigException.class)
    public void extractNotExitingBranchTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");
        File temp = File.createTempFile("tempfile", ".tmp");
        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.empty());
        extractService.extractByBranch(branch, temp.getAbsolutePath());
    }

    @Test(expected = FileNotFoundException.class)
    public void extractErrorFileBranchTest() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");
        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");
        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        when(flowapiMock.getControllerServicesFromGroup("idComponent")).thenReturn(new ControllerServicesEntity());

        File temp = File.createTempFile("tempfile", ".tmp");
        extractService.extractByBranch(branch, temp.getParent());
    }

    @Test
    public void extractEmptyBranchTestJSON() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");
        File temp = File.createTempFile("tempfile", ".json");

        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        when(flowapiMock.getControllerServicesFromGroup("idComponent")).thenReturn(new ControllerServicesEntity());

        extractService.extractByBranch(branch, temp.getAbsolutePath());

        //evaluate response
        Gson gson = new Gson();
        try (Reader reader = new InputStreamReader(new FileInputStream(temp), "UTF-8")) {
            GroupProcessorsEntity result = gson.fromJson(reader, GroupProcessorsEntity.class);
            assertTrue(result.getProcessors().isEmpty());
            assertTrue(result.getProcessGroups().isEmpty());
            assertEquals("nameComponent", result.getName());
        }


    }

    @Test
    public void extractEmptyBranchTestYAML() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");
        File temp = File.createTempFile("tempfile", ".yaml");

        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        when(flowapiMock.getControllerServicesFromGroup("idComponent")).thenReturn(new ControllerServicesEntity());

        extractService.extractByBranch(branch, temp.getAbsolutePath());

        //evaluate response
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (Reader reader = new InputStreamReader(new FileInputStream(temp), "UTF-8")) {
            GroupProcessorsEntity result = mapper.readValue(reader, GroupProcessorsEntity.class);
            assertTrue(result.getProcessors().isEmpty());
            assertTrue(result.getProcessGroups().isEmpty());
            assertEquals("nameComponent", result.getName());
        }
    }

    @Test
    public void extractBranchTestJSON() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");
        File temp = File.createTempFile("tempfile", ".json");

        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");
        response.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc","nameProc") );
        response.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idSubGroup", "nameSubGroup"));

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        ControllerServicesEntity controllerServicesEntity = new ControllerServicesEntity();
        controllerServicesEntity.getControllerServices().add(TestUtils.createControllerServiceEntity("idCtrl", "nameCtrl", "idComponent"));
        when(flowapiMock.getControllerServicesFromGroup("idComponent")).thenReturn(controllerServicesEntity);
        ControllerServicesEntity subGroupControllerServicesEntity = new ControllerServicesEntity();
        when(flowapiMock.getControllerServicesFromGroup("idSubGroup")).thenReturn(subGroupControllerServicesEntity);

        ProcessGroupFlowEntity subGroupResponse = TestUtils.createProcessGroupFlowEntity("idSubGroup", "nameSubGroup");
        when(flowapiMock.getFlow(subGroupResponse.getProcessGroupFlow().getId())).thenReturn(subGroupResponse);

        extractService.extractByBranch(branch, temp.getAbsolutePath());
        Gson gson = new Gson();
        try (Reader reader = new InputStreamReader(new FileInputStream(temp), "UTF-8")) {
            GroupProcessorsEntity result = gson.fromJson(reader, GroupProcessorsEntity.class);
            assertEquals(1, result.getProcessors().size());
            assertEquals("nameProc", result.getProcessors().get(0).getName());
            assertEquals(1,result.getProcessGroups().size());
            assertEquals("nameSubGroup", result.getProcessGroups().get(0).getName());
            assertEquals("nameComponent", result.getName());
            assertEquals(1, result.getControllerServices().size());
            assertEquals("nameCtrl", result.getControllerServices().get(0).getName());
        }
    }

    @Test
    public void extractBranchTestYAML() throws ApiException, IOException, URISyntaxException {
        List<String> branch = Arrays.asList("root", "elt1");
        File temp = File.createTempFile("tempfile", ".yaml");

        ProcessGroupFlowEntity response = TestUtils.createProcessGroupFlowEntity("idComponent", "nameComponent");
        response.getProcessGroupFlow().getFlow()
                .getProcessors().add(TestUtils.createProcessorEntity("idProc","nameProc") );
        response.getProcessGroupFlow().getFlow()
                .getProcessGroups().add(TestUtils.createProcessGroupEntity("idSubGroup", "nameSubGroup"));

        when(processGroupServiceMock.changeDirectory(branch)).thenReturn(Optional.of(response));
        ControllerServicesEntity controllerServicesEntity = new ControllerServicesEntity();
        controllerServicesEntity.getControllerServices().add(TestUtils.createControllerServiceEntity("idCtrl", "nameCtrl", "idComponent"));
        when(flowapiMock.getControllerServicesFromGroup("idComponent")).thenReturn(controllerServicesEntity);
        ControllerServicesEntity subGroupControllerServicesEntity = new ControllerServicesEntity();
        when(flowapiMock.getControllerServicesFromGroup("idSubGroup")).thenReturn(subGroupControllerServicesEntity);

        ProcessGroupFlowEntity subGroupResponse = TestUtils.createProcessGroupFlowEntity("idSubGroup", "nameSubGroup");
        when(flowapiMock.getFlow(subGroupResponse.getProcessGroupFlow().getId())).thenReturn(subGroupResponse);

        extractService.extractByBranch(branch, temp.getAbsolutePath());
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (Reader reader = new InputStreamReader(new FileInputStream(temp), "UTF-8")) {
            GroupProcessorsEntity result = mapper.readValue(reader, GroupProcessorsEntity.class);
            assertEquals(1, result.getProcessors().size());
            assertEquals("nameProc", result.getProcessors().get(0).getName());
            assertEquals(1,result.getProcessGroups().size());
            assertEquals("nameSubGroup", result.getProcessGroups().get(0).getName());
            assertEquals("nameComponent", result.getName());
            assertEquals(1, result.getControllerServices().size());
            assertEquals("nameCtrl", result.getControllerServices().get(0).getName());
        }
    }




}