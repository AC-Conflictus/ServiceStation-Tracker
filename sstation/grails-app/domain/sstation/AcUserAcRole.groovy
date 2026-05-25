package sstation

import grails.gorm.DetachedCriteria
import groovy.transform.ToString

import org.apache.commons.lang.builder.HashCodeBuilder

@ToString(cache=true, includeNames=true, includePackage=false)
class AcUserAcRole implements Serializable {

	private static final long serialVersionUID = 1

	AcUser acUser
	AcRole acRole

	AcUserAcRole(AcUser u, AcRole r) {
		this()
		acUser = u
		acRole = r
	}

	@Override
	boolean equals(other) {
		if (!(other instanceof AcUserAcRole)) {
			return false
		}

		other.acUser?.id == acUser?.id && other.acRole?.id == acRole?.id
	}

	@Override
	int hashCode() {
		def builder = new HashCodeBuilder()
		if (acUser) builder.append(acUser.id)
		if (acRole) builder.append(acRole.id)
		builder.toHashCode()
	}

	static AcUserAcRole get(long acUserId, long acRoleId) {
		criteriaFor(acUserId, acRoleId).get()
	}

	static boolean exists(long acUserId, long acRoleId) {
		criteriaFor(acUserId, acRoleId).count()
	}

	private static DetachedCriteria criteriaFor(long acUserId, long acRoleId) {
		AcUserAcRole.where {
			acUser == AcUser.load(acUserId) &&
			acRole == AcRole.load(acRoleId)
		}
	}

	static AcUserAcRole create(AcUser acUser, AcRole acRole, boolean flush = false) {
		def instance = new AcUserAcRole(acUser, acRole)
		instance.save(flush: flush, insert: true)
		instance
	}

	static boolean remove(AcUser u, AcRole r, boolean flush = false) {
		if (u == null || r == null) return false

		int rowCount = AcUserAcRole.where { acUser == u && acRole == r }.deleteAll()

		if (flush) { AcUserAcRole.withSession { it.flush() } }

		rowCount
	}

	static void removeAll(AcUser u, boolean flush = false) {
		if (u == null) return

		AcUserAcRole.where { acUser == u }.deleteAll()

		if (flush) { AcUserAcRole.withSession { it.flush() } }
	}

	static void removeAll(AcRole r, boolean flush = false) {
		if (r == null) return

		AcUserAcRole.where { acRole == r }.deleteAll()

		if (flush) { AcUserAcRole.withSession { it.flush() } }
	}

	static constraints = {
		acRole validator: { AcRole r, AcUserAcRole ur ->
			if (ur.acUser == null || ur.acUser.id == null) return
			boolean existing = false
			AcUserAcRole.withNewSession {
				existing = AcUserAcRole.exists(ur.acUser.id, r.id)
			}
			if (existing) {
				return 'userRole.exists'
			}
		}
	}

	static mapping = {
		id composite: ['acUser', 'acRole']
		version false
	}
}
