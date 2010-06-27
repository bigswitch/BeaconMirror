package net.beaconcontroller.routing;

import java.util.ArrayList;
import java.util.List;

public class Route implements Cloneable, Comparable<Route> {
    protected RouteId id;
    protected List<Link> path;

    public Route(RouteId id, List<Link> path) {
        super();
        this.id = id;
        this.path = path;
    }

    public Route(Long src, Long dst) {
        super();
        this.id = new RouteId(src, dst);
        this.path = new ArrayList<Link>();
    }

    /**
     * @return the id
     */
    public RouteId getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(RouteId id) {
        this.id = id;
    }

    /**
     * @return the path
     */
    public List<Link> getPath() {
        return path;
    }

    /**
     * @param path the path to set
     */
    public void setPath(List<Link> path) {
        this.path = path;
    }

    @Override
    public int hashCode() {
        final int prime = 5791;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Route other = (Route) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Route [id=" + id + ", path=" + path + "]";
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Route clone = (Route) super.clone();
        clone.setId((RouteId) this.id.clone());
        return clone;
    }

    /**
     * Compares the path lengths between Routes.
     */
    @Override
    public int compareTo(Route o) {
        return ((Integer)path.size()).compareTo(o.path.size());
    }
}
