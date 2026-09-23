package com.cmpe172.fitness.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Data Transfer Object for an available slot.
 * Combines info from availability_slots, providers, and services
 * so the client gets one flat, readable JSON object per slot.
 */
public class SlotDTO {

    private int slotId;
    private String providerName;
    private String serviceName;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private double price;

    public SlotDTO() {
    }

    public SlotDTO(int slotId, String providerName, String serviceName,
                    LocalDate slotDate, LocalTime startTime, LocalTime endTime, double price) {
        this.slotId = slotId;
        this.providerName = providerName;
        this.serviceName = serviceName;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
    }

    public int getSlotId() {
        return slotId;
    }

    public void setSlotId(int slotId) {
        this.slotId = slotId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public void setSlotDate(LocalDate slotDate) {
        this.slotDate = slotDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
