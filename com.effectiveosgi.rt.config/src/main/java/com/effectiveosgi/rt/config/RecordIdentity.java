package com.effectiveosgi.rt.config;

public class RecordIdentity {

	private final String id;
	private final String factoryId;

	public RecordIdentity(String id, String factoryId) {
		this.id = id;
		this.factoryId = factoryId;
	}

	public String getId() {
		return id;
	}

	public String getFactoryId() {
		return factoryId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((factoryId == null) ? 0 : factoryId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		RecordIdentity other = (RecordIdentity) obj;
		if (factoryId == null) {
			if (other.factoryId != null)
				return false;
		} else if (!factoryId.equals(other.factoryId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(id);
		if (factoryId != null)
			b.append(" (factoryPid=").append(factoryId).append(")");
		return b.toString();
	}

}
