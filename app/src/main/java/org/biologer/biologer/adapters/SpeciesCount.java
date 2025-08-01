package org.biologer.biologer.adapters;

/**
 * This adapter is used to save species name number of individuals observed for the
 * RecyclerView. It is currently used for Timed Count data.
 */

public class SpeciesCount {
    private String speciesName;
    private long speciesID;
    private int numberOfIndividuals;

    public SpeciesCount(String speciesName, long speciesID, int numberOfIndividuals) {
        this.speciesName = speciesName;
        this.speciesID = speciesID;
        this.numberOfIndividuals = numberOfIndividuals;
    }

    // Getters and setters
    public String getSpeciesName() {
        return speciesName;
    }

    public void setSpeciesName(String speciesName) {
        this.speciesName = speciesName;
    }

    public int getNumberOfIndividuals() {
        return numberOfIndividuals;
    }

    public void setNumberOfIndividuals(int numberOfIndividuals) {
        this.numberOfIndividuals = numberOfIndividuals;
    }

    public long getSpeciesID() {
        return speciesID;
    }

    public void setSpeciesID(int speciesID) {
        this.speciesID = speciesID;
    }
}