package main;

import java.util.ArrayList;

public class Region {
    public int start;
    public int end;

    public Region(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public boolean isContainedIn(ArrayList<Region> regions) {
        for (Region r : regions) {
            if (r.start < start  && end < r.end) {
                return true;
            }
        }
        return false;
    }
}
