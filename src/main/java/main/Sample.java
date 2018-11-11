package main;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Sample {
    public String id;
    public int count;
    public ArrayList<String> junctions;

    public Sample(String id, int count) {
        this.id = id;
        this.count = count;
        junctions = new ArrayList<>();
    }

    public Sample(String id) {
        this.id = id;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!Sample.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final Sample other = (Sample) obj;
        return this.id.equals(other.id);
    }

    public String toString() {
        if (junctions.size() == 0) {
            return "NA";
        } else {
            return String.join(",", junctions);
        }
    }
}
