package main;

import java.util.ArrayList;

public class Region {
    public int start;
    public int end;

    public Region(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public boolean isContainedIn(ArrayList<Region> ROHRegions) {
        for (Region r : ROHRegions) {
            if (r.start < start && end < r.end) {
                return true;
            }
        }
        return false;
    }

    public boolean isInRange(ArrayList<Integer> startsFromExcel, int dist) {
        for (int v : startsFromExcel) {
            if (Math.abs(start - v) <= dist || Math.abs(end - v) <= dist) {
                return true;
            }
        }
        return false;
    }
}
