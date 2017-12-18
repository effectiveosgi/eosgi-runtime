package com.effectiveosgi.lib.osgi;

import java.util.Collections;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

class MockServiceRef<T> implements ServiceReference<T> {
	
	private final T service;
	private final Dictionary<String, Object> props;

	MockServiceRef(T service, Dictionary<String,Object> props) {
		this.service = service;
		this.props = props;
		
		Object serviceId = props.get(Constants.SERVICE_ID);
		if (serviceId == null || !(serviceId instanceof Long))
			throw new IllegalArgumentException(Constants.SERVICE_ID + " must be present and a Long value");
	}

	T getService() {
		return service;
	}	

	@Override
	public Object getProperty(String key) {
		return props.get(key);
	}

	@Override
	public String[] getPropertyKeys() {
		return Collections.list(props.keys()).toArray(new String[0]);
	}

	@Override
	public Bundle getBundle() {
		return null;
	}

	@Override
	public Bundle[] getUsingBundles() {
		return null;
	}

	@Override
	public boolean isAssignableTo(Bundle bundle, String className) {
		return false;
	}
	
	@Override
	public String toString() {
		Object rank = props.get(Constants.SERVICE_RANKING);
		return String.format("id=%d rank=%d service=%s", props.get(Constants.SERVICE_ID), rank != null ? rank : 0, service);
	}
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((props == null) ? 0 : props.get(Constants.SERVICE_ID).hashCode());
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
		@SuppressWarnings("rawtypes")
		MockServiceRef other = (MockServiceRef) obj;
		return this.props.get(Constants.SERVICE_ID).equals(other.props.get(Constants.SERVICE_ID));
	}

	// Copied from Felix
	@SuppressWarnings("rawtypes")
	@Override
    public int compareTo(Object reference)
    {
        ServiceReference other = (ServiceReference) reference;

        Long id = (Long) getProperty(Constants.SERVICE_ID);
        Long otherId = (Long) other.getProperty(Constants.SERVICE_ID);

        if (id.equals(otherId))
        {
            return 0; // same service
        }

        Object rankObj = getProperty(Constants.SERVICE_RANKING);
        Object otherRankObj = other.getProperty(Constants.SERVICE_RANKING);

        // If no rank, then spec says it defaults to zero.
        rankObj = (rankObj == null) ? new Integer(0) : rankObj;
        otherRankObj = (otherRankObj == null) ? new Integer(0) : otherRankObj;

        // If rank is not Integer, then spec says it defaults to zero.
        Integer rank = (rankObj instanceof Integer)
            ? (Integer) rankObj : new Integer(0);
        Integer otherRank = (otherRankObj instanceof Integer)
            ? (Integer) otherRankObj : new Integer(0);

        // Sort by rank in ascending order.
        if (rank.compareTo(otherRank) < 0)
        {
            return -1; // lower rank
        }
        else if (rank.compareTo(otherRank) > 0)
        {
            return 1; // higher rank
        }

        // If ranks are equal, then sort by service id in descending order.
        return (id.compareTo(otherId) < 0) ? 1 : -1;
    }

}