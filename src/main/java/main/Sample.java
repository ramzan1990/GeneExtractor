package main;

public class Sample {
    public String id;
    public int count;

    public Sample(String id, int count){
        this.id = id;
        this.count = count;
    }

    public Sample(String id){
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
}
