package com.github.hermannpencole.nifi.config.model;

import com.github.hermannpencole.nifi.swagger.client.model.ControllerServiceDTO;
import com.github.hermannpencole.nifi.swagger.client.model.ProcessorDTO;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SFRJ2737 on 2017-05-26.
 */
public class GroupProcessorsEntity {

    @SerializedName("processors")
    private List<ProcessorDTO> processors = new ArrayList<>();

    @SerializedName("groupProcessorsEntity")
    private List<GroupProcessorsEntity> processGroups = new ArrayList<>();

    @SerializedName("controllerServices")
    private List<ControllerServiceDTO> controllerServices = new ArrayList<>();

    @SerializedName("connections")
    private List<ConnectionPort> connectionPorts = new ArrayList<>();

    @SerializedName("name")
    private String name;


    public List<ProcessorDTO> getProcessors() {
        return processors;
    }

    public void setProcessors(List<ProcessorDTO> processors) {
        this.processors = processors;
    }

    public List<GroupProcessorsEntity> getProcessGroups() {
        return processGroups;
    }

    /**
     *
     * @param processGroups
     */
    public void setProcessGroups(List<GroupProcessorsEntity> processGroups) {
        this.processGroups = processGroups;
    }

    public List<ControllerServiceDTO> getControllerServices() {
        return controllerServices;
    }

    public void setControllerServices(List<ControllerServiceDTO> controllerServices) {
        this.controllerServices = controllerServices;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public List<ConnectionPort> getConnectionPorts() {
        return connectionPorts;
    }

    public void setConnectionPorts(List<ConnectionPort> connectionPorts) {
        this.connectionPorts = connectionPorts;
    }
}
