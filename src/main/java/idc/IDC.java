package idc;

import java.io.Serializable;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * identifiable data container
 */
public /*abstract*/ class IDC implements Serializable /*, Cloneable*/ {

    private static final long serialVersionUID = 42L;

    /**
     * Has this IDC changed outside the DB? (On the client side, Hibernate does not apply.)
     */
    public enum DirtyFlag { // implicitly static

        /**
         * Default, unchanged
         */
        CLEAN,
        /**
         * New (user-supplied) data
         */
        NEW,
        /**
         * Existing data but modified
         */
        MODIFIED,
        /**
         * Existing data but marked for deletion
         */
        DELETED
    }
    public DirtyFlag dirtyFlag = DirtyFlag.CLEAN;
    public static final long INVALID_UUID = -1; // TODO rename "blank"?
    private long uuid = INVALID_UUID;
    /**
     * instead of actually deleting records from the DB.
     * 
     * If it's both non-public and inherited, we'd need to improve our
     * reflection code to reach it.
     */
    public boolean active = true;

	public IDC() {
    }

    public IDC(IDC orig) {
        if (null != orig) {
            try {
                BeanUtils.copyProperties(this, orig);
            } catch (Exception e) {
            }
        }
    }

    public boolean hasValidUuid() {
        return INVALID_UUID != getUuid();
    }

    public void invalidateUuid() {
        setUuid(INVALID_UUID);
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    public long getUuid() {
        return uuid;
    }

    public void setUuid(long uuid) {
        this.uuid = uuid;
    }

    public boolean getActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

    public String getLabel() {
        return "label:" + uuid;
    }
}
